package com.team_biance.the_coin_killer.service;

import com.team_biance.the_coin_killer.dto.TableStatus;
import com.team_biance.the_coin_killer.mapper.MonitorMapper;
import com.team_biance.the_coin_killer.model.FAggTrade1m;
import com.team_biance.the_coin_killer.model.FDepthSnapshot1s;
import com.team_biance.the_coin_killer.model.FForceOrder;
import com.team_biance.the_coin_killer.model.FKline1m;
import com.team_biance.the_coin_killer.model.FMark1s;
import com.team_biance.the_coin_killer.model.FeatureMinute;
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

    // =========================
    // Dashboard: Table Statuses
    // =========================
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

    // =========================
    // Dashboard: Recent Data
    // =========================
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

    /**
     * ✅ MonitorController에서 MonitorService.DashboardRecentData 로 타입 참조 가능하게
     * public static 으로 선언
     */
    public static class DashboardRecentData {
        public List<FKline1m> klines;
        public List<FMark1s> marks;
        public List<FDepthSnapshot1s> depths;
        public List<FForceOrder> forceOrders;
        public List<FAggTrade1m> aggTrades;
        public List<FeatureMinute> featureMinutes;
    }

    // =========================
    // Compare: Realtime Snapshot API
    // GET /api/monitor/realtime
    // =========================
    public RealtimeResponse getRealtimeSnapshot() {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        FKline1m k = firstOrNull(monitorMapper.recentKlines(1));
        FMark1s m = firstOrNull(monitorMapper.recentMarks(1));
        FDepthSnapshot1s d = firstOrNull(monitorMapper.recentDepths(1));
        FeatureMinute f = firstOrNull(monitorMapper.recentFeatureMinutes(1));

        RealtimeResponse.Kline kDto = null;
        if (k != null) {
            kDto = new RealtimeResponse.Kline(
                    k.getTsUtc(),
                    nz(k.getOpen()),
                    nz(k.getHigh()),
                    nz(k.getLow()),
                    nz(k.getClose()),
                    nz(k.getVolume()),
                    nzInt(k.getTradeCount()));
        }

        RealtimeResponse.Mark markDto = null;
        if (m != null) {
            markDto = new RealtimeResponse.Mark(
                    nz(m.getMarkPrice()),
                    nzNullable(m.getIndexPrice()),
                    nzNullable(m.getFundingRate()));
        }

        RealtimeResponse.Depth depthDto = null;
        if (d != null) {
            depthDto = new RealtimeResponse.Depth(
                    nz(d.getMidPrice()),
                    nz(d.getSpreadBps()),
                    nz(d.getImbalanceTop20()));
        }

        RealtimeResponse.Feature featDto = null;
        if (f != null) {
            featDto = new RealtimeResponse.Feature(
                    nz(f.getRet1mLog()),
                    nz(f.getRv15m()),
                    nz(f.getCvd15m()),
                    nz(f.getBuyRatio1m()));
        }

        return new RealtimeResponse(nowUtc, kDto, markDto, depthDto, featDto);
    }

    public record RealtimeResponse(
            LocalDateTime timestamp,
            Kline kline,
            Mark mark,
            Depth depth,
            Feature feature) {
        public record Kline(
                LocalDateTime tsUtc,
                double open,
                double high,
                double low,
                double close,
                double volume,
                int tradeCount) {
        }

        public record Mark(
                double markPrice,
                Double indexPrice,
                Double fundingRate) {
        }

        public record Depth(
                double midPrice,
                double spreadBps,
                double imbalance) {
        }

        public record Feature(
                double ret1mLog,
                double rv15m,
                double cvd15m,
                double buyRatio1m) {
        }
    }

    // =========================
    // Internal Helpers
    // =========================

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

    private static <T> T firstOrNull(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    private static double nz(Double v) {
        return (v == null) ? 0.0 : v;
    }

    private static Double nzNullable(Double v) {
        return v; // null 허용
    }

    private static int nzInt(Integer v) {
        return (v == null) ? 0 : v;
    }

    // 오버로드: 모델에서 primitive를 리턴해도 boxing으로 들어와서 문제 없음
    private static double nz(double v) {
        return v;
    }

    private static int nzInt(int v) {
        return v;
    }
}
