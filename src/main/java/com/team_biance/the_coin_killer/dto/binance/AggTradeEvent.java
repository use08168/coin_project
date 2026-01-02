package com.team_biance.the_coin_killer.dto.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AggTradeEvent(
        @JsonProperty("e") String eventType,
        @JsonProperty("E") long eventTime,
        @JsonProperty("s") String symbol,

        @JsonProperty("a") long aggTradeId,
        @JsonProperty("p") String price,
        @JsonProperty("q") String quantity,

        @JsonProperty("f") long firstTradeId,
        @JsonProperty("l") long lastTradeId,
        @JsonProperty("T") long tradeTime,

        @JsonProperty("m") boolean buyerIsMaker,
        @JsonProperty("M") boolean ignore) {
    /**
     * Binance 정의:
     * - buyerIsMaker == true => buyer가 maker => seller가 taker(시장가) => TAKer side =
     * SELL
     * - buyerIsMaker == false => buyer가 taker => TAKer side = BUY
     */
    public String takerSide() {
        return buyerIsMaker ? "SELL" : "BUY";
    }
}
