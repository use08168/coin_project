package com.team_biance.the_coin_killer.scheduler;

import com.team_biance.the_coin_killer.service.FeatureCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;

@Component
public class FeatureScheduler {

    private static final Logger log = LoggerFactory.getLogger(FeatureScheduler.class);

    private final FeatureCalculationService service;
    private final String symbol;

    public FeatureScheduler(
            FeatureCalculationService service,
            @Value("${binance.symbol:BTCUSDT}") String symbol) {
        this.service = service;
        this.symbol = symbol;
    }

    /**
     * 매 1분마다 실행.
     * - zone=UTC 기준
     * - "직전 완성된 1분" (예: 10:05:00 실행이면 10:04:00 계산)
     */
    @Scheduled(cron = "0 * * * * *", zone = "UTC")
    public void runEveryMinute() {
        try {
            LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC)
                    .withSecond(0).withNano(0);

            LocalDateTime targetMinute = nowUtc.minusMinutes(1); // 직전 1분 시작
            service.calculateFeatureMinute(symbol, targetMinute);

        } catch (Exception e) {
            log.error("[FEATURE][SCHED] failed: {}", e.getMessage(), e);
        }
    }
}
