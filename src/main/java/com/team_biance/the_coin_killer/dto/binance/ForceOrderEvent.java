package com.team_biance.the_coin_killer.dto.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ForceOrderEvent(
        @JsonProperty("e") String eventType,
        @JsonProperty("E") long eventTime,
        @JsonProperty("o") ForceOrder order) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ForceOrder(
            @JsonProperty("s") String symbol,
            @JsonProperty("S") String side, // BUY / SELL
            @JsonProperty("o") String orderType,
            @JsonProperty("f") String timeInForce,

            @JsonProperty("q") String originalQty,
            @JsonProperty("p") String price,
            @JsonProperty("ap") String avgPrice,

            @JsonProperty("X") String orderStatus,
            @JsonProperty("l") String lastFilledQty,
            @JsonProperty("z") String filledAccumQty,

            @JsonProperty("T") long tradeTime) {
    }
}
