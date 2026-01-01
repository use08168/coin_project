package com.team_biance.the_coin_killer.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Repository
public class FeatureHourDao {
    private final JdbcTemplate jdbc;

    public FeatureHourDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * feature_hour 최신 1개를 Map으로 반환(60개 컬럼 포함)
     */
    public Map<String, Object> findLatest(String symbol) {
        return jdbc.queryForMap(
                "SELECT * FROM feature_hour WHERE symbol=? ORDER BY ts_utc DESC LIMIT 1",
                symbol);
    }

    public boolean exists(String symbol, Instant tsUtc) {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM feature_hour WHERE symbol=? AND ts_utc=?",
                Integer.class,
                symbol,
                Timestamp.from(tsUtc));
        return c != null && c > 0;
    }

    public Instant latestTs(String symbol) {
        Timestamp ts = jdbc.queryForObject(
                "SELECT MAX(ts_utc) FROM feature_hour WHERE symbol=?",
                Timestamp.class,
                symbol);
        return ts == null ? null : ts.toInstant();
    }

    /**
     * Python에게 넘길 feature_hour Map 만들기:
     * DB row에서 메타 컬럼(symbol, ts_utc, created_at)은 제거
     */
    public Map<String, Object> toFeatureMap(Map<String, Object> row) {
        Map<String, Object> m = new HashMap<>(row);
        m.remove("symbol");
        m.remove("ts_utc");
        m.remove("created_at");
        return m;
    }
}
