package com.team_biance.the_coin_killer.model;

import java.util.List;
import java.util.Map;

public class PythonPredictResponse {
    public String symbol;
    public String ts_utc;
    public int horizon_min;

    public double pred_return_60m_log;
    public double pred_close_60m;

    public String model_version;
    public Integer latency_ms;

    public List<Map<String, Object>> top_features; // optional
}
