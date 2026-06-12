package com.salon.app.module.outlet.repository;

import com.salon.app.module.outlet.entity.Outlet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutletRepository extends JpaRepository<Outlet, UUID> {
    List<Outlet> findByIsActiveTrueAndIsDeletedFalse();
    List<Outlet> findByIsDeletedFalse();
}
