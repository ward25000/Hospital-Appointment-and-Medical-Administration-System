package com.ward.hospital.controller;

import com.ward.hospital.dto.dashboard.DashboardResponse;
import com.ward.hospital.security.CurrentUserService;
import com.ward.hospital.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserService currentUserService;

    public DashboardController(DashboardService dashboardService, CurrentUserService currentUserService) {
        this.dashboardService = dashboardService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public DashboardResponse getDashboard() {
        return dashboardService.getDashboard(currentUserService.requirePrincipal());
    }
}
