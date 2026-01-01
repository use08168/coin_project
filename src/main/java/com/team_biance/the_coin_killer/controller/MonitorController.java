package com.team_biance.the_coin_killer.controller;

import com.team_biance.the_coin_killer.model.MonitorStats;
import com.team_biance.the_coin_killer.service.StatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MonitorController {

    private final StatsService statsService;

    public MonitorController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/monitor")
    public String monitor(Model model) {
        boolean ok = statsService.ping();
        MonitorStats s = statsService.load("BTCUSDT");

        model.addAttribute("dbOk", ok);
        model.addAttribute("stats", s);
        model.addAttribute("symbol", "BTCUSDT");
        return "monitor/status";
    }
}
