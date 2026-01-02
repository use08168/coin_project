package com.team_biance.the_coin_killer.dto.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KlineEvent(
        @JsonProperty("e") String eventType,
        @JsonProperty("E") long eventTime,
        @JsonProperty("s") String symbol,
        @JsonProperty("k") Kline kline) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Kline(
            @JsonProperty("t") long startTime,
            @JsonProperty("T") long closeTime,
            @JsonProperty("s") String symbol,
            @JsonProperty("i") String interval,

            @JsonProperty("o") String open,
            @JsonProperty("c") String close,
            @JsonProperty("h") String high,
            @JsonProperty("l") String low,

            @JsonProperty("v") String volume,
            @JsonProperty("n") long numberOfTrades,
            @JsonProperty("x") boolean isFinal,

            @JsonProperty("q") String quoteAssetVolume,
            @JsonProperty("V") String takerBuyBaseAssetVolume,
            @JsonProperty("Q") String takerBuyQuoteAssetVolume) {
    }
}
