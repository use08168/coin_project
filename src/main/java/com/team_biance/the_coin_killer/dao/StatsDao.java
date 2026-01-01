package com.team_biance.the_coin_killer.dao;

import com.team_biance.the_coin_killer.model.MonitorStats;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

@Repository
public class StatsDao {
    private final JdbcTemplate jdbc;

    public StatsDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Instant toInstant(Object v) {
        if (v == null)
            return null;
        if (v instanceof Timestamp)
            return ((Timestamp) v).toInstant();
        return null;
    }

    public MonitorStats loadStats(String symbol) {
        MonitorStats s = new MonitorStats();

        Map<String, Object> depth = jdbc.queryForMap(
                "SELECT COUNT(*) cnt, MAX(ts_utc) last_ts FROM f_depth_snapshot_1s WHERE symbol=?",
                symbol);
        s.depthCount = ((Number) depth.get("cnt")).longValue();
        s.depthLastTs = toInstant(depth.get("last_ts"));

        Map<String, Object> fm = jdbc.queryForMap(
                "SELECT COUNT(*) cnt, MAX(ts_utc) last_ts FROM feature_minute WHERE symbol=?",
                symbol);
        s.featureMinuteCount = ((Number) fm.get("cnt")).longValue();
        s.featureMinuteLastTs = toInstant(fm.get("last_ts"));

        Map<String, Object> fh = jdbc.queryForMap(
                "SELECT COUNT(*) cnt, MAX(ts_utc) last_ts FROM feature_hour WHERE symbol=?",
                symbol);
        s.featureHourCount = ((Number) fh.get("cnt")).longValue();
        s.featureHourLastTs = toInstant(fh.get("last_ts"));

        Map<String, Object> pred = jdbc.queryForMap(
                "SELECT COUNT(*) cnt, MAX(ts_utc) last_ts FROM model_pred_60m WHERE symbol=?",
                symbol);
        s.predCount = ((Number) pred.get("cnt")).longValue();
        s.predLastTs = toInstant(pred.get("last_ts"));

        return s;
    }

    public boolean ping() {
        Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
        return one != null && one == 1;
    }
}
