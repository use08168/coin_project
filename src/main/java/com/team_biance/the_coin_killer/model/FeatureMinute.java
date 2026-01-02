package com.team_biance.the_coin_killer.model;

import java.time.LocalDateTime;

public class FeatureMinute {
    private String symbol;
    private LocalDateTime tsUtc;

    // 가격 기본 (6)
    private double open1m;
    private double high1m;
    private double low1m;
    private double close1m;
    private double volume1m;
    private int tradeCount1m;

    // 수익률 (4)
    private double ret1mLog;
    private double ret5mLog;
    private double ret15mLog;
    private double rangeBps1m;

    // 변동성 (2)
    private double rv15m;
    private double rv60m;

    // 거래량 (4)
    private double volZ60m;
    private double rvolTod;
    private double avgTradeSize1m;
    private double vwapGapBps;

    // 오더플로우 (5)
    private double takerBuyQty1m;
    private double takerSellQty1m;
    private double buyRatio1m;
    private double cvd1m;
    private double cvd15m;

    // 오더북 (6) - 해당 분 마지막 스냅샷(없으면 이전)
    private double midPrice1s;
    private double spreadBps1s;
    private double depthBidSumTop20;
    private double depthAskSumTop20;
    private double imbalanceTop20;
    private double micropriceGapBps;

    // 선물 특화 (3)
    private double markSpotBps;
    private double oiChg1m;
    private int liqCount1m;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDateTime getTsUtc() {
        return tsUtc;
    }

    public void setTsUtc(LocalDateTime tsUtc) {
        this.tsUtc = tsUtc;
    }

    public double getOpen1m() {
        return open1m;
    }

    public void setOpen1m(double open1m) {
        this.open1m = open1m;
    }

    public double getHigh1m() {
        return high1m;
    }

    public void setHigh1m(double high1m) {
        this.high1m = high1m;
    }

    public double getLow1m() {
        return low1m;
    }

    public void setLow1m(double low1m) {
        this.low1m = low1m;
    }

    public double getClose1m() {
        return close1m;
    }

    public void setClose1m(double close1m) {
        this.close1m = close1m;
    }

    public double getVolume1m() {
        return volume1m;
    }

    public void setVolume1m(double volume1m) {
        this.volume1m = volume1m;
    }

    public int getTradeCount1m() {
        return tradeCount1m;
    }

    public void setTradeCount1m(int tradeCount1m) {
        this.tradeCount1m = tradeCount1m;
    }

    public double getRet1mLog() {
        return ret1mLog;
    }

    public void setRet1mLog(double ret1mLog) {
        this.ret1mLog = ret1mLog;
    }

    public double getRet5mLog() {
        return ret5mLog;
    }

    public void setRet5mLog(double ret5mLog) {
        this.ret5mLog = ret5mLog;
    }

    public double getRet15mLog() {
        return ret15mLog;
    }

    public void setRet15mLog(double ret15mLog) {
        this.ret15mLog = ret15mLog;
    }

    public double getRangeBps1m() {
        return rangeBps1m;
    }

    public void setRangeBps1m(double rangeBps1m) {
        this.rangeBps1m = rangeBps1m;
    }

    public double getRv15m() {
        return rv15m;
    }

    public void setRv15m(double rv15m) {
        this.rv15m = rv15m;
    }

    public double getRv60m() {
        return rv60m;
    }

    public void setRv60m(double rv60m) {
        this.rv60m = rv60m;
    }

    public double getVolZ60m() {
        return volZ60m;
    }

    public void setVolZ60m(double volZ60m) {
        this.volZ60m = volZ60m;
    }

    public double getRvolTod() {
        return rvolTod;
    }

    public void setRvolTod(double rvolTod) {
        this.rvolTod = rvolTod;
    }

    public double getAvgTradeSize1m() {
        return avgTradeSize1m;
    }

    public void setAvgTradeSize1m(double avgTradeSize1m) {
        this.avgTradeSize1m = avgTradeSize1m;
    }

    public double getVwapGapBps() {
        return vwapGapBps;
    }

    public void setVwapGapBps(double vwapGapBps) {
        this.vwapGapBps = vwapGapBps;
    }

    public double getTakerBuyQty1m() {
        return takerBuyQty1m;
    }

    public void setTakerBuyQty1m(double takerBuyQty1m) {
        this.takerBuyQty1m = takerBuyQty1m;
    }

    public double getTakerSellQty1m() {
        return takerSellQty1m;
    }

    public void setTakerSellQty1m(double takerSellQty1m) {
        this.takerSellQty1m = takerSellQty1m;
    }

    public double getBuyRatio1m() {
        return buyRatio1m;
    }

    public void setBuyRatio1m(double buyRatio1m) {
        this.buyRatio1m = buyRatio1m;
    }

    public double getCvd1m() {
        return cvd1m;
    }

    public void setCvd1m(double cvd1m) {
        this.cvd1m = cvd1m;
    }

    public double getCvd15m() {
        return cvd15m;
    }

    public void setCvd15m(double cvd15m) {
        this.cvd15m = cvd15m;
    }

    public double getMidPrice1s() {
        return midPrice1s;
    }

    public void setMidPrice1s(double midPrice1s) {
        this.midPrice1s = midPrice1s;
    }

    public double getSpreadBps1s() {
        return spreadBps1s;
    }

    public void setSpreadBps1s(double spreadBps1s) {
        this.spreadBps1s = spreadBps1s;
    }

    public double getDepthBidSumTop20() {
        return depthBidSumTop20;
    }

    public void setDepthBidSumTop20(double depthBidSumTop20) {
        this.depthBidSumTop20 = depthBidSumTop20;
    }

    public double getDepthAskSumTop20() {
        return depthAskSumTop20;
    }

    public void setDepthAskSumTop20(double depthAskSumTop20) {
        this.depthAskSumTop20 = depthAskSumTop20;
    }

    public double getImbalanceTop20() {
        return imbalanceTop20;
    }

    public void setImbalanceTop20(double imbalanceTop20) {
        this.imbalanceTop20 = imbalanceTop20;
    }

    public double getMicropriceGapBps() {
        return micropriceGapBps;
    }

    public void setMicropriceGapBps(double micropriceGapBps) {
        this.micropriceGapBps = micropriceGapBps;
    }

    public double getMarkSpotBps() {
        return markSpotBps;
    }

    public void setMarkSpotBps(double markSpotBps) {
        this.markSpotBps = markSpotBps;
    }

    public double getOiChg1m() {
        return oiChg1m;
    }

    public void setOiChg1m(double oiChg1m) {
        this.oiChg1m = oiChg1m;
    }

    public int getLiqCount1m() {
        return liqCount1m;
    }

    public void setLiqCount1m(int liqCount1m) {
        this.liqCount1m = liqCount1m;
    }
}
