package com.salon.app.module.service.service;

import com.salon.app.module.service.dto.request.PackageRequest;
import com.salon.app.module.service.dto.request.ServiceRequest;
import com.salon.app.module.service.dto.response.PackageResponse;
import com.salon.app.module.service.dto.response.ServiceResponse;

import java.util.List;
import java.util.UUID;

public interface SalonServiceManager {
    List<ServiceResponse> getServices(UUID categoryId, UUID outletId);
    ServiceResponse createService(ServiceRequest request);
    ServiceResponse updateService(UUID id, ServiceRequest request);
    void deleteService(UUID id);
    List<PackageResponse> getPackages(UUID outletId);
    PackageResponse createPackage(PackageRequest request);
    PackageResponse updatePackage(UUID id, PackageRequest request);
    void deletePackage(UUID id);
}
