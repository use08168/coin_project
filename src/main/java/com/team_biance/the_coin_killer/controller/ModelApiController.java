package com.team_biance.the_coin_killer.controller;

import com.team_biance.the_coin_killer.dto.ModelStatusDto;
import com.team_biance.the_coin_killer.dto.PredictResultDto;
import com.team_biance.the_coin_killer.dto.TrainResultDto;
import com.team_biance.the_coin_killer.model.ModelPred60m;
import com.team_biance.the_coin_killer.service.ModelPredictService;
import com.team_biance.the_coin_killer.service.ModelTrainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/model")
public class ModelApiController {

    private final ModelTrainService modelTrainService;
    private final ModelPredictService modelPredictService;

    public ModelApiController(ModelTrainService modelTrainService, ModelPredictService modelPredictService) {
        this.modelTrainService = modelTrainService;
        this.modelPredictService = modelPredictService;
    }

    @PostMapping("/train")
    public ResponseEntity<TrainResultDto> train(
            @RequestParam(defaultValue = "BTCUSDT") String symbol,
            @RequestParam(defaultValue = "7") int days) {
        TrainResultDto dto = modelTrainService.train(symbol, days);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/predict")
    public ResponseEntity<PredictResultDto> predict(
            @RequestParam(defaultValue = "BTCUSDT") String symbol) {
        PredictResultDto dto = modelPredictService.predict(symbol);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/predictions")
    public List<ModelPred60m> recentPredictions(
            @RequestParam(defaultValue = "BTCUSDT") String symbol,
            @RequestParam(defaultValue = "10") int limit) {
        return modelPredictService.recentPredictions(symbol, limit);
    }

    @GetMapping("/status")
    public ModelStatusDto status(
            @RequestParam(defaultValue = "BTCUSDT") String symbol) {
        return modelTrainService.status(symbol);
    }
}
