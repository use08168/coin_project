package com.team_biance.the_coin_killer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReportController {

    @GetMapping("/report")
    public String report(Model model) {
        model.addAttribute("symbol", "BTCUSDT");
        return "report/index";
    }
}
