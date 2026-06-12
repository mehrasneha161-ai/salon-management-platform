package com.salon.app.module.staff.repository;

import com.salon.app.module.staff.entity.StaffAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, UUID> {
    Optional<StaffAttendance> findByStaffIdAndDate(UUID staffId, LocalDate date);
    List<StaffAttendance> findByStaffIdOrderByDateDesc(UUID staffId);

    @Query("SELECT sa FROM StaffAttendance sa WHERE sa.staff.id = :staffId " +
           "AND YEAR(sa.date) = :year AND MONTH(sa.date) = :month")
    List<StaffAttendance> findByStaffIdAndYearAndMonth(UUID staffId, int year, int month);

    @Query("SELECT COUNT(sa) FROM StaffAttendance sa WHERE sa.staff.id = :staffId AND sa.status = 'PRESENT'")
    long countPresentDays(UUID staffId);
}
