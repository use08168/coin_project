package com.team_biance.the_coin_killer.service.binance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team_biance.the_coin_killer.dao.DepthDao;
import com.team_biance.the_coin_killer.util.GzipUtil;
import com.team_biance.the_coin_killer.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DepthSnapshotFlusher {

    private static final Logger log = LoggerFactory.getLogger(DepthSnapshotFlusher.class);

    @Value("${binance.symbol:BTCUSDT}")
    private String symbol;

    private final DepthCache depthCache;
    private final DepthDao depthDao;
    private final ObjectMapper om;
    private final BinanceIngestState state;

    private volatile Instant lastFlushedSecond = null;

    public DepthSnapshotFlusher(DepthCache depthCache, DepthDao depthDao, ObjectMapper om, BinanceIngestState state) {
        this.depthCache = depthCache;
        this.depthDao = depthDao;
        this.om = om;
        this.state = state;
    }

    @Scheduled(cron = "* * * * * *") // 매초
    public void flush() {
        DepthCache.DepthData d = depthCache.get();
        if (d == null)
            return;

        Instant sec = TimeUtil.floorToSecond(d.eventTime);
        if (lastFlushedSecond != null && !sec.isAfter(lastFlushedSecond))
            return;

        try {
            if (d.bids == null || d.bids.size() == 0 || d.asks == null || d.asks.size() == 0)
                return;

            double bestBid = d.bids.get(0).get(0).asDouble();
            double bestAsk = d.asks.get(0).get(0).asDouble();
            double mid = (bestBid + bestAsk) / 2.0;
            double spreadBps = (bestAsk - bestBid) / mid * 1e4;

            double bidSum = 0.0, askSum = 0.0;
            for (int i = 0; i < d.bids.size(); i++)
                bidSum += d.bids.get(i).get(1).asDouble();
            for (int i = 0; i < d.asks.size(); i++)
                askSum += d.asks.get(i).get(1).asDouble();

            double imbalance = (bidSum - askSum) / (bidSum + askSum + 1e-12);

            Double microprice = null, microGap = null;
            try {
                double bidQty0 = d.bids.get(0).get(1).asDouble();
                double askQty0 = d.asks.get(0).get(1).asDouble();
                microprice = (bestAsk * bidQty0 + bestBid * askQty0) / (bidQty0 + askQty0 + 1e-12);
                microGap = (microprice - mid) / mid * 1e4;
            } catch (Exception ignore) {
            }

            byte[] bidsGz = GzipUtil.gzip(om.writeValueAsString(d.bids));
            byte[] asksGz = GzipUtil.gzip(om.writeValueAsString(d.asks));

            depthDao.upsert1s(symbol, sec, bestBid, bestAsk, mid, spreadBps,
                    bidSum, askSum, imbalance, microprice, microGap, bidsGz, asksGz);

            lastFlushedSecond = sec;
            state.lastDepthFlushAt.set(Instant.now());
        } catch (Exception e) {
            String msg = "Depth flush error: " + e.getMessage();
            state.lastError.set(msg);
            log.error(msg, e);
        }
    }
}
