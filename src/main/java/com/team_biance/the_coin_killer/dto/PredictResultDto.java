package com.team_biance.the_coin_killer.dto;

import java.time.LocalDateTime;

public class PredictResultDto {
    private boolean ok;
    private String symbol;
    private LocalDateTime timestamp; // UTC 기준
    private double currentClose;
    private int prediction;
    private String predictionLabel;
    private double probability;
    private String modelVersion;
    private int featuresUsed;
    private String errorCode;
    private String errorMessage;
    private long durationMs;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public double getCurrentClose() {
        return currentClose;
    }

    public void setCurrentClose(double currentClose) {
        this.currentClose = currentClose;
    }

    public int getPrediction() {
        return prediction;
    }

    public void setPrediction(int prediction) {
        this.prediction = prediction;
    }

    public String getPredictionLabel() {
        return predictionLabel;
    }

    public void setPredictionLabel(String predictionLabel) {
        this.predictionLabel = predictionLabel;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public int getFeaturesUsed() {
        return featuresUsed;
    }

    public void setFeaturesUsed(int featuresUsed) {
        this.featuresUsed = featuresUsed;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}
