package com.team_biance.the_coin_killer.model;

import java.time.Instant;

public class ModelPredView {
    public long predId;
    public String symbol;
    public Instant tsUtc;

    public double closeNow;
    public double predReturn60mLog;
    public double predClose60m;

    public String modelVersion;
    public Integer latencyMs;

    // 최신 수렴 점수(있으면)
    public Double convScore;
    public Double convScoreVol;
    public Instant convObsTsUtc;
}
