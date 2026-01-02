package com.team_biance.the_coin_killer.dto;

import java.time.LocalDateTime;

public class TableStatus {
    private String tableName;
    private String displayName;
    private long totalCount;
    private LocalDateTime latestTime;
    private String timeAgo; // "2분 전"
    private String status; // "NORMAL", "DELAYED", "STOPPED"
    private String statusColor; // "green", "yellow", "red"

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public LocalDateTime getLatestTime() {
        return latestTime;
    }

    public void setLatestTime(LocalDateTime latestTime) {
        this.latestTime = latestTime;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusColor() {
        return statusColor;
    }

    public void setStatusColor(String statusColor) {
        this.statusColor = statusColor;
    }
}
