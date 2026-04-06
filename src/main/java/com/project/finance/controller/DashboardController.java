package com.project.finance.controller;

import com.project.finance.dto.response.DashboardSummaryResponse;
import com.project.finance.dto.response.TrendResponse;
import com.project.finance.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard Analytics", description = "Aggregated financial summaries and monthly trends")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('VIEWER','ANALYST','ADMIN')")
    @Operation(summary = "Get dashboard summary",
               description = "Returns total income, total expenses, net balance, category totals, and the 5 most recent transactions. Requires VIEWER, ANALYST, or ADMIN role.")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('VIEWER','ANALYST','ADMIN')")
    @Operation(summary = "Get monthly trends",
               description = "Returns monthly aggregated income and expense totals for the current year. Requires VIEWER, ANALYST, or ADMIN role.")
    public ResponseEntity<List<TrendResponse>> getTrends() {
        return ResponseEntity.ok(dashboardService.getTrends());
    }
}
