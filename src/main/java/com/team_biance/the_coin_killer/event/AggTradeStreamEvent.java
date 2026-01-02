package com.team_biance.the_coin_killer.event;

import com.team_biance.the_coin_killer.dto.binance.AggTradeEvent;
import org.springframework.context.ApplicationEvent;

public class AggTradeStreamEvent extends ApplicationEvent {
    private final AggTradeEvent payload;

    public AggTradeStreamEvent(Object source, AggTradeEvent payload) {
        super(source);
        this.payload = payload;
    }

    public AggTradeEvent getPayload() {
        return payload;
    }
}
