package com.team_biance.the_coin_killer.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class OpenInterestDao {
    private final JdbcTemplate jdbc;

    public OpenInterestDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsert1m(String symbol, Instant tsUtc, double openInterest) {
        jdbc.update(
                "INSERT INTO f_open_interest_1m(symbol, ts_utc, open_interest) VALUES(?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE open_interest=VALUES(open_interest)",
                symbol, Timestamp.from(tsUtc), openInterest);
    }
}
