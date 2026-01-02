package com.team_biance.the_coin_killer.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team_biance.the_coin_killer.dto.binance.*;
import com.team_biance.the_coin_killer.event.*;
import com.team_biance.the_coin_killer.mapper.*;
import com.team_biance.the_coin_killer.model.*;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

@Component
public class BinanceDataPersistListener {

    private static final Logger log = LoggerFactory.getLogger(BinanceDataPersistListener.class);

    private final KlineMapper klineMapper;
    private final MarkPriceMapper markPriceMapper;
    private final ForceOrderMapper forceOrderMapper;
    private final DepthSnapshotMapper depthSnapshotMapper;
    private final AggTradeMapper aggTradeMapper;

    private final ObjectMapper objectMapper;

    // Depth: "1초에 한 번만 저장"용 (symbol -> lastSavedEpochSecond)
    private final Map<String, Long> lastDepthSavedSecond = new ConcurrentHashMap<>();

    // AggTrade: 1분 집계용 (symbol -> state)
    private final Map<String, AggState> aggStates = new ConcurrentHashMap<>();

    public BinanceDataPersistListener(
            KlineMapper klineMapper,
            MarkPriceMapper markPriceMapper,
            ForceOrderMapper forceOrderMapper,
            DepthSnapshotMapper depthSnapshotMapper,
            AggTradeMapper aggTradeMapper,
            ObjectMapper objectMapper) {
        this.klineMapper = klineMapper;
        this.markPriceMapper = markPriceMapper;
        this.forceOrderMapper = forceOrderMapper;
        this.depthSnapshotMapper = depthSnapshotMapper;
        this.aggTradeMapper = aggTradeMapper;
        this.objectMapper = objectMapper;
    }

    // =========================
    // 1) KLINE (완성 캔들만)
    // =========================
    @EventListener
    public void onKline(KlineStreamEvent event) {
        try {
            KlineEvent payload = event.getPayload();
            if (payload == null || payload.kline() == null)
                return;

            KlineEvent.Kline k = payload.kline();
            if (!k.isFinal())
                return; // 완성된 캔들만 저장

            FKline1m row = new FKline1m();
            row.setSymbol(payload.symbol());
            row.setTsUtc(utcFromMs(k.startTime())); // 캔들 시작 시각
            row.setOpen(parseDouble(k.open()));
            row.setHigh(parseDouble(k.high()));
            row.setLow(parseDouble(k.low()));
            row.setClose(parseDouble(k.close()));
            row.setVolume(parseDouble(k.volume()));
            row.setTradeCount((int) k.numberOfTrades());

            row.setQuoteVolume(parseDoubleOrNull(k.quoteAssetVolume()));
            row.setTakerBuyVol(parseDoubleOrNull(k.takerBuyBaseAssetVolume()));
            row.setTakerBuyQv(parseDoubleOrNull(k.takerBuyQuoteAssetVolume()));

            klineMapper.upsert(row);

        } catch (Exception e) {
            log.error("[PERSIST][KLINE] failed: {}", e.getMessage(), e);
        }
    }

    // =========================
    // 2) MARK PRICE (매초 저장)
    // =========================
    @EventListener
    public void onMarkPrice(MarkPriceStreamEvent event) {
        try {
            MarkPriceEvent payload = event.getPayload();
            if (payload == null)
                return;

            long tsMs = truncateToSecondMs(payload.eventTime());

            FMark1s row = new FMark1s();
            row.setSymbol(payload.symbol());
            row.setTsUtc(utcFromMs(tsMs));
            row.setMarkPrice(parseDouble(payload.markPrice()));
            row.setIndexPrice(parseDoubleOrNull(payload.indexPrice()));
            row.setFundingRate(parseDoubleOrNull(payload.fundingRate()));

            // nextFundingTime은 ms epoch로 내려옴
            if (payload.nextFundingTime() > 0) {
                row.setNextFundingUtc(utcFromMs(payload.nextFundingTime()));
            } else {
                row.setNextFundingUtc(null);
            }

            markPriceMapper.upsert(row);

        } catch (Exception e) {
            log.error("[PERSIST][MARK] failed: {}", e.getMessage(), e);
        }
    }

    // =========================
    // 3) FORCE ORDER (전부 저장)
    // =========================
    @EventListener
    public void onForceOrder(ForceOrderStreamEvent event) {
        try {
            ForceOrderEvent payload = event.getPayload();
            if (payload == null || payload.order() == null)
                return;

            ForceOrderEvent.ForceOrder o = payload.order();

            FForceOrder row = new FForceOrder();
            row.setSymbol(o.symbol());
            row.setEventUtc(utcFromMs(payload.eventTime()));
            row.setSide(o.side());

            // 가격: avgPrice 우선(있고 0이 아니면), 아니면 price
            double price = parseDoubleOrZero(o.avgPrice());
            if (price <= 0.0)
                price = parseDoubleOrZero(o.price());

            row.setPrice(price);

            // 수량: originalQty 기준
            row.setQty(parseDoubleOrZero(o.originalQty()));

            row.setStatus(o.orderStatus());

            forceOrderMapper.insert(row);

        } catch (Exception e) {
            log.error("[PERSIST][FORCE] failed: {}", e.getMessage(), e);
        }
    }

    // =========================
    // 4) DEPTH (1초 스냅샷)
    // - 100ms마다 오지만 1초에 1번만 저장
    // - bids/asks JSON->GZIP->BLOB
    // - 파생값 계산
    // =========================
    @EventListener
    public void onDepth(DepthStreamEvent event) {
        try {
            DepthEvent payload = event.getPayload();
            if (payload == null)
                return;

            String symbol = payload.symbol() != null ? payload.symbol() : "BTCUSDT";

            // 타임스탬프 선택: transactionTime > eventTime > now
            long baseMs = firstNonNullLong(payload.transactionTime(), payload.eventTime(), System.currentTimeMillis());
            long tsMs = truncateToSecondMs(baseMs);
            long epochSec = tsMs / 1000L;

            // 1초 이내 스킵
            AtomicBoolean shouldStore = new AtomicBoolean(false);
            lastDepthSavedSecond.compute(symbol, (k, prev) -> {
                if (prev == null || prev != epochSec) {
                    shouldStore.set(true);
                    return epochSec;
                }
                return prev;
            });
            if (!shouldStore.get())
                return;

            List<DepthEvent.DepthLevel> bids = payload.bids();
            List<DepthEvent.DepthLevel> asks = payload.asks();
            if (bids == null || bids.isEmpty() || asks == null || asks.isEmpty())
                return;

            // best bid/ask
            double bestBid = parseDoubleOrZero(bids.get(0).price());
            double bestAsk = parseDoubleOrZero(asks.get(0).price());
            if (bestBid <= 0 || bestAsk <= 0)
                return;

            double mid = (bestBid + bestAsk) / 2.0;
            double spreadBps = (bestAsk - bestBid) / mid * 10000.0;

            // sum(qty*price)
            double bidSum = sumNotionalTopN(bids);
            double askSum = sumNotionalTopN(asks);

            double denom = (bidSum + askSum);
            double imbalance = (denom == 0.0) ? 0.0 : (bidSum - askSum) / denom;

            // microprice
            double bidQty0 = parseDoubleOrZero(bids.get(0).qty());
            double askQty0 = parseDoubleOrZero(asks.get(0).qty());
            Double microprice = null;
            Double micropriceGapBps = null;

            double qtyDenom = bidQty0 + askQty0;
            if (qtyDenom > 0.0) {
                microprice = (bestBid * askQty0 + bestAsk * bidQty0) / qtyDenom;
                micropriceGapBps = (microprice - mid) / mid * 10000.0;
            }

            // bids/asks -> JSON bytes -> gzip
            byte[] bidsJson = objectMapper.writeValueAsBytes(bids);
            byte[] asksJson = objectMapper.writeValueAsBytes(asks);
            byte[] bidsGz = gzip(bidsJson);
            byte[] asksGz = gzip(asksJson);

            FDepthSnapshot1s row = new FDepthSnapshot1s();
            row.setSymbol(symbol);
            row.setTsUtc(utcFromMs(tsMs));

            row.setBestBid(bestBid);
            row.setBestAsk(bestAsk);
            row.setMidPrice(mid);
            row.setSpreadBps(spreadBps);

            row.setDepthBidSumTop20(bidSum);
            row.setDepthAskSumTop20(askSum);
            row.setImbalanceTop20(imbalance);

            row.setMicroprice(microprice);
            row.setMicropriceGapBps(micropriceGapBps);

            row.setBidsGzip(bidsGz);
            row.setAsksGzip(asksGz);
            row.setCompressAlgo("GZIP");

            depthSnapshotMapper.upsert(row);

        } catch (Exception e) {
            log.error("[PERSIST][DEPTH] failed: {}", e.getMessage(), e);
        }
    }

    private double sumNotionalTopN(List<DepthEvent.DepthLevel> levels) {
        double sum = 0.0;
        for (DepthEvent.DepthLevel lv : levels) {
            double p = parseDoubleOrZero(lv.price());
            double q = parseDoubleOrZero(lv.qty());
            sum += (p * q);
        }
        return sum;
    }

    // =========================
    // 5) AGG TRADE (1분 메모리 집계)
    // - 분이 바뀌면 이전 분 DB 저장 후 리셋
    // - isBuyerMaker(false)=taker buy, true=taker sell
    // - vwap = sum(price*qty)/sum(qty)
    // =========================
    @EventListener
    public void onAggTrade(AggTradeStreamEvent event) {
        try {
            AggTradeEvent payload = event.getPayload();
            if (payload == null)
                return;

            String symbol = payload.symbol();
            long tradeMs = payload.tradeTime(); // aggTrade의 T

            long minuteStartMs = floorToMinuteMs(tradeMs);

            AggState state = aggStates.computeIfAbsent(symbol, s -> new AggState(minuteStartMs));

            synchronized (state) {
                // 분이 바뀌면 flush 후 리셋
                if (state.minuteStartMs != minuteStartMs) {
                    flushAggState(symbol, state);
                    state.reset(minuteStartMs);
                }

                double qty = parseDoubleOrZero(payload.quantity());
                double price = parseDoubleOrZero(payload.price());

                if (!payload.buyerIsMaker()) {
                    // buyerIsMaker=false => taker BUY
                    state.takerBuyQty += qty;
                } else {
                    // buyerIsMaker=true => taker SELL
                    state.takerSellQty += qty;
                }

                state.tradeCount += 1;

                double notional = price * qty;
                state.sumNotional += notional;
                state.sumQty += qty;
            }

        } catch (Exception e) {
            log.error("[PERSIST][AGG] failed: {}", e.getMessage(), e);
        }
    }

    private void flushAggState(String symbol, AggState state) {
        // synchronized(state) 안에서 호출되는 용도
        try {
            if (state.tradeCount <= 0)
                return;

            Double vwap = (state.sumQty > 0.0) ? (state.sumNotional / state.sumQty) : null;

            FAggTrade1m row = new FAggTrade1m();
            row.setSymbol(symbol);
            row.setTsUtc(utcFromMs(state.minuteStartMs)); // 분 시작 시각
            row.setTakerBuyQty(state.takerBuyQty);
            row.setTakerSellQty(state.takerSellQty);
            row.setTradeCount(state.tradeCount);
            row.setVwapPrice(vwap);

            aggTradeMapper.upsert(row);

        } catch (Exception e) {
            log.error("[PERSIST][AGG][FLUSH] failed: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void onShutdownFlushAgg() {
        // 종료 시점에 현재 집계중인 분도 한번 저장
        for (Map.Entry<String, AggState> e : aggStates.entrySet()) {
            String symbol = e.getKey();
            AggState st = e.getValue();
            synchronized (st) {
                flushAggState(symbol, st);
            }
        }
    }

    // =========================
    // 내부 집계 상태
    // =========================
    private static class AggState {
        volatile long minuteStartMs;

        double takerBuyQty = 0.0;
        double takerSellQty = 0.0;
        int tradeCount = 0;

        double sumNotional = 0.0;
        double sumQty = 0.0;

        AggState(long minuteStartMs) {
            this.minuteStartMs = minuteStartMs;
        }

        void reset(long newMinuteStartMs) {
            this.minuteStartMs = newMinuteStartMs;
            this.takerBuyQty = 0.0;
            this.takerSellQty = 0.0;
            this.tradeCount = 0;
            this.sumNotional = 0.0;
            this.sumQty = 0.0;
        }
    }

    // =========================
    // 시간 변환 유틸 (UTC)
    // =========================
    public static LocalDateTime utcFromMs(long epochMs) {
        Instant inst = Instant.ofEpochMilli(epochMs);
        LocalDateTime ldt = LocalDateTime.ofInstant(inst, ZoneOffset.UTC);
        // DATETIME(3)에 맞춰 millis로 정규화
        int ms = ldt.getNano() / 1_000_000;
        return ldt.withNano(ms * 1_000_000);
    }

    public static long truncateToSecondMs(long epochMs) {
        return (epochMs / 1000L) * 1000L;
    }

    public static long floorToMinuteMs(long epochMs) {
        return (epochMs / 60_000L) * 60_000L;
    }

    private static long firstNonNullLong(Long a, Long b, long c) {
        if (a != null)
            return a;
        if (b != null)
            return b;
        return c;
    }

    // =========================
    // JSON -> GZIP
    // =========================
    public static byte[] gzip(byte[] input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(input);
        }
        return baos.toByteArray();
    }

    // =========================
    // parse helpers
    // =========================
    private static double parseDouble(String s) {
        if (s == null)
            throw new IllegalArgumentException("null numeric string");
        return Double.parseDouble(s);
    }

    private static double parseDoubleOrZero(String s) {
        if (s == null || s.isBlank())
            return 0.0;
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static Double parseDoubleOrNull(String s) {
        if (s == null || s.isBlank())
            return null;
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }
}
