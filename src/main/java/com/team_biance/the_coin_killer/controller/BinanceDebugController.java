package com.team_biance.the_coin_killer.controller;

import com.team_biance.the_coin_killer.service.binance.BinanceIngestState;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class BinanceDebugController {

    private final BinanceIngestState state;

    public BinanceDebugController(BinanceIngestState state) {
        this.state = state;
    }

    @GetMapping("/debug/binance")
    public Map<String, Object> debug() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("wsMsgTotal", state.wsMsgTotal.get());
        m.put("wsDepthMsg", state.wsDepthMsg.get());
        m.put("wsMarkMsg", state.wsMarkMsg.get());
        m.put("wsKlineMsg", state.wsKlineMsg.get());
        m.put("wsAggTradeMsg", state.wsAggTradeMsg.get());
        m.put("wsForceOrderMsg", state.wsForceOrderMsg.get());

        m.put("lastWsMsgAt", state.lastWsMsgAt.get());
        m.put("lastDepthEventAt", state.lastDepthEventAt.get());
        m.put("lastDepthFlushAt", state.lastDepthFlushAt.get());
        m.put("lastError", state.lastError.get());
        return m;
    }
}
