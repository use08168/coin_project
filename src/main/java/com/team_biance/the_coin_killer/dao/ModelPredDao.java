package com.team_biance.the_coin_killer.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team_biance.the_coin_killer.model.ModelPredView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.List;

@Repository
public class ModelPredDao {
    private final JdbcTemplate jdbc;
    private final ObjectMapper om;

    public ModelPredDao(JdbcTemplate jdbc, ObjectMapper om) {
        this.jdbc = jdbc;
        this.om = om;
    }

    public long insertPred(String symbol, Instant tsUtc, double closeNow,
            double predReturn60mLog, double predClose60m,
            String modelVersion, Integer latencyMs, String topFeaturesJson) {

        jdbc.update(
                "INSERT INTO model_pred_60m(symbol, ts_utc, horizon_min, close_now, pred_return_60m_log, pred_close_60m, model_version, latency_ms, top_features_json) "
                        +
                        "VALUES(?, ?, 60, ?, ?, ?, ?, ?, CAST(? AS JSON)) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "close_now=VALUES(close_now), pred_return_60m_log=VALUES(pred_return_60m_log), pred_close_60m=VALUES(pred_close_60m), model_version=VALUES(model_version), latency_ms=VALUES(latency_ms), top_features_json=VALUES(top_features_json)",
                symbol,
                Timestamp.from(tsUtc),
                closeNow,
                predReturn60mLog,
                predClose60m,
                modelVersion,
                latencyMs,
                topFeaturesJson == null ? "null" : topFeaturesJson);

        // pred_id 가져오기 (최근 삽입된 행 조회)
        Long id = jdbc.queryForObject(
                "SELECT pred_id FROM model_pred_60m WHERE symbol=? AND ts_utc=? AND horizon_min=60",
                Long.class,
                symbol,
                Timestamp.from(tsUtc));
        return id == null ? -1L : id;
    }

    public void upsertConvergence(long predId, Instant obsTsUtc, double closeNow,
            double errInit, double errNow, double convScore, double convScoreVol,
            Double volZ5m, Integer remainingMin) {

        jdbc.update(
                "INSERT INTO pred_convergence_1m(pred_id, obs_ts_utc, close_now, err_init, err_now, conv_score, conv_score_vol, vol_z_5m, remaining_min) "
                        +
                        "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE close_now=VALUES(close_now), err_now=VALUES(err_now), conv_score=VALUES(conv_score), conv_score_vol=VALUES(conv_score_vol), vol_z_5m=VALUES(vol_z_5m), remaining_min=VALUES(remaining_min)",
                predId,
                Timestamp.from(obsTsUtc),
                closeNow,
                errInit,
                errNow,
                convScore,
                convScoreVol,
                volZ5m,
                remainingMin);
    }

    private static final RowMapper<ModelPredView> PRED_VIEW_MAPPER = (rs, rowNum) -> {
        ModelPredView v = new ModelPredView();
        v.predId = rs.getLong("pred_id");
        v.symbol = rs.getString("symbol");
        v.tsUtc = rs.getTimestamp("ts_utc").toInstant();
        v.closeNow = rs.getDouble("close_now");
        v.predReturn60mLog = rs.getDouble("pred_return_60m_log");
        v.predClose60m = rs.getDouble("pred_close_60m");
        v.modelVersion = rs.getString("model_version");
        int lm = rs.getInt("latency_ms");
        v.latencyMs = rs.wasNull() ? null : lm;

        Timestamp cts = rs.getTimestamp("conv_obs_ts_utc");
        v.convObsTsUtc = (cts == null) ? null : cts.toInstant();

        Double cs = (Double) rs.getObject("conv_score");
        Double csv = (Double) rs.getObject("conv_score_vol");
        v.convScore = cs;
        v.convScoreVol = csv;

        return v;
    };

    public List<ModelPredView> listRecent(String symbol, int limit) {
        return jdbc.query(
                "SELECT p.pred_id, p.symbol, p.ts_utc, p.close_now, p.pred_return_60m_log, p.pred_close_60m, p.model_version, p.latency_ms, "
                        +
                        "c.obs_ts_utc AS conv_obs_ts_utc, c.conv_score, c.conv_score_vol " +
                        "FROM model_pred_60m p " +
                        "LEFT JOIN pred_convergence_1m c ON c.pred_id = p.pred_id " +
                        "AND c.obs_ts_utc = (SELECT MAX(c2.obs_ts_utc) FROM pred_convergence_1m c2 WHERE c2.pred_id=p.pred_id) "
                        +
                        "WHERE p.symbol=? " +
                        "ORDER BY p.ts_utc DESC " +
                        "LIMIT ?",
                PRED_VIEW_MAPPER,
                symbol,
                limit);
    }
}
