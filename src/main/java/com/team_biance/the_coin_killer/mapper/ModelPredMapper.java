package com.team_biance.the_coin_killer.mapper;

import com.team_biance.the_coin_killer.model.ModelPred60m;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ModelPredMapper {
    void insertPrediction(ModelPred60m pred);

    List<ModelPred60m> recentPredictions(@Param("symbol") String symbol, @Param("limit") int limit);
}
