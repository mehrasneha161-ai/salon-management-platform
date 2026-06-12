package com.salon.app.module.outlet.controller;

import com.salon.app.module.outlet.dto.request.OutletRequest;
import com.salon.app.module.outlet.dto.response.OutletResponse;
import com.salon.app.module.outlet.service.OutletService;
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
@RequestMapping("/api/v1/outlets")
@RequiredArgsConstructor
public class OutletController {

    private final OutletService outletService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OutletResponse>>> getActiveOutlets() {
        return ResponseEntity.ok(ApiResponse.success(outletService.getAllActiveOutlets()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OutletResponse>>> getAllOutlets() {
        return ResponseEntity.ok(ApiResponse.success(outletService.getAllOutlets()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OutletResponse>> getOutletById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(outletService.getOutletById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OutletResponse>> createOutlet(@Valid @RequestBody OutletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Outlet created", outletService.createOutlet(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OutletResponse>> updateOutlet(@PathVariable UUID id,
                                                                     @Valid @RequestBody OutletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Outlet updated", outletService.updateOutlet(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOutlet(@PathVariable UUID id) {
        outletService.deleteOutlet(id);
        return ResponseEntity.ok(ApiResponse.success("Outlet deleted", null));
    }
}
