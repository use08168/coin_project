<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Binance Monitor Dashboard</title>

    <!-- Bootstrap 5 CDN -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"/>

    <style>
        .status-pill {
            font-weight: 700;
            padding: .25rem .6rem;
            border-radius: 999px;
            font-size: .85rem;
            display: inline-block;
        }
        .pill-green { background: #d1e7dd; color: #0f5132; }
        .pill-yellow { background: #fff3cd; color: #664d03; }
        .pill-red { background: #f8d7da; color: #842029; }
        .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; }
        .small-muted { font-size: .85rem; color: #6c757d; }
    </style>

    <script>
        // 30초마다 전체 새로고침
        setInterval(function () {
            location.reload();
        }, 30000);
    </script>
</head>

<body class="bg-light">
<div class="container py-4">

    <div class="d-flex align-items-center justify-content-between mb-3">
        <div>
            <h3 class="mb-1">Binance 데이터 수집 모니터링</h3>
            <div class="small-muted">30초마다 자동 새로고침 · 최근 ${limit}건 미리보기</div>
        </div>
        <div class="text-end">
            <a class="btn btn-outline-secondary btn-sm" href="/monitor/dashboard?limit=5">최근 5건</a>
            <a class="btn btn-outline-secondary btn-sm" href="/monitor/dashboard?limit=10">최근 10건</a>
        </div>
    </div>

    <!-- 상태 카드 -->
    <div class="row g-3">
        <c:forEach var="st" items="${statuses}">
            <c:set var="borderClass" value="border-success"/>
            <c:set var="pillClass" value="pill-green"/>

            <c:choose>
                <c:when test="${st.status eq 'DELAYED'}">
                    <c:set var="borderClass" value="border-warning"/>
                    <c:set var="pillClass" value="pill-yellow"/>
                </c:when>
                <c:when test="${st.status eq 'STOPPED'}">
                    <c:set var="borderClass" value="border-danger"/>
                    <c:set var="pillClass" value="pill-red"/>
                </c:when>
            </c:choose>

            <div class="col-12 col-md-6 col-xl-4">
                <div class="card ${borderClass} border-2 shadow-sm h-100">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-start">
                            <div>
                                <div class="small-muted mono">${st.tableName}</div>
                                <h5 class="mb-1">${st.displayName}</h5>
                            </div>
                            <span class="status-pill ${pillClass}">
                                ${st.status}
                            </span>
                        </div>

                        <hr class="my-3"/>

                        <div class="row">
                            <div class="col-6">
                                <div class="small-muted">총 레코드</div>
                                <div class="fw-bold mono">${st.totalCount}</div>
                            </div>
                            <div class="col-6">
                                <div class="small-muted">최신 시각(UTC)</div>
                                <div class="fw-bold mono">
                                    <c:choose>
                                        <c:when test="${st.latestTime != null}">
                                            ${st.latestTime}
                                        </c:when>
                                        <c:otherwise>
                                            -
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>

                        <div class="mt-3">
                            <div class="small-muted">경과</div>
                            <div class="fw-bold">${st.timeAgo}</div>
                        </div>
                    </div>
                </div>
            </div>

        </c:forEach>
    </div>

    <!-- 최근 데이터 미리보기 (아코디언) -->
    <div class="mt-4">
        <div class="d-flex align-items-center justify-content-between mb-2">
            <h5 class="mb-0">최근 데이터 미리보기</h5>
            <span class="small-muted">각 섹션을 펼치면 최근 ${limit}건 표시</span>
        </div>

        <div class="accordion" id="recentAccordion">

            <!-- 1) Kline -->
            <div class="accordion-item">
                <h2 class="accordion-header" id="hKline">
                    <button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#cKline">
                        f_kline_1m (1분 캔들)
                    </button>
                </h2>
                <div id="cKline" class="accordion-collapse collapse show" data-bs-parent="#recentAccordion">
                    <div class="accordion-body">
                        <div class="table-responsive">
                            <table class="table table-sm table-striped align-middle">
                                <thead>
                                <tr>
                                    <th>symbol</th><th>ts_utc</th>
                                    <th>open</th><th>high</th><th>low</th><th>close</th>
                                    <th>vol</th><th>trades</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="r" items="${recentKlines}">
                                    <tr class="mono">
                                        <td>${r.symbol}</td>
                                        <td>${r.tsUtc}</td>
                                        <td>${r.open}</td>
                                        <td>${r.high}</td>
                                        <td>${r.low}</td>
                                        <td>${r.close}</td>
                                        <td>${r.volume}</td>
                                        <td>${r.tradeCount}</td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 2) Mark -->
            <div class="accordion-item">
                <h2 class="accordion-header" id="hMark">
                    <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#cMark">
                        f_mark_1s (마크가격)
                    </button>
                </h2>
                <div id="cMark" class="accordion-collapse collapse" data-bs-parent="#recentAccordion">
                    <div class="accordion-body">
                        <div class="table-responsive">
                            <table class="table table-sm table-striped align-middle">
                                <thead>
                                <tr>
                                    <th>symbol</th><th>ts_utc</th>
                                    <th>mark</th><th>index</th><th>funding</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="r" items="${recentMarks}">
                                    <tr class="mono">
                                        <td>${r.symbol}</td>
                                        <td>${r.tsUtc}</td>
                                        <td>${r.markPrice}</td>
                                        <td>${r.indexPrice}</td>
                                        <td>${r.fundingRate}</td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 3) Depth -->
            <div class="accordion-item">
                <h2 class="accordion-header" id="hDepth">
                    <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#cDepth">
                        f_depth_snapshot_1s (오더북 스냅샷)
                    </button>
                </h2>
                <div id="cDepth" class="accordion-collapse collapse" data-bs-parent="#recentAccordion">
                    <div class="accordion-body">
                        <div class="table-responsive">
                            <table class="table table-sm table-striped align-middle">
                                <thead>
                                <tr>
                                    <th>symbol</th><th>ts_utc</th>
                                    <th>mid</th><th>spread_bps</th>
                                    <th>imbalance</th><th>micro_gap_bps</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="r" items="${recentDepths}">
                                    <tr class="mono">
                                        <td>${r.symbol}</td>
                                        <td>${r.tsUtc}</td>
                                        <td>${r.midPrice}</td>
                                        <td>${r.spreadBps}</td>
                                        <td>${r.imbalanceTop20}</td>
                                        <td>${r.micropriceGapBps}</td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 4) ForceOrder -->
            <div class="accordion-item">
                <h2 class="accordion-header" id="hForce">
                    <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#cForce">
                        f_forceorder (강제청산)
                    </button>
                </h2>
                <div id="cForce" class="accordion-collapse collapse" data-bs-parent="#recentAccordion">
                    <div class="accordion-body">
                        <div class="table-responsive">
                            <table class="table table-sm table-striped align-middle">
                                <thead>
                                <tr>
                                    <th>id</th><th>symbol</th><th>event_utc</th>
                                    <th>side</th><th>price</th><th>qty</th><th>status</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="r" items="${recentForceOrders}">
                                    <tr class="mono">
                                        <td>${r.id}</td>
                                        <td>${r.symbol}</td>
                                        <td>${r.eventUtc}</td>
                                        <td>${r.side}</td>
                                        <td>${r.price}</td>
                                        <td>${r.qty}</td>
                                        <td>${r.status}</td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 5) AggTrade -->
            <div class="accordion-item">
                <h2 class="accordion-header" id="hAgg">
                    <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#cAgg">
                        f_aggtrade_1m (체결 집계)
                    </button>
                </h2>
                <div id="cAgg" class="accordion-collapse collapse" data-bs-parent="#recentAccordion">
                    <div class="accordion-body">
                        <div class="table-responsive">
                            <table class="table table-sm table-striped align-middle">
                                <thead>
                                <tr>
                                    <th>symbol</th><th>ts_utc</th>
                                    <th>buy_qty</th><th>sell_qty</th><th>count</th><th>vwap</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="r" items="${recentAggTrades}">
                                    <tr class="mono">
                                        <td>${r.symbol}</td>
                                        <td>${r.tsUtc}</td>
                                        <td>${r.takerBuyQty}</td>
                                        <td>${r.takerSellQty}</td>
                                        <td>${r.tradeCount}</td>
                                        <td>${r.vwapPrice}</td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 6) FeatureMinute -->
            <div class="accordion-item">
                <h2 class="accordion-header" id="hFeat">
                    <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#cFeat">
                        feature_minute (계산된 피처)
                    </button>
                </h2>
                <div id="cFeat" class="accordion-collapse collapse" data-bs-parent="#recentAccordion">
                    <div class="accordion-body">
                        <div class="table-responsive">
                            <table class="table table-sm table-striped align-middle">
                                <thead>
                                <tr>
                                    <th>symbol</th><th>ts_utc</th>
                                    <th>close</th><th>ret1m</th><th>rv15m</th><th>vol_z_60m</th><th>mark_spot_bps</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="r" items="${recentFeatureMinutes}">
                                    <tr class="mono">
                                        <td>${r.symbol}</td>
                                        <td>${r.tsUtc}</td>
                                        <td>${r.close1m}</td>
                                        <td>${r.ret1mLog}</td>
                                        <td>${r.rv15m}</td>
                                        <td>${r.volZ60m}</td>
                                        <td>${r.markSpotBps}</td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

        </div>
    </div>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
