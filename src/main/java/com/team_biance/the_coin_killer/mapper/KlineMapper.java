package com.team_biance.the_coin_killer.mapper;

import com.team_biance.the_coin_killer.model.FKline1m;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KlineMapper {
    int upsert(FKline1m row);
}
