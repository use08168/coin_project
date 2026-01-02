package com.team_biance.the_coin_killer.event;

import com.team_biance.the_coin_killer.dto.binance.ForceOrderEvent;
import org.springframework.context.ApplicationEvent;

public class ForceOrderStreamEvent extends ApplicationEvent {
    private final ForceOrderEvent payload;

    public ForceOrderStreamEvent(Object source, ForceOrderEvent payload) {
        super(source);
        this.payload = payload;
    }

    public ForceOrderEvent getPayload() {
        return payload;
    }
}
