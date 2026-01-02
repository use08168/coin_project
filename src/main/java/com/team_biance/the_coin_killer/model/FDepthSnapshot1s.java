package com.team_biance.the_coin_killer.model;

import java.time.LocalDateTime;

public class FDepthSnapshot1s {
    private String symbol;
    private LocalDateTime tsUtc;

    private double bestBid;
    private double bestAsk;
    private double midPrice;
    private double spreadBps;

    private double depthBidSumTop20;
    private double depthAskSumTop20;
    private double imbalanceTop20;

    private Double microprice;
    private Double micropriceGapBps;

    private byte[] bidsGzip;
    private byte[] asksGzip;

    private String compressAlgo = "GZIP";

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

    public double getBestBid() {
        return bestBid;
    }

    public void setBestBid(double bestBid) {
        this.bestBid = bestBid;
    }

    public double getBestAsk() {
        return bestAsk;
    }

    public void setBestAsk(double bestAsk) {
        this.bestAsk = bestAsk;
    }

    public double getMidPrice() {
        return midPrice;
    }

    public void setMidPrice(double midPrice) {
        this.midPrice = midPrice;
    }

    public double getSpreadBps() {
        return spreadBps;
    }

    public void setSpreadBps(double spreadBps) {
        this.spreadBps = spreadBps;
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

    public Double getMicroprice() {
        return microprice;
    }

    public void setMicroprice(Double microprice) {
        this.microprice = microprice;
    }

    public Double getMicropriceGapBps() {
        return micropriceGapBps;
    }

    public void setMicropriceGapBps(Double micropriceGapBps) {
        this.micropriceGapBps = micropriceGapBps;
    }

    public byte[] getBidsGzip() {
        return bidsGzip;
    }

    public void setBidsGzip(byte[] bidsGzip) {
        this.bidsGzip = bidsGzip;
    }

    public byte[] getAsksGzip() {
        return asksGzip;
    }

    public void setAsksGzip(byte[] asksGzip) {
        this.asksGzip = asksGzip;
    }

    public String getCompressAlgo() {
        return compressAlgo;
    }

    public void setCompressAlgo(String compressAlgo) {
        this.compressAlgo = compressAlgo;
    }
}
