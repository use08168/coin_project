package com.team_biance.the_coin_killer.mapper;

import com.team_biance.the_coin_killer.model.FAggTrade1m;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AggTradeMapper {
    int upsert(FAggTrade1m row);
}
