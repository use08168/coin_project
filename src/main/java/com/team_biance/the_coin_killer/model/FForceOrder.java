package com.team_biance.the_coin_killer.model;

import java.time.LocalDateTime;

public class FForceOrder {
    private Long id; // AUTO_INCREMENT
    private String symbol;
    private LocalDateTime eventUtc;
    private String side; // BUY/SELL
    private double price;
    private double qty;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDateTime getEventUtc() {
        return eventUtc;
    }

    public void setEventUtc(LocalDateTime eventUtc) {
        this.eventUtc = eventUtc;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
