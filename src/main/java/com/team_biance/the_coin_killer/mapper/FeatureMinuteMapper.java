package com.team_biance.the_coin_killer.mapper;

import com.team_biance.the_coin_killer.model.FeatureMinute;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FeatureMinuteMapper {
    int upsert(FeatureMinute row);
}
