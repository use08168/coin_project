package com.team_biance.the_coin_killer.service.binance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team_biance.the_coin_killer.dao.ForceOrderDao;
import com.team_biance.the_coin_killer.dao.KlineDao;
import com.team_biance.the_coin_killer.dao.MarkDao;
import com.team_biance.the_coin_killer.util.TimeUtil;
import okhttp3.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class BinanceWsClient {

    @Value("${binance.futures.ws-base}")
    private String wsBase;

    @Value("${binance.symbol}")
    private String symbol;

    private final ObjectMapper om;
    private final KlineDao klineDao;
    private final MarkDao markDao;
    private final ForceOrderDao forceOrderDao;
    private final DepthCache depthCache;
    private final AggTradeAggregator aggTradeAggregator;

    private OkHttpClient client;
    private WebSocket ws;

    public BinanceWsClient(ObjectMapper om,
            KlineDao klineDao,
            MarkDao markDao,
            ForceOrderDao forceOrderDao,
            DepthCache depthCache,
            AggTradeAggregator aggTradeAggregator) {
        this.om = om;
        this.klineDao = klineDao;
        this.markDao = markDao;
        this.forceOrderDao = forceOrderDao;
        this.depthCache = depthCache;
        this.aggTradeAggregator = aggTradeAggregator;
    }

    public void start() {
        if (client == null) {
            client = new OkHttpClient.Builder().build();
        }

        String s = symbol.toLowerCase();

        // Combined Streams
        String url = wsBase + "/stream?streams="
                + s + "@kline_1m/"
                + s + "@markPrice@1s/"
                + s + "@forceOrder/"
                + s + "@depth20@250ms/"
                + s + "@aggTrade";

        Request req = new Request.Builder().url(url).build();

        ws = client.newWebSocket(req, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleMessage(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                // 재연결은 bootstrap에서 주기적으로 상태 확인하며 다시 start하도록(다음 단계에서 개선)
            }
        });
    }

    private void handleMessage(String text) {
        try {
            JsonNode root = om.readTree(text);
            JsonNode data = root.get("data");
            if (data == null)
                return;

            String e = data.get("e").asText();

            switch (e) {
                case "kline" -> handleKline(data);
                case "markPriceUpdate" -> handleMark(data);
                case "forceOrder" -> handleForceOrder(data);
                case "depthUpdate" -> handleDepth(data);
                case "aggTrade" -> handleAggTrade(data);
            }
        } catch (Exception ignore) {
        }
    }

    private void handleKline(JsonNode data) {
        JsonNode k = data.get("k");
        if (k == null)
            return;

        boolean closed = k.get("x").asBoolean(false);
        if (!closed)
            return; // 1분 봉 마감 시점만 저장

        long t = k.get("t").asLong(); // open time
        Instant tsUtc = TimeUtil.fromEpochMs(t);

        double open = k.get("o").asDouble();
        double high = k.get("h").asDouble();
        double low = k.get("l").asDouble();
        double close = k.get("c").asDouble();
        double vol = k.get("v").asDouble();
        int trades = k.get("n").asInt();

        Double quoteVol = k.hasNonNull("q") ? k.get("q").asDouble() : null;
        Double takerBuyVol = k.hasNonNull("V") ? k.get("V").asDouble() : null;
        Double takerBuyQv = k.hasNonNull("Q") ? k.get("Q").asDouble() : null;

        klineDao.upsert1m(symbol, tsUtc, open, high, low, close, vol, trades, quoteVol, takerBuyVol, takerBuyQv);
    }

    private void handleMark(JsonNode data) {
        long E = data.get("E").asLong();
        Instant tsUtc = TimeUtil.fromEpochMs(E);

        double mark = data.get("p").asDouble();
        Double index = data.hasNonNull("i") ? data.get("i").asDouble() : null;
        Double funding = data.hasNonNull("r") ? data.get("r").asDouble() : null;
        Instant nextFunding = data.hasNonNull("T") ? TimeUtil.fromEpochMs(data.get("T").asLong()) : null;

        markDao.upsert1s(symbol, tsUtc, mark, index, funding, nextFunding);
    }

    private void handleForceOrder(JsonNode data) {
        long E = data.get("E").asLong();
        Instant eventUtc = TimeUtil.fromEpochMs(E);

        JsonNode o = data.get("o");
        if (o == null)
            return;

        String side = o.get("S").asText(); // BUY/SELL
        String status = o.hasNonNull("X") ? o.get("X").asText() : null;
        double price = o.get("p").asDouble();
        double qty = o.get("q").asDouble();

        forceOrderDao.insert(symbol, eventUtc, side, price, qty, status);
    }

    private void handleDepth(JsonNode data) {
        long E = data.get("E").asLong();
        Instant eventUtc = TimeUtil.fromEpochMs(E);

        JsonNode bids = data.get("b");
        JsonNode asks = data.get("a");
        if (bids == null || asks == null)
            return;

        depthCache.set(new DepthCache.DepthData(eventUtc, bids, asks));
    }

    private void handleAggTrade(JsonNode data) {
        long E = data.get("E").asLong();
        Instant eventUtc = TimeUtil.fromEpochMs(E);

        double price = data.get("p").asDouble();
        double qty = data.get("q").asDouble();
        boolean buyerIsMaker = data.get("m").asBoolean();

        aggTradeAggregator.onAggTrade(symbol, eventUtc, price, qty, buyerIsMaker);
    }
}
