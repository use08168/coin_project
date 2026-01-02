package com.team_biance.the_coin_killer.event;

import com.team_biance.the_coin_killer.dto.binance.DepthEvent;
import org.springframework.context.ApplicationEvent;

public class DepthStreamEvent extends ApplicationEvent {
    private final DepthEvent payload;

    public DepthStreamEvent(Object source, DepthEvent payload) {
        super(source);
        this.payload = payload;
    }

    public DepthEvent getPayload() {
        return payload;
    }
}
