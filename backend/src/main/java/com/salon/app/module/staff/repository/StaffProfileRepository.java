package com.salon.app.module.staff.repository;

import com.salon.app.module.staff.entity.StaffProfile;
import com.salon.app.shared.enums.StaffStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffProfileRepository extends JpaRepository<StaffProfile, UUID> {
    List<StaffProfile> findByOutletIdAndIsDeletedFalse(UUID outletId);
    List<StaffProfile> findByOutletIdAndStatusAndIsDeletedFalse(UUID outletId, StaffStatus status);
    List<StaffProfile> findByIsDeletedFalse();
    Optional<StaffProfile> findByUserIdAndIsDeletedFalse(UUID userId);

    @Query("SELECT sp FROM StaffProfile sp WHERE sp.isDeleted = false " +
           "AND (:outletId IS NULL OR sp.outlet.id = :outletId) " +
           "AND (:status IS NULL OR sp.status = :status)")
    List<StaffProfile> findByFilters(UUID outletId, StaffStatus status);
}
