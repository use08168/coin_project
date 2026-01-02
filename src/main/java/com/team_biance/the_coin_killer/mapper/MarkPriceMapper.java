package com.team_biance.the_coin_killer.mapper;

import com.team_biance.the_coin_killer.model.FMark1s;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MarkPriceMapper {
    int upsert(FMark1s row);
}
