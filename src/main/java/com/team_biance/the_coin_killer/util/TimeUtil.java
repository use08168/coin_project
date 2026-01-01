package com.team_biance.the_coin_killer.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class TimeUtil {

    public static Instant fromEpochMs(long ms) {
        return Instant.ofEpochMilli(ms);
    }

    public static Instant floorToSecond(Instant t) {
        return t.truncatedTo(ChronoUnit.SECONDS);
    }

    public static Instant floorToMinute(Instant t) {
        return t.truncatedTo(ChronoUnit.MINUTES);
    }

    public static ZonedDateTime utc(Instant t) {
        return t.atZone(ZoneOffset.UTC);
    }
}
