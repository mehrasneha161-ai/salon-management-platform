package com.salon.app.module.analytics.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import com.salon.app.module.booking.entity.Booking;

import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface AnalyticsRepository extends Repository<Booking, UUID> {

    @Query(value = "SELECT b.outlet_id, o.name as outlet_name, " +
                   "SUM(b.total_amount) as total_revenue, COUNT(b.id) as total_bookings " +
                   "FROM bookings b JOIN outlets o ON b.outlet_id = o.id " +
                   "WHERE b.status = 'COMPLETED' AND b.is_deleted = false " +
                   "GROUP BY b.outlet_id, o.name ORDER BY total_revenue DESC",
           nativeQuery = true)
    List<Object[]> findOutletRevenue();

    @Query(value = "SELECT b.service_id, s.name as service_name, sc.name as category_name, " +
                   "COUNT(b.id) as booking_count " +
                   "FROM bookings b JOIN services s ON b.service_id = s.id " +
                   "JOIN service_categories sc ON s.category_id = sc.id " +
                   "WHERE b.status = 'COMPLETED' AND b.is_deleted = false AND b.service_id IS NOT NULL " +
                   "GROUP BY b.service_id, s.name, sc.name ORDER BY booking_count DESC LIMIT 10",
           nativeQuery = true)
    List<Object[]> findPopularServices();
}
