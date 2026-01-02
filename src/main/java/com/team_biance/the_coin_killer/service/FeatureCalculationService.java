package com.team_biance.the_coin_killer.service;

import com.team_biance.the_coin_killer.mapper.FeatureMinuteMapper;
import com.team_biance.the_coin_killer.mapper.FeatureSourceMapper;
import com.team_biance.the_coin_killer.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeatureCalculationService {

    private static final Logger log = LoggerFactory.getLogger(FeatureCalculationService.class);

    private final FeatureSourceMapper sourceMapper;
    private final FeatureMinuteMapper featureMinuteMapper;

    public FeatureCalculationService(
            FeatureSourceMapper sourceMapper,
            FeatureMinuteMapper featureMinuteMapper) {
        this.sourceMapper = sourceMapper;
        this.featureMinuteMapper = featureMinuteMapper;
    }

    public void calculateFeatureMinute(String symbol, LocalDateTime minuteStartUtc) {
        try {
            // minuteStartUtc는 "해당 1분 시작"
            LocalDateTime minuteEndExclusive = minuteStartUtc.plusMinutes(1);

            // 1) 기준 캔들 (해당 분)
            FKline1m k0 = sourceMapper.getKlineAt(symbol, minuteStartUtc);
            if (k0 == null) {
                // 아직 캔들이 DB에 안 들어온 경우가 있음(경계 타이밍). 다음 분에 backfill 안 되므로 로그는 남김.
                log.warn("[FEATURE][1m] missing kline: symbol={}, ts={}", symbol, minuteStartUtc);
                return;
            }

            // 2) 최근 60분 kline 범위 (close/volume 계산용)
            // - from: minuteStartUtc - 60m ~ to: minuteEndExclusive
            LocalDateTime kFrom = minuteStartUtc.minusMinutes(60);
            LocalDateTime kTo = minuteEndExclusive;
            List<FKline1m> kline60 = sourceMapper.getKlineRange(symbol, kFrom, kTo);

            Map<LocalDateTime, FKline1m> kMap = kline60.stream()
                    .collect(Collectors.toMap(FKline1m::getTsUtc, x -> x, (a, b) -> b));

            double closeNow = nz(k0.getClose());

            // 3) 과거 close 조회 (없으면 0)
            double close1 = closeAtOrZero(kMap, minuteStartUtc.minusMinutes(1));
            double close5 = closeAtOrZero(kMap, minuteStartUtc.minusMinutes(5));
            double close15 = closeAtOrZero(kMap, minuteStartUtc.minusMinutes(15));

            double ret1m = logRet(closeNow, close1);
            double ret5m = logRet(closeNow, close5);
            double ret15m = logRet(closeNow, close15);

            double rangeBps = (closeNow > 0)
                    ? ((nz(k0.getHigh()) - nz(k0.getLow())) / closeNow * 10000.0)
                    : 0.0;

            // 4) rv15m, rv60m
            double rv15 = realizedVol(kMap, minuteStartUtc, 15);
            double rv60 = realizedVol(kMap, minuteStartUtc, 60);

            // 5) vol_z_60m: "이전 60분(현재 분 제외)" 기준 평균/표준편차
            double volZ60 = volumeZ60(kMap, minuteStartUtc, nz(k0.getVolume()));

            // 6) rvol_tod: 일단 1.0 고정
            double rvolTod = 1.0;

            // 7) avg_trade_size_1m
            double avgTradeSize = (k0.getTradeCount() > 0) ? (nz(k0.getVolume()) / k0.getTradeCount()) : 0.0;

            // 8) aggtrade (현재 분) + 15분 범위
            FAggTrade1m a0 = sourceMapper.getAggTradeAt(symbol, minuteStartUtc);

            double takerBuy = (a0 != null) ? nz(a0.getTakerBuyQty()) : 0.0;
            double takerSell = (a0 != null) ? nz(a0.getTakerSellQty()) : 0.0;

            double buyRatio = (takerBuy + takerSell > 0.0) ? (takerBuy / (takerBuy + takerSell)) : 0.0;
            double cvd1m = takerBuy - takerSell;

            // cvd_15m: 최근 15분(현재 포함) 합
            LocalDateTime aFrom = minuteStartUtc.minusMinutes(14);
            LocalDateTime aTo = minuteEndExclusive;
            List<FAggTrade1m> a15 = sourceMapper.getAggTradeRange(symbol, aFrom, aTo);

            double cvd15m = 0.0;
            for (FAggTrade1m r : a15) {
                cvd15m += nz(r.getTakerBuyQty()) - nz(r.getTakerSellQty());
            }

            // vwap_gap_bps
            double vwapGapBps = 0.0;
            if (a0 != null && a0.getVwapPrice() != null && a0.getVwapPrice() > 0.0) {
                double vwap = a0.getVwapPrice();
                vwapGapBps = (closeNow - vwap) / vwap * 10000.0;
            }

            // 9) 오더북: 해당 분 마지막 스냅샷(없으면 이전)
            FDepthSnapshot1s d = sourceMapper.getLatestDepthBefore(symbol, minuteEndExclusive);

            double mid1s = (d != null) ? nz(d.getMidPrice()) : 0.0;
            double spread1s = (d != null) ? nz(d.getSpreadBps()) : 0.0;
            double bidSumTop20 = (d != null) ? nz(d.getDepthBidSumTop20()) : 0.0;
            double askSumTop20 = (d != null) ? nz(d.getDepthAskSumTop20()) : 0.0;
            double imbTop20 = (d != null) ? nz(d.getImbalanceTop20()) : 0.0;
            double microGapBps = (d != null) ? nzNullable(d.getMicropriceGapBps()) : 0.0;

            // 10) mark_spot_bps: 해당 분 마지막 mark
            FMark1s m = sourceMapper.getLatestMarkBefore(symbol, minuteEndExclusive);
            double markSpotBps = 0.0;
            if (m != null && m.getMarkPrice() > 0.0 && closeNow > 0.0) {
                markSpotBps = (m.getMarkPrice() - closeNow) / closeNow * 10000.0;
            }

            // 11) oi_chg_1m: 테이블 없으면 0 고정
            double oiChg = 0.0;

            // 12) liq_count_1m: 해당 1분간 청산 횟수
            int liqCount = sourceMapper.countForceOrders(symbol, minuteStartUtc, minuteEndExclusive);

            // ====== 최종 row 구성 ======
            FeatureMinute out = new FeatureMinute();
            out.setSymbol(symbol);
            out.setTsUtc(minuteStartUtc);

            // 가격 기본
            out.setOpen1m(nz(k0.getOpen()));
            out.setHigh1m(nz(k0.getHigh()));
            out.setLow1m(nz(k0.getLow()));
            out.setClose1m(closeNow);
            out.setVolume1m(nz(k0.getVolume()));
            out.setTradeCount1m(k0.getTradeCount());

            // 수익률
            out.setRet1mLog(ret1m);
            out.setRet5mLog(ret5m);
            out.setRet15mLog(ret15m);
            out.setRangeBps1m(safeFinite(rangeBps));

            // 변동성
            out.setRv15m(safeFinite(rv15));
            out.setRv60m(safeFinite(rv60));

            // 거래량
            out.setVolZ60m(safeFinite(volZ60));
            out.setRvolTod(rvolTod);
            out.setAvgTradeSize1m(safeFinite(avgTradeSize));
            out.setVwapGapBps(safeFinite(vwapGapBps));

            // 오더플로우
            out.setTakerBuyQty1m(safeFinite(takerBuy));
            out.setTakerSellQty1m(safeFinite(takerSell));
            out.setBuyRatio1m(safeFinite(buyRatio));
            out.setCvd1m(safeFinite(cvd1m));
            out.setCvd15m(safeFinite(cvd15m));

            // 오더북
            out.setMidPrice1s(safeFinite(mid1s));
            out.setSpreadBps1s(safeFinite(spread1s));
            out.setDepthBidSumTop20(safeFinite(bidSumTop20));
            out.setDepthAskSumTop20(safeFinite(askSumTop20));
            out.setImbalanceTop20(safeFinite(imbTop20));
            out.setMicropriceGapBps(safeFinite(microGapBps));

            // 선물
            out.setMarkSpotBps(safeFinite(markSpotBps));
            out.setOiChg1m(safeFinite(oiChg));
            out.setLiqCount1m(liqCount);

            // upsert
            featureMinuteMapper.upsert(out);

            log.debug("[FEATURE][1m] saved: symbol={}, ts={}", symbol, minuteStartUtc);

        } catch (Exception e) {
            // 실패해도 다음 분 계속 진행
            log.error("[FEATURE][1m] failed: symbol={}, ts={}, err={}", symbol, minuteStartUtc, e.getMessage(), e);
        }
    }

    // ======================
    // 계산 유틸
    // ======================

    private static double closeAtOrZero(Map<LocalDateTime, FKline1m> kMap, LocalDateTime ts) {
        FKline1m k = kMap.get(ts);
        if (k == null)
            return 0.0;
        return nz(k.getClose());
    }

    private static double logRet(double now, double past) {
        if (now > 0.0 && past > 0.0) {
            return Math.log(now / past);
        }
        return 0.0;
    }

    /**
     * realized vol: 최근 N분 ret1m_log 표준편차 * sqrt(N)
     * - 정확히 N개 리턴이 있어야 계산 (하나라도 누락이면 0)
     */
    private static double realizedVol(Map<LocalDateTime, FKline1m> kMap, LocalDateTime minuteStart, int n) {
        // 필요한 close: minuteStart - n .. minuteStart (총 n+1개)
        double[] rets = new double[n];

        for (int i = 0; i < n; i++) {
            LocalDateTime tPrev = minuteStart.minusMinutes(n - i);
            LocalDateTime tNow = minuteStart.minusMinutes(n - i - 1);

            FKline1m kPrev = kMap.get(tPrev);
            FKline1m kNow = kMap.get(tNow);
            if (kPrev == null || kNow == null)
                return 0.0;

            double cPrev = nz(kPrev.getClose());
            double cNow = nz(kNow.getClose());
            if (cPrev <= 0.0 || cNow <= 0.0)
                return 0.0;

            rets[i] = Math.log(cNow / cPrev);
        }

        double std = stdPopulation(rets);
        return std * Math.sqrt(n);
    }

    /**
     * vol_z_60m:
     * - 이전 60분(현재 분 제외) volume 평균/표준편차
     * - std==0 or 데이터 부족이면 0
     */
    private static double volumeZ60(Map<LocalDateTime, FKline1m> kMap, LocalDateTime minuteStart, double currentVol) {
        double[] vols = new double[60];

        for (int i = 0; i < 60; i++) {
            LocalDateTime t = minuteStart.minusMinutes(60 - i); // -60 .. -1
            FKline1m k = kMap.get(t);
            if (k == null)
                return 0.0;
            vols[i] = nz(k.getVolume());
        }

        double mean = mean(vols);
        double std = stdPopulation(vols);
        if (std <= 0.0)
            return 0.0;

        return (currentVol - mean) / std;
    }

    private static double mean(double[] x) {
        double s = 0.0;
        for (double v : x)
            s += v;
        return s / x.length;
    }

    private static double stdPopulation(double[] x) {
        if (x.length <= 1)
            return 0.0;
        double m = mean(x);
        double s2 = 0.0;
        for (double v : x) {
            double d = v - m;
            s2 += d * d;
        }
        return Math.sqrt(s2 / x.length);
    }

    private static double nz(Double v) {
        return (v == null) ? 0.0 : v;
    }

    private static double nzNullable(Double v) {
        return (v == null) ? 0.0 : v;
    }

    private static double safeFinite(double v) {
        if (Double.isFinite(v))
            return v;
        return 0.0;
    }
}
