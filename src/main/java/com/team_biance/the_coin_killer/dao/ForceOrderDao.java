package com.team_biance.the_coin_killer.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class ForceOrderDao {
    private final JdbcTemplate jdbc;

    public ForceOrderDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(String symbol, Instant eventUtc, String side, double price, double qty, String status) {
        jdbc.update(
                "INSERT INTO f_forceorder(symbol, event_utc, side, price_, qty_, status_) VALUES(?, ?, ?, ?, ?, ?)",
                symbol, Timestamp.from(eventUtc), side, price, qty, status);
    }
}
