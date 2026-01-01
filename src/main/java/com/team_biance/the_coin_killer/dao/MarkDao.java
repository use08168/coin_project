package com.team_biance.the_coin_killer.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class MarkDao {
    private final JdbcTemplate jdbc;

    public MarkDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsert1s(String symbol, Instant tsUtc,
            double markPrice, Double indexPrice,
            Double fundingRate, Instant nextFundingUtc) {

        jdbc.update(
                "INSERT INTO f_mark_1s(symbol, ts_utc, mark_price, index_price, funding_rate, next_funding_utc) " +
                        "VALUES(?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "mark_price=VALUES(mark_price), index_price=VALUES(index_price), funding_rate=VALUES(funding_rate), next_funding_utc=VALUES(next_funding_utc)",
                symbol, Timestamp.from(tsUtc),
                markPrice, indexPrice, fundingRate,
                nextFundingUtc == null ? null : Timestamp.from(nextFundingUtc));
    }
}
