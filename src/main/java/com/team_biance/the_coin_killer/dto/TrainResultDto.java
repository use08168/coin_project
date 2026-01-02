package com.team_biance.the_coin_killer.dto;

public class TrainResultDto {
    private boolean ok;
    private String symbol;
    private int days;
    private int rowsUsed;
    private int trainRows;
    private int testRows;
    private int featuresUsed;
    private String modelPath;
    private String modelVersion;
    private TrainMetrics metrics;
    private String errorCode;
    private String errorMessage;
    private long durationMs;

    public static class TrainMetrics {
        private double accuracy;
        private double precision;
        private double recall;
        private double f1;
        private double mda;

        public double getAccuracy() {
            return accuracy;
        }

        public void setAccuracy(double accuracy) {
            this.accuracy = accuracy;
        }

        public double getPrecision() {
            return precision;
        }

        public void setPrecision(double precision) {
            this.precision = precision;
        }

        public double getRecall() {
            return recall;
        }

        public void setRecall(double recall) {
            this.recall = recall;
        }

        public double getF1() {
            return f1;
        }

        public void setF1(double f1) {
            this.f1 = f1;
        }

        public double getMda() {
            return mda;
        }

        public void setMda(double mda) {
            this.mda = mda;
        }
    }

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

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public int getRowsUsed() {
        return rowsUsed;
    }

    public void setRowsUsed(int rowsUsed) {
        this.rowsUsed = rowsUsed;
    }

    public int getTrainRows() {
        return trainRows;
    }

    public void setTrainRows(int trainRows) {
        this.trainRows = trainRows;
    }

    public int getTestRows() {
        return testRows;
    }

    public void setTestRows(int testRows) {
        this.testRows = testRows;
    }

    public int getFeaturesUsed() {
        return featuresUsed;
    }

    public void setFeaturesUsed(int featuresUsed) {
        this.featuresUsed = featuresUsed;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public TrainMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(TrainMetrics metrics) {
        this.metrics = metrics;
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
