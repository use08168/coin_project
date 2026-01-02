package com.team_biance.the_coin_killer.event;

import com.team_biance.the_coin_killer.dto.binance.MarkPriceEvent;
import org.springframework.context.ApplicationEvent;

public class MarkPriceStreamEvent extends ApplicationEvent {
    private final MarkPriceEvent payload;

    public MarkPriceStreamEvent(Object source, MarkPriceEvent payload) {
        super(source);
        this.payload = payload;
    }

    public MarkPriceEvent getPayload() {
        return payload;
    }
}
