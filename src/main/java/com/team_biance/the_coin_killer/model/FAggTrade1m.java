package com.team_biance.the_coin_killer.model;

import java.time.LocalDateTime;

public class FAggTrade1m {
    private String symbol;
    private LocalDateTime tsUtc;

    private double takerBuyQty;
    private double takerSellQty;
    private int tradeCount;
    private Double vwapPrice;

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

    public double getTakerBuyQty() {
        return takerBuyQty;
    }

    public void setTakerBuyQty(double takerBuyQty) {
        this.takerBuyQty = takerBuyQty;
    }

    public double getTakerSellQty() {
        return takerSellQty;
    }

    public void setTakerSellQty(double takerSellQty) {
        this.takerSellQty = takerSellQty;
    }

    public int getTradeCount() {
        return tradeCount;
    }

    public void setTradeCount(int tradeCount) {
        this.tradeCount = tradeCount;
    }

    public Double getVwapPrice() {
        return vwapPrice;
    }

    public void setVwapPrice(Double vwapPrice) {
        this.vwapPrice = vwapPrice;
    }
}
