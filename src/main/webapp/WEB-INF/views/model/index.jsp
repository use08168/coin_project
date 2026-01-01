<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
    <title>Monitor</title>
    <style>
        body { font-family: Arial, sans-serif; padding: 16px; }
        .ok { color: green; font-weight: bold; }
        .bad { color: red; font-weight: bold; }
        table { border-collapse: collapse; width: 720px; }
        th, td { border: 1px solid #ddd; padding: 8px; }
        th { background: #f7f7f7; text-align: left; }
        .nav a { margin-right: 12px; }
    </style>
</head>
<body>

<div class="nav">
    <a href="/monitor">/monitor</a>
    <a href="/model">/model</a>
    <a href="/compare">/compare</a>
    <a href="/report">/report</a>
</div>

<h2>DB / 수집 상태 - ${symbol}</h2>

<p>
    DB 연결:
    <c:choose>
        <c:when test="${dbOk}"><span class="ok">OK</span></c:when>
        <c:otherwise><span class="bad">ERROR</span></c:otherwise>
    </c:choose>
</p>

<table>
    <tr><th>테이블</th><th>행 수</th><th>마지막 ts_utc</th></tr>
    <tr><td>f_depth_snapshot_1s</td><td>${stats.depthCount}</td><td>${stats.depthLastTs}</td></tr>
    <tr><td>feature_minute</td><td>${stats.featureMinuteCount}</td><td>${stats.featureMinuteLastTs}</td></tr>
    <tr><td>feature_hour</td><td>${stats.featureHourCount}</td><td>${stats.featureHourLastTs}</td></tr>
    <tr><td>model_pred_60m</td><td>${stats.predCount}</td><td>${stats.predLastTs}</td></tr>
</table>

<p style="margin-top: 16px;">
    다음 단계: (1) Binance 수집을 붙이면 feature_minute/hour가 쌓이고, (2) Python 모델이 떠있으면 /model에서 예측이 바로 보임.
</p>

</body>
</html>
