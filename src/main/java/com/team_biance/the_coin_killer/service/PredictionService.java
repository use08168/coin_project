package com.team_biance.the_coin_killer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team_biance.the_coin_killer.dao.FeatureHourDao;
import com.team_biance.the_coin_killer.dao.ModelPredDao;
import com.team_biance.the_coin_killer.model.PythonPredictRequest;
import com.team_biance.the_coin_killer.model.PythonPredictResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class PredictionService {

    private final FeatureHourDao featureHourDao;
    private final PythonModelClient pythonModelClient;
    private final ModelPredDao modelPredDao;
    private final ObjectMapper om;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    public PredictionService(FeatureHourDao featureHourDao,
            PythonModelClient pythonModelClient,
            ModelPredDao modelPredDao,
            ObjectMapper om) {
        this.featureHourDao = featureHourDao;
        this.pythonModelClient = pythonModelClient;
        this.modelPredDao = modelPredDao;
        this.om = om;
    }

    /**
     * feature_hour 최신 1개로 Python 예측 호출 후 model_pred_60m 저장
     * - feature_hour가 없으면 null 반환
     */
    public Long predictLatest(String symbol) {
        Map<String, Object> row;
        try {
            row = featureHourDao.findLatest(symbol);
        } catch (Exception e) {
            return null; // 아직 feature_hour 없음
        }

        Instant tsUtc = ((java.sql.Timestamp) row.get("ts_utc")).toInstant();
        double closeNow = ((Number) row.get("close_60m")).doubleValue(); // feature_hour의 close_60m을 현재가로 사용

        PythonPredictRequest req = new PythonPredictRequest();
        req.symbol = symbol;
        req.ts_utc = ISO.format(tsUtc);
        req.close_now = closeNow;
        req.feature_hour = featureHourDao.toFeatureMap(row);

        PythonPredictResponse out;
        try {
            out = pythonModelClient.predict(req);
        } catch (Exception ex) {
            // Python 서버가 아직 안 떠있을 수 있으니 조용히 스킵
            return null;
        }
        if (out == null)
            return null;

        String topJson = null;
        try {
            topJson = (out.top_features == null) ? null : om.writeValueAsString(out.top_features);
        } catch (Exception ignore) {
        }

        long predId = modelPredDao.insertPred(
                symbol,
                tsUtc,
                closeNow,
                out.pred_return_60m_log,
                out.pred_close_60m,
                out.model_version == null ? "python_model" : out.model_version,
                out.latency_ms,
                topJson);

        return predId < 0 ? null : predId;
    }
}
