<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>LightGBM 모델 관리</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"/>

    <style>
        body { background:#0b0f14; }
        .cardx { background:#111827; border:1px solid rgba(255,255,255,.08); }
        .muted { color: rgba(255,255,255,.6); }
        .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; }
        .flash { animation: flashFade .45s ease-in-out; }
        @keyframes flashFade { 0%{opacity:.35} 100%{opacity:1} }
        .ok { border-left: 4px solid #22c55e; }
        .bad { border-left: 4px solid #ef4444; }
        .warn { border-left: 4px solid #f59e0b; }
    </style>
</head>

<body data-bs-theme="dark">
<div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
            <h3 class="mb-1">🤖 LightGBM 모델 관리</h3>
            <div class="muted">Python(train/predict) 실행 + 결과 확인 + 예측 DB 저장</div>
        </div>
        <span class="badge text-bg-secondary mono">/model/control</span>
    </div>

    <!-- STATUS -->
    <div class="cardx rounded-3 p-3 mb-3" id="statusCard">
        <div class="d-flex justify-content-between align-items-start">
            <div>
                <div class="fw-bold mb-2">모델 상태</div>
                <div class="mono small">modelPath: <span id="stModelPath">-</span></div>
                <div class="mono small">exists: <span id="stExists">-</span></div>
                <div class="mono small">lastModifiedUtc: <span id="stMtime">-</span></div>
                <div class="mono small">version: <span id="stVersion">-</span></div>
                <div class="mono small">trainedAtUtc: <span id="stTrainedAt">-</span></div>
            </div>
            <button class="btn btn-outline-light btn-sm" onclick="loadStatus()">새로고침</button>
        </div>
    </div>

    <div class="row g-3">
        <!-- TRAIN -->
        <div class="col-12 col-lg-6">
            <div class="cardx rounded-3 p-3">
                <div class="fw-bold mb-2">학습</div>

                <div class="row g-2 align-items-end">
                    <div class="col-6">
                        <label class="form-label muted">Symbol</label>
                        <select id="trainSymbol" class="form-select">
                            <option value="BTCUSDT" selected>BTCUSDT</option>
                        </select>
                    </div>
                    <div class="col-3">
                        <label class="form-label muted">Days</label>
                        <input id="trainDays" type="number" class="form-control" value="7" min="1" max="60"/>
                    </div>
                    <div class="col-3 d-grid">
                        <button id="btnTrain" class="btn btn-primary" onclick="runTrain()">
                            <span id="trainBtnText">🚀 학습 시작</span>
                            <span id="trainSpinner" class="spinner-border spinner-border-sm ms-2 d-none"></span>
                        </button>
                    </div>
                </div>

                <div class="mt-3" id="trainResultBox"></div>
            </div>
        </div>

        <!-- PREDICT -->
        <div class="col-12 col-lg-6">
            <div class="cardx rounded-3 p-3">
                <div class="fw-bold mb-2">예측</div>

                <div class="row g-2 align-items-end">
                    <div class="col-9">
                        <label class="form-label muted">Symbol</label>
                        <select id="predSymbol" class="form-select">
                            <option value="BTCUSDT" selected>BTCUSDT</option>
                        </select>
                    </div>
                    <div class="col-3 d-grid">
                        <button id="btnPredict" class="btn btn-success" onclick="runPredict()">
                            <span id="predBtnText">🔮 예측 실행</span>
                            <span id="predSpinner" class="spinner-border spinner-border-sm ms-2 d-none"></span>
                        </button>
                    </div>
                </div>

                <div class="mt-3" id="predResultBox"></div>
            </div>
        </div>
    </div>

    <!-- RECENT PREDICTIONS -->
    <div class="cardx rounded-3 p-3 mt-3">
        <div class="d-flex justify-content-between align-items-center mb-2">
            <div class="fw-bold">최근 예측 기록</div>
            <div class="muted small">30초마다 자동 갱신</div>
        </div>

        <div class="table-responsive">
            <table class="table table-dark table-hover align-middle mb-0">
                <thead>
                <tr class="muted">
                    <th class="mono">ts_utc</th>
                    <th class="mono text-end">current_close</th>
                    <th class="mono text-center">pred</th>
                    <th class="mono text-end">prob</th>
                    <th class="mono">model_version</th>
                </tr>
                </thead>
                <tbody id="predTableBody">
                <tr><td colspan="5" class="muted">-</td></tr>
                </tbody>
            </table>
        </div>
    </div>
</div>

<script>
    function setLoading(btnId, spinnerId, on) {
        const btn = document.getElementById(btnId);
        const sp = document.getElementById(spinnerId);
        btn.disabled = on;
        sp.classList.toggle("d-none", !on);
    }

    function fmtPrice(x) {
        if (x === null || x === undefined) return "-";
        const n = Number(x);
        if (!isFinite(n)) return "-";
        return n.toLocaleString("en-US", {minimumFractionDigits: 1, maximumFractionDigits: 1});
    }
    function fmtProb(x) {
        if (x === null || x === undefined) return "-";
        const n = Number(x);
        if (!isFinite(n)) return "-";
        return (n * 100).toFixed(1) + "%";
    }

    function flash(el) {
        el.classList.remove("flash");
        void el.offsetWidth;
        el.classList.add("flash");
    }

    async function loadStatus() {
        const symbol = document.getElementById("trainSymbol").value;
        const res = await fetch("/api/model/status?symbol=" + encodeURIComponent(symbol), {cache:"no-store"});
        const data = await res.json();

        document.getElementById("stModelPath").textContent = data.modelPath ?? "-";
        document.getElementById("stExists").textContent = data.modelExists ? "YES" : "NO";
        document.getElementById("stMtime").textContent = data.lastModifiedUtc ?? "-";
        document.getElementById("stVersion").textContent = data.modelVersion ?? "-";
        document.getElementById("stTrainedAt").textContent = data.trainedAtUtc ?? "-";

        const card = document.getElementById("statusCard");
        card.classList.remove("ok","bad","warn");
        card.classList.add(data.modelExists ? "ok" : "bad");
        flash(card);
    }

    async function runTrain() {
        const symbol = document.getElementById("trainSymbol").value;
        const days = document.getElementById("trainDays").value;

        setLoading("btnTrain","trainSpinner", true);
        const box = document.getElementById("trainResultBox");
        box.innerHTML = "";

        try {
            const body = new URLSearchParams({symbol, days});
            const res = await fetch("/api/model/train", {
                method: "POST",
                headers: {"Content-Type":"application/x-www-form-urlencoded"},
                body
            });
            const data = await res.json();

            if (data.ok) {
                const acc = (data.metrics?.accuracy ?? 0).toFixed(4);
                const prec = (data.metrics?.precision ?? 0).toFixed(4);
                const rec = (data.metrics?.recall ?? 0).toFixed(4);
                const f1 = (data.metrics?.f1 ?? 0).toFixed(4);
                const mda = (data.metrics?.mda ?? 0).toFixed(4);
                const dur = (data.durationMs/1000).toFixed(2);
                
                box.innerHTML = 
                  '<div class="cardx rounded-3 p-3 ok">' +
                    '<div class="fw-bold mb-2">학습 성공 ✅</div>' +
                    '<div class="mono small">modelVersion: ' + data.modelVersion + '</div>' +
                    '<div class="mono small">modelPath: ' + data.modelPath + '</div>' +
                    '<hr/>' +
                    '<div class="row g-2">' +
                      '<div class="col-6 mono">Accuracy</div><div class="col-6 text-end mono">' + acc + '</div>' +
                      '<div class="col-6 mono">Precision</div><div class="col-6 text-end mono">' + prec + '</div>' +
                      '<div class="col-6 mono">Recall</div><div class="col-6 text-end mono">' + rec + '</div>' +
                      '<div class="col-6 mono">F1</div><div class="col-6 text-end mono">' + f1 + '</div>' +
                      '<div class="col-6 mono">MDA</div><div class="col-6 text-end mono">' + mda + '</div>' +
                      '<div class="col-6 mono">Duration</div><div class="col-6 text-end mono">' + dur + 's</div>' +
                    '</div>' +
                  '</div>';
            } else {
                box.innerHTML = 
                  '<div class="cardx rounded-3 p-3 bad">' +
                    '<div class="fw-bold mb-2">학습 실패 ❌</div>' +
                    '<div class="mono small">code: ' + (data.errorCode ?? "-") + '</div>' +
                    '<div class="mono small">message: ' + (data.errorMessage ?? "-") + '</div>' +
                  '</div>';
            }

            flash(box);
            await loadStatus();

        } catch (e) {
            box.innerHTML = 
              '<div class="cardx rounded-3 p-3 bad">' +
                '<div class="fw-bold mb-2">학습 호출 오류 ❌</div>' +
                '<div class="mono small">' + e + '</div>' +
              '</div>';
            flash(box);
        } finally {
            setLoading("btnTrain","trainSpinner", false);
        }
    }

    async function runPredict() {
        const symbol = document.getElementById("predSymbol").value;

        setLoading("btnPredict","predSpinner", true);
        const box = document.getElementById("predResultBox");
        box.innerHTML = "";

        try {
            const body = new URLSearchParams({symbol});
            const res = await fetch("/api/model/predict", {
                method: "POST",
                headers: {"Content-Type":"application/x-www-form-urlencoded"},
                body
            });
            const data = await res.json();

            if (data.ok) {
                const up = data.prediction === 1;
                const emoji = up ? "📈" : "📉";
                const cls = up ? "ok" : "warn";
                const dbWarn = (data.errorCode === "DB_INSERT_FAILED") 
                    ? '<div class="mt-2 text-warning mono small">⚠ DB 저장 실패: ' + data.errorMessage + '</div>' 
                    : '';
                
                box.innerHTML = 
                  '<div class="cardx rounded-3 p-3 ' + cls + '">' +
                    '<div class="fw-bold mb-2">예측 결과 ' + emoji + '</div>' +
                    '<div class="mono small">ts_utc: ' + data.timestamp + '</div>' +
                    '<div class="mono small">model: ' + data.modelVersion + '</div>' +
                    '<hr/>' +
                    '<div class="row g-2">' +
                      '<div class="col-6 mono">Current Close</div><div class="col-6 text-end mono">$' + fmtPrice(data.currentClose) + '</div>' +
                      '<div class="col-6 mono">Prediction</div><div class="col-6 text-end mono">' + data.predictionLabel + ' (' + fmtProb(data.probability) + ')</div>' +
                      '<div class="col-6 mono">Duration</div><div class="col-6 text-end mono">' + (data.durationMs/1000).toFixed(2) + 's</div>' +
                    '</div>' +
                    dbWarn +
                  '</div>';
            } else {
                box.innerHTML = 
                  '<div class="cardx rounded-3 p-3 bad">' +
                    '<div class="fw-bold mb-2">예측 실패 ❌</div>' +
                    '<div class="mono small">code: ' + (data.errorCode ?? "-") + '</div>' +
                    '<div class="mono small">message: ' + (data.errorMessage ?? "-") + '</div>' +
                  '</div>';
            }

            flash(box);
            await loadPredictions();

        } catch (e) {
            box.innerHTML = 
              '<div class="cardx rounded-3 p-3 bad">' +
                '<div class="fw-bold mb-2">예측 호출 오류 ❌</div>' +
                '<div class="mono small">' + e + '</div>' +
              '</div>';
            flash(box);
        } finally {
            setLoading("btnPredict","predSpinner", false);
        }
    }

    async function loadPredictions() {
        const symbol = document.getElementById("predSymbol").value;
        const res = await fetch("/api/model/predictions?symbol=" + encodeURIComponent(symbol) + "&limit=10", {cache:"no-store"});
        const rows = await res.json();

        const tbody = document.getElementById("predTableBody");
        if (!rows || rows.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="muted">데이터 없음</td></tr>';
            return;
        }

        let html = '';
        for (let i = 0; i < rows.length; i++) {
            const r = rows[i];
            const up = (r.prediction === 1);
            const badge = up ? "text-bg-success" : "text-bg-warning";
            const label = up ? "UP" : "DOWN";
            html += '<tr>' +
                '<td class="mono">' + (r.tsUtc ?? "-") + '</td>' +
                '<td class="mono text-end">$' + fmtPrice(r.currentClose) + '</td>' +
                '<td class="text-center"><span class="badge ' + badge + ' mono">' + label + '</span></td>' +
                '<td class="mono text-end">' + fmtProb(r.probability) + '</td>' +
                '<td class="mono">' + (r.modelVersion ?? "-") + '</td>' +
              '</tr>';
        }
        tbody.innerHTML = html;

        flash(tbody);
    }

    // init
    loadStatus();
    loadPredictions();
    setInterval(loadPredictions, 30000);
</script>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
