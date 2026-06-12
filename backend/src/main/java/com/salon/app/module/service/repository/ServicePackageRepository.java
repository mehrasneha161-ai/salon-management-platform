package com.salon.app.module.service.repository;

import com.salon.app.module.service.entity.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, UUID> {
    List<ServicePackage> findByIsActiveTrueAndIsDeletedFalse();
    List<ServicePackage> findByOutletIdAndIsActiveTrueAndIsDeletedFalse(UUID outletId);
}
