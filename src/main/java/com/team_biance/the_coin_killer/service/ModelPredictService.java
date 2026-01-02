package com.team_biance.the_coin_killer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team_biance.the_coin_killer.dto.PredictResultDto;
import com.team_biance.the_coin_killer.mapper.ModelPredMapper;
import com.team_biance.the_coin_killer.model.ModelPred60m;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class ModelPredictService {

    private final PythonExecutorService pythonExecutor;
    private final ModelPredMapper modelPredMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModelPredictService(PythonExecutorService pythonExecutor, ModelPredMapper modelPredMapper) {
        this.pythonExecutor = pythonExecutor;
        this.modelPredMapper = modelPredMapper;
    }

    public PredictResultDto predict(String symbol) {
        String modelRelPath = "model/lgbm_" + symbol.toLowerCase() + ".pkl";

        PythonExecutorService.ExecutionResult er = pythonExecutor.execute("predict.py",
                "--symbol", symbol,
                "--model", modelRelPath);

        PredictResultDto dto = parsePredictJson(er.stdout());
        dto.setDurationMs(er.durationMs());

        // 성공 시 DB 저장
        if (dto.isOk()) {
            try {
                ModelPred60m p = new ModelPred60m();
                p.setSymbol(dto.getSymbol());
                p.setTsUtc(dto.getTimestamp());
                p.setCurrentClose(BigDecimal.valueOf(dto.getCurrentClose()));
                p.setPrediction(dto.getPrediction());
                p.setProbability(BigDecimal.valueOf(dto.getProbability()));
                p.setModelVersion(dto.getModelVersion());
                modelPredMapper.insertPrediction(p);
            } catch (Exception e) {
                // 저장 실패해도 예측 자체는 성공이므로 ok는 유지 (요구사항: 끊기면 안됨)
                // 화면에는 경고로 보이게 하고 싶으면 errorMessage에 보강 가능
                dto.setErrorCode("DB_INSERT_FAILED");
                dto.setErrorMessage("Prediction OK, but DB insert failed: " + e.getMessage());
            }
        } else {
            if ((dto.getErrorMessage() == null || dto.getErrorMessage().isBlank()) && er.exitCode() != 0) {
                dto.setErrorMessage("Python process failed. exitCode=" + er.exitCode());
            }
        }

        return dto;
    }

    public List<ModelPred60m> recentPredictions(String symbol, int limit) {
        return modelPredMapper.recentPredictions(symbol, limit);
    }

    private PredictResultDto parsePredictJson(String stdout) {
        PredictResultDto dto = new PredictResultDto();
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
                dto.setErrorCode(err.path("code").asText("PREDICT_FAILED"));
                dto.setErrorMessage(err.path("message").asText("predict failed"));
                return dto;
            }

            dto.setSymbol(root.path("symbol").asText());
            dto.setCurrentClose(root.path("current_close").asDouble(0.0));
            dto.setPrediction(root.path("prediction").asInt());
            dto.setPredictionLabel(root.path("prediction_label").asText());
            dto.setProbability(root.path("probability").asDouble(0.0));
            dto.setModelVersion(root.path("model_version").asText());
            dto.setFeaturesUsed(root.path("features_used").asInt());

            String ts = root.path("timestamp").asText();
            dto.setTimestamp(parseZonedUtc(ts));

            return dto;

        } catch (Exception e) {
            dto.setOk(false);
            dto.setErrorCode("JSON_PARSE_ERROR");
            dto.setErrorMessage("Failed to parse stdout JSON: " + e.getMessage());
            return dto;
        }
    }

    private static LocalDateTime parseZonedUtc(String s) {
        // "2024-01-15T10:30:00Z" 형태를 UTC LocalDateTime으로
        try {
            if (s.endsWith("Z")) {
                return OffsetDateTime.parse(s).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            }
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}
