package com.team_biance.the_coin_killer.model;

import java.time.Instant;

import lombok.Data;

@Data
public class MonitorStats {
    public long depthCount;
    public Instant depthLastTs;

    public long featureMinuteCount;
    public Instant featureMinuteLastTs;

    public long featureHourCount;
    public Instant featureHourLastTs;

    public long predCount;
    public Instant predLastTs;
}
