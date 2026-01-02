package com.team_biance.the_coin_killer.dto.binance;

import java.util.List;

public record DepthEvent(
        String symbol,
        Long eventTime,
        Long transactionTime,
        Long lastUpdateId,
        List<DepthLevel> bids,
        List<DepthLevel> asks) {
    public record DepthLevel(String price, String qty) {
    }
}
