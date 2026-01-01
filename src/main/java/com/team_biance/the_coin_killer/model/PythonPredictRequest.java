package com.team_biance.the_coin_killer.model;

import java.util.Map;

public class PythonPredictRequest {
    public String symbol;
    public String ts_utc; // ISO-8601 string
    public double close_now;
    public Map<String, Object> feature_hour; // 60개 컬럼을 Map으로 그대로 전달
}
