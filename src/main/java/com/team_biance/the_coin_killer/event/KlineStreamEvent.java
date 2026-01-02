package com.team_biance.the_coin_killer.event;

import com.team_biance.the_coin_killer.dto.binance.KlineEvent;
import org.springframework.context.ApplicationEvent;

public class KlineStreamEvent extends ApplicationEvent {
    private final KlineEvent payload;

    public KlineStreamEvent(Object source, KlineEvent payload) {
        super(source);
        this.payload = payload;
    }

    public KlineEvent getPayload() {
        return payload;
    }
}
