package com.salon.app.module.staff.repository;

import com.salon.app.module.staff.entity.StaffLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StaffLeaveRepository extends JpaRepository<StaffLeave, UUID> {

    // True if the staff has an active leave covering the given date
    // (startDate <= date <= endDate).
    boolean existsByStaffIdAndIsDeletedFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID staffId, LocalDate onOrBeforeDate, LocalDate onOrAfterDate);

    List<StaffLeave> findByStaffIdAndIsDeletedFalseOrderByStartDateDesc(UUID staffId);
}
