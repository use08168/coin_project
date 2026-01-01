package com.team_biance.the_coin_killer.service.binance;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class BinanceIngestState {
    public final AtomicLong wsMsgTotal = new AtomicLong(0);
    public final AtomicLong wsDepthMsg = new AtomicLong(0);
    public final AtomicLong wsMarkMsg = new AtomicLong(0);
    public final AtomicLong wsKlineMsg = new AtomicLong(0);
    public final AtomicLong wsAggTradeMsg = new AtomicLong(0);
    public final AtomicLong wsForceOrderMsg = new AtomicLong(0);

    public final AtomicReference<Instant> lastWsMsgAt = new AtomicReference<>(null);
    public final AtomicReference<Instant> lastDepthEventAt = new AtomicReference<>(null);
    public final AtomicReference<Instant> lastDepthFlushAt = new AtomicReference<>(null);

    public final AtomicReference<String> lastError = new AtomicReference<>(null);
}
