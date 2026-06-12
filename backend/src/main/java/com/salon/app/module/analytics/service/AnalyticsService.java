package com.salon.app.module.analytics.service;

import com.salon.app.module.analytics.dto.OutletRevenueDto;
import com.salon.app.module.analytics.dto.PopularServiceDto;
import com.salon.app.module.analytics.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    public List<OutletRevenueDto> getOutletRevenue() {
        log.info("Fetching outlet revenue analytics");
        return analyticsRepository.findOutletRevenue().stream().map(row ->
                OutletRevenueDto.builder()
                        .outletId(UUID.fromString(row[0].toString()))
                        .outletName(row[1].toString())
                        .totalRevenue(new BigDecimal(row[2].toString()))
                        .totalBookings(Long.parseLong(row[3].toString()))
                        .build()
        ).toList();
    }

    public List<PopularServiceDto> getPopularServices() {
        log.info("Fetching popular services analytics");
        return analyticsRepository.findPopularServices().stream().map(row ->
                PopularServiceDto.builder()
                        .serviceId(UUID.fromString(row[0].toString()))
                        .serviceName(row[1].toString())
                        .categoryName(row[2].toString())
                        .bookingCount(Long.parseLong(row[3].toString()))
                        .build()
        ).toList();
    }
}
