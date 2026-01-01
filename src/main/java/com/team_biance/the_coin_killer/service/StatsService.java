package com.team_biance.the_coin_killer.service;

import com.team_biance.the_coin_killer.dao.StatsDao;
import com.team_biance.the_coin_killer.model.MonitorStats;
import org.springframework.stereotype.Service;

@Service
public class StatsService {
    private final StatsDao statsDao;

    public StatsService(StatsDao statsDao) {
        this.statsDao = statsDao;
    }

    public boolean ping() {
        return statsDao.ping();
    }

    public MonitorStats load(String symbol) {
        return statsDao.loadStats(symbol);
    }
}
