package com.salon.app.module.service.controller;

import com.salon.app.module.service.dto.request.PackageRequest;
import com.salon.app.module.service.dto.request.ServiceRequest;
import com.salon.app.module.service.dto.response.CategoryResponse;
import com.salon.app.module.service.dto.response.PackageResponse;
import com.salon.app.module.service.dto.response.ServiceResponse;
import com.salon.app.module.service.service.SalonServiceManager;
import com.salon.app.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ServiceController {

    private final SalonServiceManager salonServiceManager;

    @GetMapping("/api/v1/service-categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(salonServiceManager.getCategories()));
    }

    @GetMapping("/api/v1/services")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getServices(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID outletId) {
        return ResponseEntity.ok(ApiResponse.success(salonServiceManager.getServices(categoryId, outletId)));
    }

    @PostMapping("/api/v1/services")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceResponse>> createService(@Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service created", salonServiceManager.createService(request)));
    }

    @PutMapping("/api/v1/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceResponse>> updateService(@PathVariable UUID id,
                                                                       @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Service updated", salonServiceManager.updateService(id, request)));
    }

    @DeleteMapping("/api/v1/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable UUID id) {
        salonServiceManager.deleteService(id);
        return ResponseEntity.ok(ApiResponse.success("Service deleted", null));
    }

    @GetMapping("/api/v1/packages")
    public ResponseEntity<ApiResponse<List<PackageResponse>>> getPackages(
            @RequestParam(required = false) UUID outletId) {
        return ResponseEntity.ok(ApiResponse.success(salonServiceManager.getPackages(outletId)));
    }

    @PostMapping("/api/v1/packages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PackageResponse>> createPackage(@Valid @RequestBody PackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Package created", salonServiceManager.createPackage(request)));
    }

    @PutMapping("/api/v1/packages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PackageResponse>> updatePackage(@PathVariable UUID id,
                                                                       @Valid @RequestBody PackageRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Package updated", salonServiceManager.updatePackage(id, request)));
    }

    @DeleteMapping("/api/v1/packages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePackage(@PathVariable UUID id) {
        salonServiceManager.deletePackage(id);
        return ResponseEntity.ok(ApiResponse.success("Package deleted", null));
    }
}
