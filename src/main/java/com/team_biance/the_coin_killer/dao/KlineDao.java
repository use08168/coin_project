package com.team_biance.the_coin_killer.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class KlineDao {
    private final JdbcTemplate jdbc;

    public KlineDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsert1m(String symbol, Instant tsUtc,
            double open, double high, double low, double close,
            double volume, int tradeCount,
            Double quoteVol, Double takerBuyVol, Double takerBuyQv) {

        jdbc.update(
                "INSERT INTO f_kline_1m(symbol, ts_utc, open_, high_, low_, close_, volume_, trade_count, quote_volume, taker_buy_vol, taker_buy_qv) "
                        +
                        "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "open_=VALUES(open_), high_=VALUES(high_), low_=VALUES(low_), close_=VALUES(close_), volume_=VALUES(volume_), trade_count=VALUES(trade_count), "
                        +
                        "quote_volume=VALUES(quote_volume), taker_buy_vol=VALUES(taker_buy_vol), taker_buy_qv=VALUES(taker_buy_qv)",
                symbol, Timestamp.from(tsUtc),
                open, high, low, close,
                volume, tradeCount,
                quoteVol, takerBuyVol, takerBuyQv);
    }
}
