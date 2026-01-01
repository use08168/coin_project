package com.team_biance.the_coin_killer.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class DepthDao {
    private final JdbcTemplate jdbc;

    public DepthDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsert1s(String symbol, Instant tsUtc,
            double bestBid, double bestAsk, double mid, double spreadBps,
            double bidSum, double askSum, double imbalance,
            Double microprice, Double microGapBps,
            byte[] bidsGzip, byte[] asksGzip) {

        jdbc.update(
                "INSERT INTO f_depth_snapshot_1s(symbol, ts_utc, best_bid, best_ask, mid_price, spread_bps, " +
                        "depth_bid_sum_top20, depth_ask_sum_top20, imbalance_top20, microprice, microprice_gap_bps, " +
                        "bids_gzip, asks_gzip, compress_algo) " +
                        "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'GZIP') " +
                        "ON DUPLICATE KEY UPDATE " +
                        "best_bid=VALUES(best_bid), best_ask=VALUES(best_ask), mid_price=VALUES(mid_price), spread_bps=VALUES(spread_bps), "
                        +
                        "depth_bid_sum_top20=VALUES(depth_bid_sum_top20), depth_ask_sum_top20=VALUES(depth_ask_sum_top20), imbalance_top20=VALUES(imbalance_top20), "
                        +
                        "microprice=VALUES(microprice), microprice_gap_bps=VALUES(microprice_gap_bps), " +
                        "bids_gzip=VALUES(bids_gzip), asks_gzip=VALUES(asks_gzip), compress_algo='GZIP'",
                symbol, Timestamp.from(tsUtc),
                bestBid, bestAsk, mid, spreadBps,
                bidSum, askSum, imbalance,
                microprice, microGapBps,
                bidsGzip, asksGzip);
    }
}
