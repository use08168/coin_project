package com.team_biance.the_coin_killer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team_biance.the_coin_killer.dto.ModelStatusDto;
import com.team_biance.the_coin_killer.dto.TrainResultDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.file.attribute.FileTime;

import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class ModelTrainService {

    private final PythonExecutorService pythonExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${python.script.dir:src/main/python}")
    private String scriptDir;

    public ModelTrainService(PythonExecutorService pythonExecutor) {
        this.pythonExecutor = pythonExecutor;
    }

    public TrainResultDto train(String symbol, int days) {
        String modelRelPath = buildModelRelPath(symbol);

        PythonExecutorService.ExecutionResult er = pythonExecutor.execute("train.py",
                "--days", String.valueOf(days),
                "--symbol", symbol,
                "--output", modelRelPath);

        TrainResultDto dto = parseTrainJson(er.stdout());
        dto.setDurationMs(er.durationMs());

        // stderr는 로그니까 필요하면 서버 로그로만 남기고, 화면에는 dto만
        // 학습 성공 시 meta 저장 (status에서 읽도록)
        if (dto.isOk()) {
            writeModelMeta(modelRelPath, dto.getModelVersion());
        } else {
            // Python이 ok:false JSON을 줬는데도 exitCode!=0일 수 있음 -> 메시지 보강
            if ((dto.getErrorMessage() == null || dto.getErrorMessage().isBlank()) && er.exitCode() != 0) {
                dto.setErrorMessage("Python process failed. exitCode=" + er.exitCode());
            }
        }

        return dto;
    }

    public ModelStatusDto status(String symbol) {
        String modelRelPath = buildModelRelPath(symbol);
        Path modelPath = Paths.get(scriptDir).resolve(modelRelPath).normalize();
        Path metaPath = Paths.get(scriptDir).resolve(modelRelPath + ".meta.json").normalize();

        ModelStatusDto st = new ModelStatusDto();
        st.setSymbol(symbol);
        st.setModelPath(modelRelPath);
        st.setModelExists(Files.exists(modelPath));

        if (Files.exists(modelPath)) {
            try {
                FileTime ft = Files.getLastModifiedTime(modelPath);
                Instant instant = ft.toInstant();
                st.setLastModifiedUtc(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
            } catch (IOException ignored) {
            }
        }

        if (Files.exists(metaPath)) {
            try {
                JsonNode n = objectMapper.readTree(Files.readString(metaPath));
                if (n.hasNonNull("modelVersion"))
                    st.setModelVersion(n.get("modelVersion").asText());
                if (n.hasNonNull("trainedAtUtc")) {
                    st.setTrainedAtUtc(parseUtcToLocalDateTime(n.get("trainedAtUtc").asText()));
                }
            } catch (Exception ignored) {
            }
        }

        return st;
    }

    private String buildModelRelPath(String symbol) {
        // python scriptDir 기준 상대경로
        return "model/lgbm_" + symbol.toLowerCase() + ".pkl";
    }

    private TrainResultDto parseTrainJson(String stdout) {
        TrainResultDto dto = new TrainResultDto();
        if (stdout == null || stdout.isBlank()) {
            dto.setOk(false);
            dto.setErrorCode("EMPTY_STDOUT");
            dto.setErrorMessage("Python stdout is empty");
            return dto;
        }

        try {
            JsonNode root = objectMapper.readTree(stdout);

            boolean ok = root.path("ok").asBoolean(false);
            dto.setOk(ok);

            if (!ok) {
                JsonNode err = root.path("error");
                dto.setErrorCode(err.path("code").asText("TRAIN_FAILED"));
                dto.setErrorMessage(err.path("message").asText("train failed"));
                return dto;
            }

            dto.setSymbol(root.path("symbol").asText());
            dto.setDays(root.path("days").asInt());
            dto.setRowsUsed(root.path("rows_used").asInt());
            dto.setTrainRows(root.path("train_rows").asInt());
            dto.setTestRows(root.path("test_rows").asInt());
            dto.setFeaturesUsed(root.path("features_used").asInt());
            dto.setModelPath(root.path("model_path").asText());
            dto.setModelVersion(root.path("model_version").asText());

            TrainResultDto.TrainMetrics m = new TrainResultDto.TrainMetrics();
            JsonNode mn = root.path("metrics");
            m.setAccuracy(mn.path("accuracy").asDouble(0.0));
            m.setPrecision(mn.path("precision").asDouble(0.0));
            m.setRecall(mn.path("recall").asDouble(0.0));
            m.setF1(mn.path("f1").asDouble(0.0));
            m.setMda(mn.path("mda").asDouble(0.0));
            dto.setMetrics(m);

            return dto;

        } catch (Exception e) {
            dto.setOk(false);
            dto.setErrorCode("JSON_PARSE_ERROR");
            dto.setErrorMessage("Failed to parse stdout JSON: " + e.getMessage());
            return dto;
        }
    }

    private void writeModelMeta(String modelRelPath, String modelVersion) {
        try {
            Path metaPath = Paths.get(scriptDir).resolve(modelRelPath + ".meta.json").normalize();
            Files.createDirectories(metaPath.getParent());

            Map<String, Object> meta = new HashMap<>();
            meta.put("modelVersion", modelVersion);
            meta.put("trainedAtUtc", LocalDateTime.now(ZoneOffset.UTC).toString());

            Files.writeString(metaPath, objectMapper.writeValueAsString(meta),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {
        }
    }

    private static LocalDateTime parseUtcToLocalDateTime(String s) {
        // "2024-01-15T10:30:00Z" 또는 "2024-01-15T10:30:00"
        try {
            if (s.endsWith("Z")) {
                return OffsetDateTime.parse(s).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            }
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
