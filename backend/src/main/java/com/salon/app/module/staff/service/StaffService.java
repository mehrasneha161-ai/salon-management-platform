package com.salon.app.module.staff.service;

import com.salon.app.module.staff.dto.request.LeaveRequest;
import com.salon.app.module.staff.dto.request.RegisterStaffRequest;
import com.salon.app.module.staff.dto.request.UpdateShiftRequest;
import com.salon.app.module.staff.dto.response.AttendanceResponse;
import com.salon.app.module.staff.dto.response.LeaveResponse;
import com.salon.app.module.staff.dto.response.StaffResponse;
import com.salon.app.shared.enums.StaffStatus;

import java.util.List;
import java.util.UUID;

public interface StaffService {
    StaffResponse registerStaff(RegisterStaffRequest request);
    List<StaffResponse> getStaff(UUID outletId, StaffStatus status);
    StaffResponse getStaffById(UUID id);
    StaffResponse updateStatus(UUID staffId, StaffStatus status);
    StaffResponse updateShift(UUID staffId, UpdateShiftRequest request);
    AttendanceResponse checkIn(UUID userId);
    AttendanceResponse checkOut(UUID userId);
    List<AttendanceResponse> getAttendance(UUID staffId, Integer year, Integer month);
    LeaveResponse addLeave(UUID staffId, LeaveRequest request);
    List<LeaveResponse> getLeaves(UUID staffId);
    void cancelLeave(UUID leaveId);
}
