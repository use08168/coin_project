package com.team_biance.the_coin_killer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CompareController {

    @GetMapping("/compare")
    public String compare(Model model) {
        model.addAttribute("symbol", "BTCUSDT");
        return "compare/index";
    }
}
