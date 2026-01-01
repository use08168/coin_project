package com.team_biance.the_coin_killer.service.binance;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class DepthCache {

    public static class DepthData {
        public final Instant eventTime;
        public final JsonNode bids; // [[price, qty], ...]
        public final JsonNode asks;

        public DepthData(Instant eventTime, JsonNode bids, JsonNode asks) {
            this.eventTime = eventTime;
            this.bids = bids;
            this.asks = asks;
        }
    }

    private final AtomicReference<DepthData> ref = new AtomicReference<>();

    public void set(DepthData d) {
        ref.set(d);
    }

    public DepthData get() {
        return ref.get();
    }
}
