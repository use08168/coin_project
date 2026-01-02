package com.team_biance.the_coin_killer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/model")
public class ModelController {

    @GetMapping("/control")
    public String control() {
        return "model/control";
    }
}
