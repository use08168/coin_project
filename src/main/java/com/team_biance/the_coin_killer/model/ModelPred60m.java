package com.team_biance.the_coin_killer.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ModelPred60m {
    private Long id;
    private String symbol;
    private LocalDateTime tsUtc;
    private BigDecimal currentClose;
    private Integer prediction;
    private BigDecimal probability;
    private String modelVersion;
    private LocalDateTime createdAt;

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

    public LocalDateTime getTsUtc() {
        return tsUtc;
    }

    public void setTsUtc(LocalDateTime tsUtc) {
        this.tsUtc = tsUtc;
    }

    public BigDecimal getCurrentClose() {
        return currentClose;
    }

    public void setCurrentClose(BigDecimal currentClose) {
        this.currentClose = currentClose;
    }

    public Integer getPrediction() {
        return prediction;
    }

    public void setPrediction(Integer prediction) {
        this.prediction = prediction;
    }

    public BigDecimal getProbability() {
        return probability;
    }

    public void setProbability(BigDecimal probability) {
        this.probability = probability;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
