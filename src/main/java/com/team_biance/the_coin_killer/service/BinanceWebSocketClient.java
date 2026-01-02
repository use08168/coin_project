package com.team_biance.the_coin_killer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team_biance.the_coin_killer.dto.binance.*;
import com.team_biance.the_coin_killer.event.*;
import okhttp3.*;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class BinanceWebSocketClient implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BinanceWebSocketClient.class);

    private static final String BASE_URL = "wss://fstream.binance.com";
    private static final String SYMBOL = "btcusdt"; // lower-case stream rule

    private static final String STREAM_KLINE_1M = SYMBOL + "@kline_1m";
    private static final String STREAM_DEPTH20 = SYMBOL + "@depth20@100ms";
    private static final String STREAM_AGG_TRADE = SYMBOL + "@aggTrade";
    private static final String STREAM_FORCE_ORDER = SYMBOL + "@forceOrder";
    private static final String STREAM_MARK_PRICE = SYMBOL + "@markPrice@1s";

    private static final String COMBINED_URL = BASE_URL + "/stream?streams=" +
            STREAM_KLINE_1M + "/" +
            STREAM_DEPTH20 + "/" +
            STREAM_AGG_TRADE + "/" +
            STREAM_FORCE_ORDER + "/" +
            STREAM_MARK_PRICE;

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher publisher;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "binance-ws-reconnect");
        t.setDaemon(true);
        return t;
    });

    private volatile WebSocket webSocket;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);

    public BinanceWebSocketClient(
            OkHttpClient okHttpClient,
            ObjectMapper objectMapper,
            ApplicationEventPublisher publisher) {
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
        this.publisher = publisher;
    }

    @Override
    public void run(ApplicationArguments args) {
        connect();
    }

    private synchronized void connect() {
        if (shuttingDown.get())
            return;

        // 이미 열려있으면 중복 연결 방지
        if (webSocket != null) {
            return;
        }

        log.info("[BINANCE-WS] connecting: {}", COMBINED_URL);

        Request request = new Request.Builder()
                .url(COMBINED_URL)
                .build();

        webSocket = okHttpClient.newWebSocket(request, new BinanceListener());
    }

    private void scheduleReconnect() {
        if (shuttingDown.get())
            return;

        // 중복 스케줄 방지
        if (!reconnectScheduled.compareAndSet(false, true)) {
            return;
        }

        log.warn("[BINANCE-WS] reconnect scheduled in 3s...");
        scheduler.schedule(() -> {
            try {
                synchronized (this) {
                    webSocket = null; // 재연결을 위해 null로
                }
                connect();
            } finally {
                reconnectScheduled.set(false);
            }
        }, 3, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        shuttingDown.set(true);
        log.info("[BINANCE-WS] shutting down...");

        WebSocket ws = this.webSocket;
        this.webSocket = null;

        if (ws != null) {
            try {
                ws.close(1000, "shutdown");
            } catch (Exception ignored) {
            }
        }

        scheduler.shutdownNow();
    }

    // =========================
    // OkHttp WebSocket Listener
    // =========================
    private class BinanceListener extends WebSocketListener {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            log.info("[BINANCE-WS] connected. code={}, message={}",
                    response.code(), response.message());
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            try {
                handleMessage(text);
            } catch (Exception e) {
                log.error("[BINANCE-WS] message handling error: {}", e.getMessage(), e);
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            // 바이너스는 보통 text지만 혹시 몰라 지원
            onMessage(webSocket, bytes.utf8());
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            log.warn("[BINANCE-WS] closing. code={}, reason={}", code, reason);
            webSocket.close(code, reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            log.warn("[BINANCE-WS] closed. code={}, reason={}", code, reason);
            synchronized (BinanceWebSocketClient.this) {
                BinanceWebSocketClient.this.webSocket = null;
            }
            scheduleReconnect();
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            String resp = (response == null) ? "null" : (response.code() + " " + response.message());
            log.error("[BINANCE-WS] failure. response={}, err={}", resp, t.getMessage(), t);

            synchronized (BinanceWebSocketClient.this) {
                BinanceWebSocketClient.this.webSocket = null;
            }
            scheduleReconnect();
        }
    }

    // =========================
    // Message Router
    // =========================
    private void handleMessage(String raw) throws Exception {
        JsonNode root = objectMapper.readTree(raw);

        // Combined Stream: { "stream": "...", "data": {...} }
        JsonNode streamNode = root.get("stream");
        JsonNode dataNode = root.get("data");

        if (streamNode == null || dataNode == null) {
            // 가끔 ping/기타 메시지 형태가 다를 수 있어서 로그만
            log.debug("[BINANCE-WS] unknown payload: {}", raw);
            return;
        }

        String stream = streamNode.asText();

        switch (stream) {
            case STREAM_KLINE_1M -> handleKline(dataNode);
            case STREAM_DEPTH20 -> handleDepth(dataNode);
            case STREAM_AGG_TRADE -> handleAggTrade(dataNode);
            case STREAM_FORCE_ORDER -> handleForceOrder(dataNode);
            case STREAM_MARK_PRICE -> handleMarkPrice(dataNode);
            default -> log.debug("[BINANCE-WS] unhandled stream={}", stream);
        }
    }

    // =========================
    // Stream Handlers
    // =========================
    private void handleKline(JsonNode dataNode) throws Exception {
        KlineEvent dto = objectMapper.treeToValue(dataNode, KlineEvent.class);
        publisher.publishEvent(new KlineStreamEvent(this, dto));
    }

    private void handleAggTrade(JsonNode dataNode) throws Exception {
        AggTradeEvent dto = objectMapper.treeToValue(dataNode, AggTradeEvent.class);
        publisher.publishEvent(new AggTradeStreamEvent(this, dto));
    }

    private void handleForceOrder(JsonNode dataNode) throws Exception {
        ForceOrderEvent dto = objectMapper.treeToValue(dataNode, ForceOrderEvent.class);
        publisher.publishEvent(new ForceOrderStreamEvent(this, dto));
    }

    private void handleMarkPrice(JsonNode dataNode) throws Exception {
        MarkPriceEvent dto = objectMapper.treeToValue(dataNode, MarkPriceEvent.class);
        publisher.publishEvent(new MarkPriceStreamEvent(this, dto));
    }

    /**
     * depth20@100ms 는 스펙 변형 가능성이 있어 안전하게 JsonNode로 파싱
     * - bids: "b" : [[price, qty], ...]
     * - asks: "a" : [[price, qty], ...]
     * - event time: "E" (optional)
     * - transaction time: "T" (optional)
     * - lastUpdateId: "lastUpdateId" (optional)
     * - symbol: "s" (optional)
     */
    private void handleDepth(JsonNode dataNode) {
        String symbol = textOrNull(dataNode.get("s"));
        if (symbol == null)
            symbol = "BTCUSDT";

        Long eventTime = longOrNull(dataNode.get("E"));
        Long transactionTime = longOrNull(dataNode.get("T"));
        Long lastUpdateId = longOrNull(dataNode.get("lastUpdateId"));

        List<DepthEvent.DepthLevel> bids = parseLevels(dataNode.get("b"));
        List<DepthEvent.DepthLevel> asks = parseLevels(dataNode.get("a"));

        DepthEvent dto = new DepthEvent(symbol, eventTime, transactionTime, lastUpdateId, bids, asks);
        publisher.publishEvent(new DepthStreamEvent(this, dto));
    }

    private List<DepthEvent.DepthLevel> parseLevels(JsonNode levelsNode) {
        List<DepthEvent.DepthLevel> out = new ArrayList<>();
        if (levelsNode == null || !levelsNode.isArray())
            return out;

        for (JsonNode lvl : levelsNode) {
            if (!lvl.isArray() || lvl.size() < 2)
                continue;
            String price = lvl.get(0).asText();
            String qty = lvl.get(1).asText();
            out.add(new DepthEvent.DepthLevel(price, qty));
        }
        return out;
    }

    private static String textOrNull(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.asText(null);
    }

    private static Long longOrNull(JsonNode node) {
        if (node == null || node.isNull())
            return null;
        if (node.isNumber())
            return node.asLong();
        String t = node.asText(null);
        if (t == null)
            return null;
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
