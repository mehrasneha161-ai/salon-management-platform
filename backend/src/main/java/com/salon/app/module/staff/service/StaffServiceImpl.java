package com.salon.app.module.staff.service;

import com.salon.app.module.auth.entity.User;
import com.salon.app.module.auth.repository.UserRepository;
import com.salon.app.module.outlet.entity.Outlet;
import com.salon.app.module.outlet.repository.OutletRepository;
import com.salon.app.module.staff.dto.request.LeaveRequest;
import com.salon.app.module.staff.dto.request.RegisterStaffRequest;
import com.salon.app.module.staff.dto.request.UpdateShiftRequest;
import com.salon.app.module.staff.dto.response.AttendanceResponse;
import com.salon.app.module.staff.dto.response.LeaveResponse;
import com.salon.app.module.staff.dto.response.StaffResponse;
import com.salon.app.module.staff.entity.StaffAttendance;
import com.salon.app.module.staff.entity.StaffLeave;
import com.salon.app.module.staff.entity.StaffProfile;
import com.salon.app.module.staff.repository.StaffAttendanceRepository;
import com.salon.app.module.staff.repository.StaffLeaveRepository;
import com.salon.app.module.staff.repository.StaffProfileRepository;
import com.salon.app.shared.enums.StaffStatus;
import com.salon.app.shared.enums.UserRole;
import com.salon.app.shared.exception.BusinessException;
import com.salon.app.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final StaffProfileRepository staffProfileRepository;
    private final StaffAttendanceRepository attendanceRepository;
    private final StaffLeaveRepository leaveRepository;
    private final UserRepository userRepository;
    private final OutletRepository outletRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public StaffResponse registerStaff(RegisterStaffRequest request) {
        log.info("Registering staff: {}", request.getPhoneNumber());
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("Phone number already registered");
        }
        Outlet outlet = outletRepository.findById(request.getOutletId())
                .orElseThrow(() -> new ResourceNotFoundException("Outlet", "id", request.getOutletId()));
        User user = User.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.STAFF)
                .isActive(true)
                .build();
        userRepository.save(user);
        StaffProfile profile = StaffProfile.builder()
                .user(user)
                .outlet(outlet)
                .specialization(request.getSpecialization())
                .bio(request.getBio())
                .status(StaffStatus.AVAILABLE)
                .build();
        staffProfileRepository.save(profile);
        log.info("Staff registered: {}", profile.getId());
        return toResponse(profile);
    }

    @Override
    public List<StaffResponse> getStaff(UUID outletId, StaffStatus status) {
        return staffProfileRepository.findByFilters(outletId, status)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public StaffResponse getStaffById(UUID id) {
        return toResponse(findById(id));
    }

    @Override
    public StaffResponse getMyProfile(UUID userId) {
        StaffProfile profile = staffProfileRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "userId", userId));
        return toResponse(profile);
    }

    @Override
    @Transactional
    public StaffResponse updateStatus(UUID staffId, StaffStatus status) {
        log.info("Updating staff {} status to {}", staffId, status);
        StaffProfile profile = findById(staffId);
        profile.setStatus(status);
        return toResponse(staffProfileRepository.save(profile));
    }

    @Override
    @Transactional
    public StaffResponse updateShift(UUID staffId, UpdateShiftRequest request) {
        log.info("Updating shift for staff {} to {}–{}", staffId, request.getShiftStart(), request.getShiftEnd());
        StaffProfile profile = findById(staffId);
        if (request.getShiftStart() != null && request.getShiftEnd() != null
                && !request.getShiftStart().isBefore(request.getShiftEnd())) {
            throw new BusinessException("Shift start must be before shift end");
        }
        profile.setShiftStart(request.getShiftStart());
        profile.setShiftEnd(request.getShiftEnd());
        return toResponse(staffProfileRepository.save(profile));
    }

    @Override
    @Transactional
    public AttendanceResponse checkIn(UUID userId) {
        log.info("Check-in for user: {}", userId);
        StaffProfile profile = staffProfileRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "userId", userId));
        LocalDate today = LocalDate.now();
        if (attendanceRepository.findByStaffIdAndDate(profile.getId(), today).isPresent()) {
            throw new BusinessException("Already checked in today");
        }
        StaffAttendance attendance = StaffAttendance.builder()
                .staff(profile)
                .date(today)
                .checkInAt(Instant.now())
                .status("PRESENT")
                .build();
        return toAttendanceResponse(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional
    public AttendanceResponse checkOut(UUID userId) {
        log.info("Check-out for user: {}", userId);
        StaffProfile profile = staffProfileRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "userId", userId));
        StaffAttendance attendance = attendanceRepository
                .findByStaffIdAndDate(profile.getId(), LocalDate.now())
                .orElseThrow(() -> new BusinessException("No check-in found for today"));
        if (attendance.getCheckOutAt() != null) {
            throw new BusinessException("Already checked out today");
        }
        attendance.setCheckOutAt(Instant.now());
        return toAttendanceResponse(attendanceRepository.save(attendance));
    }

    @Override
    public List<AttendanceResponse> getAttendance(UUID staffId, Integer year, Integer month) {
        if (year != null && month != null) {
            return attendanceRepository.findByStaffIdAndYearAndMonth(staffId, year, month)
                    .stream().map(this::toAttendanceResponse).toList();
        }
        return attendanceRepository.findByStaffIdOrderByDateDesc(staffId)
                .stream().map(this::toAttendanceResponse).toList();
    }

    @Override
    @Transactional
    public LeaveResponse addLeave(UUID staffId, LeaveRequest request) {
        log.info("Adding leave for staff {}: {} to {}", staffId, request.getStartDate(), request.getEndDate());
        StaffProfile profile = findById(staffId);
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("Leave end date cannot be before the start date");
        }
        StaffLeave leave = StaffLeave.builder()
                .staff(profile)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .build();
        return toLeaveResponse(leaveRepository.save(leave));
    }

    @Override
    public List<LeaveResponse> getLeaves(UUID staffId) {
        return leaveRepository.findByStaffIdAndIsDeletedFalseOrderByStartDateDesc(staffId)
                .stream().map(this::toLeaveResponse).toList();
    }

    @Override
    @Transactional
    public void cancelLeave(UUID leaveId) {
        log.info("Cancelling leave: {}", leaveId);
        StaffLeave leave = leaveRepository.findById(leaveId)
                .filter(l -> !l.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("StaffLeave", "id", leaveId));
        leave.setDeleted(true);
        leaveRepository.save(leave);
    }

    private StaffProfile findById(UUID id) {
        return staffProfileRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "id", id));
    }

    private StaffResponse toResponse(StaffProfile p) {
        return StaffResponse.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .fullName(p.getUser().getFullName())
                .phoneNumber(p.getUser().getPhoneNumber())
                .specialization(p.getSpecialization())
                .bio(p.getBio())
                .profilePicUrl(p.getProfilePicUrl())
                .status(p.getStatus().name())
                .outletId(p.getOutlet().getId())
                .outletName(p.getOutlet().getName())
                .shiftStart(p.getShiftStart())
                .shiftEnd(p.getShiftEnd())
                .totalPresentDays(attendanceRepository.countPresentDays(p.getId()))
                .createdAt(p.getCreatedAt())
                .build();
    }

    private LeaveResponse toLeaveResponse(StaffLeave l) {
        return LeaveResponse.builder()
                .id(l.getId())
                .staffId(l.getStaff().getId())
                .startDate(l.getStartDate())
                .endDate(l.getEndDate())
                .reason(l.getReason())
                .build();
    }

    private AttendanceResponse toAttendanceResponse(StaffAttendance a) {
        return AttendanceResponse.builder()
                .id(a.getId())
                .date(a.getDate())
                .checkInAt(a.getCheckInAt())
                .checkOutAt(a.getCheckOutAt())
                .status(a.getStatus())
                .build();
    }
}
