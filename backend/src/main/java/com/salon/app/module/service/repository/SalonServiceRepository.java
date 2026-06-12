package com.salon.app.module.service.repository;

import com.salon.app.module.service.entity.SalonService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalonServiceRepository extends JpaRepository<SalonService, UUID> {

    @Query("SELECT s FROM SalonService s WHERE s.isDeleted = false AND s.isActive = true " +
           "AND (:categoryId IS NULL OR s.category.id = :categoryId) " +
           "AND (:outletId IS NULL OR s.outlet.id = :outletId OR s.outlet IS NULL)")
    List<SalonService> findByFilters(UUID categoryId, UUID outletId);
}
