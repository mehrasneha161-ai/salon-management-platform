package com.salon.app.module.service.repository;

import com.salon.app.module.service.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID> {
    List<ServiceCategory> findByIsActiveTrueAndIsDeletedFalseOrderBySortOrderAsc();
}
