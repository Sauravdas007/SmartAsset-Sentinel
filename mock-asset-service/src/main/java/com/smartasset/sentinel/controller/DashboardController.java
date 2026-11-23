package com.smartasset.sentinel.controller;

import com.smartasset.sentinel.model.AssetStatus;
import com.smartasset.sentinel.model.AssetType;
import com.smartasset.sentinel.model.WorkOrderPriority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("appName", "SmartAsset Sentinel");
        model.addAttribute("assetTypes", AssetType.values());
        model.addAttribute("assetStatuses", AssetStatus.values());
        model.addAttribute("priorities", WorkOrderPriority.values());
        return "dashboard";
    }
}
