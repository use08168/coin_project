package com.team_biance.the_coin_killer.model;

import java.time.LocalDateTime;

public class FMark1s {
    private String symbol;
    private LocalDateTime tsUtc;

    private double markPrice;
    private Double indexPrice;
    private Double fundingRate;
    private LocalDateTime nextFundingUtc;

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

    public double getMarkPrice() {
        return markPrice;
    }

    public void setMarkPrice(double markPrice) {
        this.markPrice = markPrice;
    }

    public Double getIndexPrice() {
        return indexPrice;
    }

    public void setIndexPrice(Double indexPrice) {
        this.indexPrice = indexPrice;
    }

    public Double getFundingRate() {
        return fundingRate;
    }

    public void setFundingRate(Double fundingRate) {
        this.fundingRate = fundingRate;
    }

    public LocalDateTime getNextFundingUtc() {
        return nextFundingUtc;
    }

    public void setNextFundingUtc(LocalDateTime nextFundingUtc) {
        this.nextFundingUtc = nextFundingUtc;
    }
}
