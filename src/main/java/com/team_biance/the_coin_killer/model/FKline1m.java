package com.team_biance.the_coin_killer.model;

import java.time.LocalDateTime;

public class FKline1m {
    private String symbol;
    private LocalDateTime tsUtc;

    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;

    private int tradeCount;

    private Double quoteVolume;
    private Double takerBuyVol;
    private Double takerBuyQv;

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

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public int getTradeCount() {
        return tradeCount;
    }

    public void setTradeCount(int tradeCount) {
        this.tradeCount = tradeCount;
    }

    public Double getQuoteVolume() {
        return quoteVolume;
    }

    public void setQuoteVolume(Double quoteVolume) {
        this.quoteVolume = quoteVolume;
    }

    public Double getTakerBuyVol() {
        return takerBuyVol;
    }

    public void setTakerBuyVol(Double takerBuyVol) {
        this.takerBuyVol = takerBuyVol;
    }

    public Double getTakerBuyQv() {
        return takerBuyQv;
    }

    public void setTakerBuyQv(Double takerBuyQv) {
        this.takerBuyQv = takerBuyQv;
    }
}
