<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>TradingView vs Our Binance Data (Realtime)</title>

    <!-- Bootstrap 5 (다크 테마 사용) -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"/>

    <style>
        body { background: #0b0f14; }
        .panel-card { background: #111827; border: 1px solid rgba(255,255,255,.08); }
        .muted { color: rgba(255,255,255,.6); }
        .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; }
        .tv-wrap { background: #0b0f14; border: 1px solid rgba(255,255,255,.08); border-radius: 12px; overflow: hidden; }

        /* 업데이트 시 깜빡임(fade) */
        .flash {
            animation: flashFade .45s ease-in-out;
        }
        @keyframes flashFade {
            0% { opacity: 0.35; }
            100% { opacity: 1; }
        }

        .kv { display: flex; justify-content: space-between; gap: 12px; padding: 6px 0; border-bottom: 1px solid rgba(255,255,255,.06); }
        .kv:last-child { border-bottom: none; }
        .kv .k { color: rgba(255,255,255,.65); }
        .kv .v { font-weight: 700; }
        .badge-soft { background: rgba(59,130,246,.15); color: #93c5fd; border: 1px solid rgba(59,130,246,.25); }
    </style>
</head>

<body data-bs-theme="dark">
<div class="container-fluid py-3">
    <div class="d-flex align-items-center justify-content-between mb-3">
        <div>
            <h4 class="mb-1">TradingView vs Our Binance Data</h4>
            <div class="muted small">왼쪽: TradingView (BINANCE:BTCUSDT.P) · 오른쪽: DB 최신값(5초마다 갱신)</div>
        </div>
        <span class="badge badge-soft mono">/api/monitor/realtime</span>
    </div>

    <div class="row g-3">
        <!-- LEFT 60% -->
        <div class="col-12 col-lg-7">
            <div class="tv-wrap p-2">
                <!-- TradingView Widget BEGIN -->
                <div class="tradingview-widget-container">
                    <div id="tradingview_chart"></div>
                    <script type="text/javascript" src="https://s3.tradingview.com/tv.js"></script>
                    <script type="text/javascript">
                        new TradingView.widget({
                            "width": "100%",
                            "height": 600,
                            "symbol": "BINANCE:BTCUSDT.P",
                            "interval": "1",
                            "timezone": "Etc/UTC",
                            "theme": "dark",
                            "style": "1",
                            "locale": "kr",
                            "toolbar_bg": "#0b0f14",
                            "enable_publishing": false,
                            "allow_symbol_change": true,
                            "container_id": "tradingview_chart"
                        });
                    </script>
                </div>
                <!-- TradingView Widget END -->
            </div>
        </div>

        <!-- RIGHT 40% -->
        <div class="col-12 col-lg-5">
            <div id="rightPanel">

                <!-- updated at -->
                <div class="panel-card rounded-3 p-3 mb-3">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <div class="muted small">API timestamp (UTC)</div>
                            <div class="mono fw-bold" id="apiTimestamp">-</div>
                        </div>
                        <div class="text-end">
                            <div class="muted small">갱신 주기</div>
                            <div class="mono fw-bold">5s</div>
                        </div>
                    </div>
                </div>

                <!-- Kline -->
                <div class="panel-card rounded-3 p-3 mb-3">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <div class="fw-bold">최신 캔들 (OHLCV)</div>
                        <span class="muted small mono" id="klineTs">-</span>
                    </div>
                    <div class="kv"><div class="k">Open</div><div class="v mono" id="kOpen">-</div></div>
                    <div class="kv"><div class="k">High</div><div class="v mono" id="kHigh">-</div></div>
                    <div class="kv"><div class="k">Low</div><div class="v mono" id="kLow">-</div></div>
                    <div class="kv"><div class="k">Close</div><div class="v mono" id="kClose">-</div></div>
                    <div class="kv"><div class="k">Volume</div><div class="v mono" id="kVol">-</div></div>
                    <div class="kv"><div class="k">Trade Count</div><div class="v mono" id="kTrades">-</div></div>
                </div>

                <!-- Mark -->
                <div class="panel-card rounded-3 p-3 mb-3">
                    <div class="fw-bold mb-2">마크가격 / 펀딩</div>
                    <div class="kv"><div class="k">Mark Price</div><div class="v mono" id="mMark">-</div></div>
                    <div class="kv"><div class="k">Index Price</div><div class="v mono" id="mIndex">-</div></div>
                    <div class="kv"><div class="k">Funding Rate</div><div class="v mono" id="mFunding">-</div></div>
                </div>

                <!-- Depth -->
                <div class="panel-card rounded-3 p-3 mb-3">
                    <div class="fw-bold mb-2">오더북 요약</div>
                    <div class="kv"><div class="k">Mid Price</div><div class="v mono" id="dMid">-</div></div>
                    <div class="kv"><div class="k">Spread (bps)</div><div class="v mono" id="dSpread">-</div></div>
                    <div class="kv"><div class="k">Imbalance</div><div class="v mono" id="dImb">-</div></div>
                </div>

                <!-- Feature -->
                <div class="panel-card rounded-3 p-3">
                    <div class="fw-bold mb-2">Feature 일부</div>
                    <div class="kv"><div class="k">ret1m_log</div><div class="v mono" id="fRet1m">-</div></div>
                    <div class="kv"><div class="k">rv15m</div><div class="v mono" id="fRv15m">-</div></div>
                    <div class="kv"><div class="k">cvd_15m</div><div class="v mono" id="fCvd15m">-</div></div>
                    <div class="kv"><div class="k">buy_ratio_1m</div><div class="v mono" id="fBuyRatio">-</div></div>
                </div>

            </div>
        </div>
    </div>
</div>

<script>
    const API_URL = "/api/monitor/realtime";

    function fmtPrice(v) {
        if (v === null || v === undefined) return "-";
        const n = Number(v);
        if (!isFinite(n)) return "-";
        return n.toLocaleString("en-US", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
    }

    function fmtVol(v) {
        if (v === null || v === undefined) return "-";
        const n = Number(v);
        if (!isFinite(n)) return "-";
        return n.toLocaleString("en-US", { minimumFractionDigits: 3, maximumFractionDigits: 3 });
    }

    function fmtInt(v) {
        if (v === null || v === undefined) return "-";
        const n = Number(v);
        if (!isFinite(n)) return "-";
        return Math.trunc(n).toLocaleString("en-US");
    }

    function fmtRate(v) {
        if (v === null || v === undefined) return "-";
        const n = Number(v);
        if (!isFinite(n)) return "-";
        return n.toFixed(4);
    }

    function fmtSmall(v) {
        if (v === null || v === undefined) return "-";
        const n = Number(v);
        if (!isFinite(n)) return "-";
        return n.toFixed(6);
    }

    function flashPanel() {
        const el = document.getElementById("rightPanel");
        el.classList.remove("flash");
        // reflow
        void el.offsetWidth;
        el.classList.add("flash");
    }

    function setText(id, text) {
        const el = document.getElementById(id);
        if (el) el.textContent = text;
    }

    async function refresh() {
        try {
            const res = await fetch(API_URL, { cache: "no-store" });
            if (!res.ok) throw new Error("HTTP " + res.status);
            const data = await res.json();

            setText("apiTimestamp", data.timestamp ?? "-");

            // kline
            if (data.kline) {
                setText("klineTs", data.kline.tsUtc ?? "-");
                setText("kOpen", fmtPrice(data.kline.open));
                setText("kHigh", fmtPrice(data.kline.high));
                setText("kLow", fmtPrice(data.kline.low));
                setText("kClose", fmtPrice(data.kline.close));
                setText("kVol", fmtVol(data.kline.volume));
                setText("kTrades", fmtInt(data.kline.tradeCount));
            } else {
                setText("klineTs", "-");
                ["kOpen","kHigh","kLow","kClose","kVol","kTrades"].forEach(x => setText(x, "-"));
            }

            // mark
            if (data.mark) {
                setText("mMark", fmtPrice(data.mark.markPrice));
                setText("mIndex", fmtPrice(data.mark.indexPrice));
                setText("mFunding", fmtRate(data.mark.fundingRate));
            } else {
                ["mMark","mIndex","mFunding"].forEach(x => setText(x, "-"));
            }

            // depth
            if (data.depth) {
                setText("dMid", fmtPrice(data.depth.midPrice));
                setText("dSpread", fmtRate(data.depth.spreadBps));
                setText("dImb", fmtRate(data.depth.imbalance));
            } else {
                ["dMid","dSpread","dImb"].forEach(x => setText(x, "-"));
            }

            // feature
            if (data.feature) {
                setText("fRet1m", fmtSmall(data.feature.ret1mLog));
                setText("fRv15m", fmtRate(data.feature.rv15m));
                setText("fCvd15m", fmtRate(data.feature.cvd15m));
                setText("fBuyRatio", fmtRate(data.feature.buyRatio1m));
            } else {
                ["fRet1m","fRv15m","fCvd15m","fBuyRatio"].forEach(x => setText(x, "-"));
            }

            flashPanel();
        } catch (e) {
            // API 실패해도 화면은 유지 (깜빡임 없이)
            console.error("[realtime refresh] failed:", e);
        }
    }

    // 즉시 1회 + 5초마다
    refresh();
    setInterval(refresh, 5000);
</script>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
