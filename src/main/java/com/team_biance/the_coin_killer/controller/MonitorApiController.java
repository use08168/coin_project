package com.team_biance.the_coin_killer.controller;

import com.team_biance.the_coin_killer.service.MonitorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor")
public class MonitorApiController {

    private final MonitorService monitorService;

    public MonitorApiController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/realtime")
    public MonitorService.RealtimeResponse realtime() {
        return monitorService.getRealtimeSnapshot();
    }
}
