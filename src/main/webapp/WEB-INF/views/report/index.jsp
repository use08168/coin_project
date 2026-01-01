<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Compare</title>
    <style>
        body { font-family: Arial, sans-serif; padding: 16px; }
        .nav a { margin-right: 12px; }
        .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
        .box { border: 1px solid #ddd; padding: 12px; }
    </style>
</head>
<body>

<div class="nav">
    <a href="/monitor">/monitor</a>
    <a href="/model">/model</a>
    <a href="/compare">/compare</a>
    <a href="/report">/report</a>
</div>

<h2>TradingView vs MySQL 차트 비교 - ${symbol}</h2>

<div class="grid">
    <div class="box">
        <h3>TradingView (임베드)</h3>
        <p>여기에 TradingView 위젯 스크립트 넣으면 됨.</p>
    </div>

    <div class="box">
        <h3>MySQL 기반 차트</h3>
        <p>나중에 /api/kline 같은 REST 만들어서 Chart.js로 그리면 됨.</p>
    </div>
</div>

</body>
</html>
