package com.team_biance.the_coin_killer.mapper;

import com.team_biance.the_coin_killer.model.FForceOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ForceOrderMapper {
    int insert(FForceOrder row);
}
