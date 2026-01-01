package com.team_biance.the_coin_killer.worker;

import com.team_biance.the_coin_killer.service.PredictionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ModelPredictScheduler {

    private final PredictionService predictionService;

    public ModelPredictScheduler(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    /**
     * 매 1분마다 최신 feature_hour로 예측 시도
     * feature_hour가 없거나 python이 안 떠있으면 조용히 스킵
     */
    @Scheduled(cron = "5 * * * * *") // 매 분 5초에 실행
    public void run() {
        predictionService.predictLatest("BTCUSDT");
    }
}
