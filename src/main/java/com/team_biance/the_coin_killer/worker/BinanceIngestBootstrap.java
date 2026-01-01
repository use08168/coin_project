package com.team_biance.the_coin_killer.worker;

import com.team_biance.the_coin_killer.service.binance.AggTradeAggregator;
import com.team_biance.the_coin_killer.service.binance.BinanceWsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class BinanceIngestBootstrap {

    @Value("${binance.symbol}")
    private String symbol;

    private final BinanceWsClient wsClient;
    private final AggTradeAggregator aggTradeAggregator;

    public BinanceIngestBootstrap(BinanceWsClient wsClient, AggTradeAggregator aggTradeAggregator) {
        this.wsClient = wsClient;
        this.aggTradeAggregator = aggTradeAggregator;
    }

    @PostConstruct
    public void start() {
        wsClient.start();
    }

    @Scheduled(cron = "*/10 * * * * *") // 10초마다
    public void flushAggTrades() {
        aggTradeAggregator.flushExpired(symbol);
    }
}
