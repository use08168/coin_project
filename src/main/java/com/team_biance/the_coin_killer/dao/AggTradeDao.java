package com.team_biance.the_coin_killer.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class AggTradeDao {
    private final JdbcTemplate jdbc;

    public AggTradeDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsert1m(String symbol, Instant tsUtc,
            double takerBuyQty, double takerSellQty, int tradeCount, Double vwapPrice) {

        jdbc.update(
                "INSERT INTO f_aggtrade_1m(symbol, ts_utc, taker_buy_qty, taker_sell_qty, trade_count, vwap_price) " +
                        "VALUES(?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "taker_buy_qty=VALUES(taker_buy_qty), taker_sell_qty=VALUES(taker_sell_qty), trade_count=VALUES(trade_count), vwap_price=VALUES(vwap_price)",
                symbol, Timestamp.from(tsUtc),
                takerBuyQty, takerSellQty, tradeCount, vwapPrice);
    }
}
