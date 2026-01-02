package com.team_biance.the_coin_killer.mapper;

import com.team_biance.the_coin_killer.model.FDepthSnapshot1s;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DepthSnapshotMapper {
    int upsert(FDepthSnapshot1s row);
}
