package com.team_biance.the_coin_killer.dto;

import java.time.LocalDateTime;

public class ModelStatusDto {
    private String symbol;
    private String modelPath;
    private boolean modelExists;
    private LocalDateTime lastModifiedUtc;
    private String modelVersion; // meta 파일에서 읽음(없으면 null)
    private LocalDateTime trainedAtUtc;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public boolean isModelExists() {
        return modelExists;
    }

    public void setModelExists(boolean modelExists) {
        this.modelExists = modelExists;
    }

    public LocalDateTime getLastModifiedUtc() {
        return lastModifiedUtc;
    }

    public void setLastModifiedUtc(LocalDateTime lastModifiedUtc) {
        this.lastModifiedUtc = lastModifiedUtc;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public LocalDateTime getTrainedAtUtc() {
        return trainedAtUtc;
    }

    public void setTrainedAtUtc(LocalDateTime trainedAtUtc) {
        this.trainedAtUtc = trainedAtUtc;
    }
}
