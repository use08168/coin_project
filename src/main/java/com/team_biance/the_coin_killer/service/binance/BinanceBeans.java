package com.team_biance.the_coin_killer.service.binance;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BinanceBeans {
    @Bean
    public DepthCache depthCache() {
        return new DepthCache();
    }
}
