package com.team_biance.the_coin_killer.service.binance;

import com.team_biance.the_coin_killer.dao.AggTradeDao;
import com.team_biance.the_coin_killer.util.TimeUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AggTradeAggregator {

    private static class Bucket {
        double takerBuyQty = 0.0;
        double takerSellQty = 0.0;
        int tradeCount = 0;

        double pvSum = 0.0; // price*qty
        double vSum = 0.0; // qty
    }

    private final AggTradeDao aggTradeDao;
    private final Map<Long, Bucket> buckets = new ConcurrentHashMap<>();

    public AggTradeAggregator(AggTradeDao aggTradeDao) {
        this.aggTradeDao = aggTradeDao;
    }

    /**
     * buyerIsMaker=true => taker SELL (공격매도)
     * buyerIsMaker=false => taker BUY (공격매수)
     */
    public void onAggTrade(String symbol, Instant eventTime, double price, double qty, boolean buyerIsMaker) {
        Instant minute = TimeUtil.floorToMinute(eventTime);
        long key = minute.toEpochMilli();

        Bucket b = buckets.computeIfAbsent(key, k -> new Bucket());
        synchronized (b) {
            if (buyerIsMaker)
                b.takerSellQty += qty;
            else
                b.takerBuyQty += qty;

            b.tradeCount += 1;
            b.pvSum += price * qty;
            b.vSum += qty;
        }
    }

    /**
     * 매 분 지난 버킷들을 DB에 flush
     */
    public void flushExpired(String symbol) {
        Instant now = Instant.now();
        Instant curMinute = TimeUtil.floorToMinute(now);
        long curKey = curMinute.toEpochMilli();

        for (Map.Entry<Long, Bucket> e : buckets.entrySet()) {
            long key = e.getKey();
            if (key >= curKey)
                continue; // 현재 진행 중 분은 보류

            Bucket b = e.getValue();
            double buy, sell, vwap;
            int cnt;
            synchronized (b) {
                buy = b.takerBuyQty;
                sell = b.takerSellQty;
                cnt = b.tradeCount;
                vwap = (b.vSum > 0) ? (b.pvSum / b.vSum) : 0.0;
            }

            aggTradeDao.upsert1m(symbol, Instant.ofEpochMilli(key), buy, sell, cnt, (b.vSum > 0) ? vwap : null);
            buckets.remove(key);
        }
    }
}
