package com.team_biance.the_coin_killer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/compare")
public class CompareController {

    @GetMapping("/realtime")
    public String realtime() {
        return "compare/realtime";
    }
}
