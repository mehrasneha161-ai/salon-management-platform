package com.salon.app.module.staff.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class RegisterStaffRequest {
    @NotBlank
    @Size(min = 2, max = 100)
    private String fullName;
    @NotBlank
    @Pattern(regexp = "^[6-9]\\d{9}$")
    private String phoneNumber;
    @NotBlank
    @Size(min = 8)
    private String password;
    @NotNull
    private UUID outletId;
    private String specialization;
    private String bio;
}
