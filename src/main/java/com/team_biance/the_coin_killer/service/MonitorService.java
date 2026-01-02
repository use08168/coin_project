package com.team_biance.the_coin_killer.service;

import com.team_biance.the_coin_killer.dto.TableStatus;
import com.team_biance.the_coin_killer.mapper.MonitorMapper;
import com.team_biance.the_coin_killer.model.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class MonitorService {

    private final MonitorMapper monitorMapper;

    public MonitorService(MonitorMapper monitorMapper) {
        this.monitorMapper = monitorMapper;
    }

    public List<TableStatus> getTableStatuses() {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        List<TableStatus> list = new ArrayList<>();
        list.add(buildStatus("f_kline_1m", "1분 캔들", monitorMapper.countKline(), monitorMapper.latestKlineTime(),
                nowUtc));
        list.add(buildStatus("f_mark_1s", "마크가격(1초)", monitorMapper.countMark(), monitorMapper.latestMarkTime(),
                nowUtc));
        list.add(buildStatus("f_depth_snapshot_1s", "오더북 스냅샷(1초)", monitorMapper.countDepth(),
                monitorMapper.latestDepthTime(), nowUtc));
        list.add(buildStatus("f_forceorder", "강제청산", monitorMapper.countForceOrder(),
                monitorMapper.latestForceOrderTime(), nowUtc));
        list.add(buildStatus("f_aggtrade_1m", "체결 집계(1분)", monitorMapper.countAggTrade(),
                monitorMapper.latestAggTradeTime(), nowUtc));
        list.add(buildStatus("feature_minute", "피처(1분)", monitorMapper.countFeatureMinute(),
                monitorMapper.latestFeatureMinuteTime(), nowUtc));

        return list;
    }

    public DashboardRecentData getRecentData(int limit) {
        DashboardRecentData d = new DashboardRecentData();
        d.klines = monitorMapper.recentKlines(limit);
        d.marks = monitorMapper.recentMarks(limit);
        d.depths = monitorMapper.recentDepths(limit);
        d.forceOrders = monitorMapper.recentForceOrders(limit);
        d.aggTrades = monitorMapper.recentAggTrades(limit);
        d.featureMinutes = monitorMapper.recentFeatureMinutes(limit);
        return d;
    }

    // -------------------------
    // 내부 헬퍼 / DTO
    // -------------------------

    private TableStatus buildStatus(String tableName, String displayName, long count, LocalDateTime latest,
            LocalDateTime nowUtc) {
        TableStatus st = new TableStatus();
        st.setTableName(tableName);
        st.setDisplayName(displayName);
        st.setTotalCount(count);
        st.setLatestTime(latest);

        if (latest == null) {
            st.setTimeAgo("데이터 없음");
            st.setStatus("STOPPED");
            st.setStatusColor("red");
            return st;
        }

        Duration d = Duration.between(latest, nowUtc);
        long seconds = Math.max(0, d.getSeconds());
        long minutes = seconds / 60;

        st.setTimeAgo(formatAgo(seconds));

        if (minutes <= 2) {
            st.setStatus("NORMAL");
            st.setStatusColor("green");
        } else if (minutes <= 5) {
            st.setStatus("DELAYED");
            st.setStatusColor("yellow");
        } else {
            st.setStatus("STOPPED");
            st.setStatusColor("red");
        }
        return st;
    }

    private String formatAgo(long seconds) {
        if (seconds < 10)
            return "방금 전";
        if (seconds < 60)
            return seconds + "초 전";
        long minutes = seconds / 60;
        if (minutes < 60)
            return minutes + "분 전";
        long hours = minutes / 60;
        if (hours < 24)
            return hours + "시간 전";
        long days = hours / 24;
        return days + "일 전";
    }

    public static class DashboardRecentData {
        public List<FKline1m> klines;
        public List<FMark1s> marks;
        public List<FDepthSnapshot1s> depths;
        public List<FForceOrder> forceOrders;
        public List<FAggTrade1m> aggTrades;
        public List<FeatureMinute> featureMinutes;
    }
}
