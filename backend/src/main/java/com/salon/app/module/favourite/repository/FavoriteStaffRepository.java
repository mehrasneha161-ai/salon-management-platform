package com.salon.app.module.favourite.repository;

import com.salon.app.module.favourite.entity.FavoriteStaff;
import com.salon.app.module.favourite.entity.FavoriteStaffId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FavoriteStaffRepository extends JpaRepository<FavoriteStaff, FavoriteStaffId> {
    List<FavoriteStaff> findByCustomerId(UUID customerId);
    boolean existsByCustomerIdAndStaffId(UUID customerId, UUID staffId);
    void deleteByCustomerIdAndStaffId(UUID customerId, UUID staffId);
}
