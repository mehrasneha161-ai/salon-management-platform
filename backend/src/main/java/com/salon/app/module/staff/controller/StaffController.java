package com.salon.app.module.staff.controller;

import com.salon.app.module.auth.repository.UserRepository;
import com.salon.app.module.staff.dto.request.RegisterStaffRequest;
import com.salon.app.module.staff.dto.response.AttendanceResponse;
import com.salon.app.module.staff.dto.response.StaffResponse;
import com.salon.app.module.staff.service.StaffService;
import com.salon.app.shared.enums.StaffStatus;
import com.salon.app.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<List<StaffResponse>>> getStaff(
            @RequestParam(required = false) UUID outletId,
            @RequestParam(required = false) StaffStatus status) {
        return ResponseEntity.ok(ApiResponse.success(staffService.getStaff(outletId, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffResponse>> getStaffById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(staffService.getStaffById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StaffResponse>> registerStaff(@Valid @RequestBody RegisterStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff registered", staffService.registerStaff(request)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<StaffResponse>> updateStatus(@PathVariable UUID id,
                                                                    @RequestParam StaffStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", staffService.updateStatus(id, status)));
    }

    @PostMapping("/attendance/check-in")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Checked in", staffService.checkIn(resolveUserId(userDetails))));
    }

    @PostMapping("/attendance/check-out")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Checked out", staffService.checkOut(resolveUserId(userDetails))));
    }

    @GetMapping("/{id}/attendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAttendance(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(ApiResponse.success(staffService.getAttendance(id, year, month)));
    }

    private UUID resolveUserId(UserDetails userDetails) {
        return userRepository.findByPhoneNumberAndIsDeletedFalse(userDetails.getUsername())
                .orElseThrow().getId();
    }
}
