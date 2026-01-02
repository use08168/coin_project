package com.team_biance.the_coin_killer.mapper;

import com.team_biance.the_coin_killer.model.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MonitorMapper {

    // count
    long countKline();

    long countMark();

    long countDepth();

    long countForceOrder();

    long countAggTrade();

    long countFeatureMinute();

    // latest time
    LocalDateTime latestKlineTime();

    LocalDateTime latestMarkTime();

    LocalDateTime latestDepthTime();

    LocalDateTime latestForceOrderTime();

    LocalDateTime latestAggTradeTime();

    LocalDateTime latestFeatureMinuteTime();

    // recent
    List<FKline1m> recentKlines(@Param("limit") int limit);

    List<FMark1s> recentMarks(@Param("limit") int limit);

    List<FDepthSnapshot1s> recentDepths(@Param("limit") int limit);

    List<FForceOrder> recentForceOrders(@Param("limit") int limit);

    List<FAggTrade1m> recentAggTrades(@Param("limit") int limit);

    List<FeatureMinute> recentFeatureMinutes(@Param("limit") int limit);
}
