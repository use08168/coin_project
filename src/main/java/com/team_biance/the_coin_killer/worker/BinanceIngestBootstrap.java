package com.team_biance.the_coin_killer.worker;

import com.team_biance.the_coin_killer.service.binance.AggTradeAggregator;
import com.team_biance.the_coin_killer.service.binance.BinanceIngestState;
import com.team_biance.the_coin_killer.service.binance.BinanceWsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;

@Component
public class BinanceIngestBootstrap {

    @Value("${binance.symbol:BTCUSDT}")
    private String symbol;

    private final BinanceWsClient wsClient;
    private final AggTradeAggregator aggTradeAggregator;
    private final BinanceIngestState state;

    public BinanceIngestBootstrap(BinanceWsClient wsClient, AggTradeAggregator aggTradeAggregator,
            BinanceIngestState state) {
        this.wsClient = wsClient;
        this.aggTradeAggregator = aggTradeAggregator;
        this.state = state;
    }

    @PostConstruct
    public void start() {
        wsClient.start();
    }

    @Scheduled(cron = "*/10 * * * * *") // 10초마다 aggTrade 분버킷 flush
    public void flushAggTrades() {
        aggTradeAggregator.flushExpired(symbol);
    }

    @Scheduled(cron = "*/15 * * * * *") // 15초마다 WS health check
    public void ensureWsAlive() {
        Instant last = state.lastWsMsgAt.get();
        if (last == null)
            return;

        if (Duration.between(last, Instant.now()).toSeconds() > 30) {
            state.lastError.set("WS stale >30s -> restart");
            wsClient.restart();
        }
    }
}
