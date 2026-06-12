package com.salon.app.module.analytics.controller;

import com.salon.app.module.analytics.dto.OutletRevenueDto;
import com.salon.app.module.analytics.dto.PopularServiceDto;
import com.salon.app.module.analytics.service.AnalyticsService;
import com.salon.app.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/outlet-performance")
    public ResponseEntity<ApiResponse<List<OutletRevenueDto>>> getOutletPerformance() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getOutletRevenue()));
    }

    @GetMapping("/popular-services")
    public ResponseEntity<ApiResponse<List<PopularServiceDto>>> getPopularServices() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getPopularServices()));
    }
}
