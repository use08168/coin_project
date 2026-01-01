package com.team_biance.the_coin_killer.controller;

import com.team_biance.the_coin_killer.dao.ModelPredDao;
import com.team_biance.the_coin_killer.model.ModelPredView;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ModelController {

    private final ModelPredDao modelPredDao;

    public ModelController(ModelPredDao modelPredDao) {
        this.modelPredDao = modelPredDao;
    }

    @GetMapping("/model")
    public String modelPage(Model model) {
        List<ModelPredView> preds = modelPredDao.listRecent("BTCUSDT", 50);
        model.addAttribute("symbol", "BTCUSDT");
        model.addAttribute("preds", preds);
        return "model/index";
    }
}
