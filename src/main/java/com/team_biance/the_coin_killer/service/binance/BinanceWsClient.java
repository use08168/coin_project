package com.team_biance.the_coin_killer.service.binance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team_biance.the_coin_killer.dao.ForceOrderDao;
import com.team_biance.the_coin_killer.dao.KlineDao;
import com.team_biance.the_coin_killer.dao.MarkDao;
import com.team_biance.the_coin_killer.util.TimeUtil;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class BinanceWsClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceWsClient.class);

    @Value("${binance.futures.ws-base:wss://fstream.binance.com}")
    private String wsBase;

    @Value("${binance.symbol:BTCUSDT}")
    private String symbol;

    private final ObjectMapper om;
    private final KlineDao klineDao;
    private final MarkDao markDao;
    private final ForceOrderDao forceOrderDao;
    private final DepthCache depthCache;
    private final AggTradeAggregator aggTradeAggregator;
    private final BinanceIngestState state;

    private OkHttpClient client;
    private WebSocket ws;

    public BinanceWsClient(ObjectMapper om,
            KlineDao klineDao,
            MarkDao markDao,
            ForceOrderDao forceOrderDao,
            DepthCache depthCache,
            AggTradeAggregator aggTradeAggregator,
            BinanceIngestState state) {
        this.om = om;
        this.klineDao = klineDao;
        this.markDao = markDao;
        this.forceOrderDao = forceOrderDao;
        this.depthCache = depthCache;
        this.aggTradeAggregator = aggTradeAggregator;
        this.state = state;
    }

    public synchronized void start() {
        if (client == null)
            client = new OkHttpClient.Builder().build();

        String s = symbol.toLowerCase();
        String url = wsBase + "/stream?streams="
                + s + "@kline_1m/"
                + s + "@markPrice@1s/"
                + s + "@forceOrder/"
                + s + "@depth20@250ms/"
                + s + "@aggTrade";

        log.info("[BINANCE] WS start url={}", url);

        Request req = new Request.Builder().url(url).build();
        ws = client.newWebSocket(req, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("[BINANCE] WS opened");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                state.wsMsgTotal.incrementAndGet();
                state.lastWsMsgAt.set(Instant.now());
                handleMessage(text);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.warn("[BINANCE] WS closed code={} reason={}", code, reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                String msg = "[BINANCE] WS failure: " + (t == null ? "null" : t.getMessage());
                log.error(msg, t);
                state.lastError.set(msg);
            }
        });
    }

    public synchronized void restart() {
        try {
            if (ws != null)
                ws.close(1000, "restart");
        } catch (Exception ignore) {
        }
        ws = null;
        start();
    }

    private void handleMessage(String text) {
        try {
            JsonNode root = om.readTree(text);
            JsonNode data = root.get("data");
            if (data == null)
                return;

            String e = data.get("e").asText();

            switch (e) {
                case "kline" -> {
                    state.wsKlineMsg.incrementAndGet();
                    handleKline(data);
                }
                case "markPriceUpdate" -> {
                    state.wsMarkMsg.incrementAndGet();
                    handleMark(data);
                }
                case "forceOrder" -> {
                    state.wsForceOrderMsg.incrementAndGet();
                    handleForceOrder(data);
                }
                case "depthUpdate" -> {
                    state.wsDepthMsg.incrementAndGet();
                    handleDepth(data);
                }
                case "aggTrade" -> {
                    state.wsAggTradeMsg.incrementAndGet();
                    handleAggTrade(data);
                }
            }
        } catch (Exception ex) {
            state.lastError.set("handleMessage error: " + ex.getMessage());
        }
    }

    private void handleKline(JsonNode data) {
        JsonNode k = data.get("k");
        if (k == null)
            return;

        boolean closed = k.get("x").asBoolean(false);
        if (!closed)
            return;

        Instant tsUtc = TimeUtil.fromEpochMs(k.get("t").asLong());

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
        Instant tsUtc = TimeUtil.fromEpochMs(data.get("E").asLong());

        double mark = data.get("p").asDouble();
        Double index = data.hasNonNull("i") ? data.get("i").asDouble() : null;
        Double funding = data.hasNonNull("r") ? data.get("r").asDouble() : null;
        Instant nextFunding = data.hasNonNull("T") ? TimeUtil.fromEpochMs(data.get("T").asLong()) : null;

        markDao.upsert1s(symbol, tsUtc, mark, index, funding, nextFunding);
    }

    private void handleForceOrder(JsonNode data) {
        Instant eventUtc = TimeUtil.fromEpochMs(data.get("E").asLong());

        JsonNode o = data.get("o");
        if (o == null)
            return;

        String side = o.get("S").asText();
        String status = o.hasNonNull("X") ? o.get("X").asText() : null;
        double price = o.get("p").asDouble();
        double qty = o.get("q").asDouble();

        forceOrderDao.insert(symbol, eventUtc, side, price, qty, status);
    }

    private void handleDepth(JsonNode data) {
        Instant eventUtc = TimeUtil.fromEpochMs(data.get("E").asLong());

        JsonNode bids = data.get("b");
        JsonNode asks = data.get("a");
        if (bids == null || asks == null)
            return;

        state.lastDepthEventAt.set(eventUtc);
        depthCache.set(new DepthCache.DepthData(eventUtc, bids, asks));
    }

    private void handleAggTrade(JsonNode data) {
        Instant eventUtc = TimeUtil.fromEpochMs(data.get("E").asLong());
        double price = data.get("p").asDouble();
        double qty = data.get("q").asDouble();
        boolean buyerIsMaker = data.get("m").asBoolean();

        aggTradeAggregator.onAggTrade(symbol, eventUtc, price, qty, buyerIsMaker);
    }
}
