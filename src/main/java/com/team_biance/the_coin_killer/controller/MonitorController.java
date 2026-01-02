package com.team_biance.the_coin_killer.controller;

import com.team_biance.the_coin_killer.service.MonitorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/monitor")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(name = "limit", defaultValue = "5") int limit,
            Model model) {
        model.addAttribute("statuses", monitorService.getTableStatuses());

        MonitorService.DashboardRecentData recent = monitorService.getRecentData(limit);
        model.addAttribute("recentKlines", recent.klines);
        model.addAttribute("recentMarks", recent.marks);
        model.addAttribute("recentDepths", recent.depths);
        model.addAttribute("recentForceOrders", recent.forceOrders);
        model.addAttribute("recentAggTrades", recent.aggTrades);
        model.addAttribute("recentFeatureMinutes", recent.featureMinutes);

        model.addAttribute("limit", limit);
        return "monitor/dashboard";
    }
}
