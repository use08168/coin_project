package com.team_biance.the_coin_killer.dto.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarkPriceEvent(
        @JsonProperty("e") String eventType,
        @JsonProperty("E") long eventTime,
        @JsonProperty("s") String symbol,

        @JsonProperty("p") String markPrice,
        @JsonProperty("i") String indexPrice,
        @JsonProperty("P") String estimatedSettlePrice,

        @JsonProperty("r") String fundingRate,
        @JsonProperty("T") long nextFundingTime) {
}
