package com.team_biance.the_coin_killer.mapper;

import com.team_biance.the_coin_killer.model.FAggTrade1m;
import com.team_biance.the_coin_killer.model.FDepthSnapshot1s;
import com.team_biance.the_coin_killer.model.FKline1m;
import com.team_biance.the_coin_killer.model.FMark1s;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FeatureSourceMapper {

    FKline1m getKlineAt(@Param("symbol") String symbol, @Param("tsUtc") LocalDateTime tsUtc);

    List<FKline1m> getKlineRange(@Param("symbol") String symbol,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    FDepthSnapshot1s getLatestDepthBefore(@Param("symbol") String symbol,
            @Param("beforeTs") LocalDateTime beforeTs);

    FMark1s getLatestMarkBefore(@Param("symbol") String symbol,
            @Param("beforeTs") LocalDateTime beforeTs);

    FAggTrade1m getAggTradeAt(@Param("symbol") String symbol, @Param("tsUtc") LocalDateTime tsUtc);

    List<FAggTrade1m> getAggTradeRange(@Param("symbol") String symbol,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    int countForceOrders(@Param("symbol") String symbol,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
