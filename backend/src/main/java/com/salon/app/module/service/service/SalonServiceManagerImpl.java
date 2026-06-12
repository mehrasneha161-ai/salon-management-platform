package com.salon.app.module.service.service;

import com.salon.app.module.outlet.entity.Outlet;
import com.salon.app.module.outlet.repository.OutletRepository;
import com.salon.app.module.service.dto.request.PackageRequest;
import com.salon.app.module.service.dto.request.ServiceRequest;
import com.salon.app.module.service.dto.response.PackageResponse;
import com.salon.app.module.service.dto.response.ServiceResponse;
import com.salon.app.module.service.entity.SalonService;
import com.salon.app.module.service.entity.ServiceCategory;
import com.salon.app.module.service.entity.ServicePackage;
import com.salon.app.module.service.repository.SalonServiceRepository;
import com.salon.app.module.service.repository.ServiceCategoryRepository;
import com.salon.app.module.service.repository.ServicePackageRepository;
import com.salon.app.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalonServiceManagerImpl implements SalonServiceManager {

    private final SalonServiceRepository serviceRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final ServicePackageRepository packageRepository;
    private final OutletRepository outletRepository;

    @Override
    public List<ServiceResponse> getServices(UUID categoryId, UUID outletId) {
        return serviceRepository.findByFilters(categoryId, outletId)
                .stream().map(this::toServiceResponse).toList();
    }

    @Override
    @Transactional
    public ServiceResponse createService(ServiceRequest request) {
        log.info("Creating service: {}", request.getName());
        ServiceCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", "id", request.getCategoryId()));
        Outlet outlet = request.getOutletId() != null
                ? outletRepository.findById(request.getOutletId()).orElse(null) : null;
        SalonService service = SalonService.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(category)
                .outlet(outlet)
                .durationMinutes(request.getDurationMinutes())
                .price(request.getPrice())
                .isActive(request.isActive())
                .build();
        return toServiceResponse(serviceRepository.save(service));
    }

    @Override
    @Transactional
    public ServiceResponse updateService(UUID id, ServiceRequest request) {
        SalonService service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));
        ServiceCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", "id", request.getCategoryId()));
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setCategory(category);
        service.setDurationMinutes(request.getDurationMinutes());
        service.setPrice(request.getPrice());
        service.setActive(request.isActive());
        return toServiceResponse(serviceRepository.save(service));
    }

    @Override
    @Transactional
    public void deleteService(UUID id) {
        SalonService service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));
        service.setDeleted(true);
        serviceRepository.save(service);
    }

    @Override
    public List<PackageResponse> getPackages(UUID outletId) {
        List<ServicePackage> packages = outletId != null
                ? packageRepository.findByOutletIdAndIsActiveTrueAndIsDeletedFalse(outletId)
                : packageRepository.findByIsActiveTrueAndIsDeletedFalse();
        return packages.stream().map(this::toPackageResponse).toList();
    }

    @Override
    @Transactional
    public PackageResponse createPackage(PackageRequest request) {
        log.info("Creating package: {}", request.getName());
        Outlet outlet = request.getOutletId() != null
                ? outletRepository.findById(request.getOutletId()).orElse(null) : null;
        Set<SalonService> services = new HashSet<>();
        if (request.getServiceIds() != null) {
            request.getServiceIds().forEach(sid ->
                    serviceRepository.findById(sid).ifPresent(services::add));
        }
        ServicePackage pkg = ServicePackage.builder()
                .name(request.getName())
                .description(request.getDescription())
                .outlet(outlet)
                .price(request.getPrice())
                .discountPct(request.getDiscountPct())
                .services(services)
                .isActive(request.isActive())
                .build();
        return toPackageResponse(packageRepository.save(pkg));
    }

    @Override
    @Transactional
    public PackageResponse updatePackage(UUID id, PackageRequest request) {
        ServicePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package", "id", id));
        pkg.setName(request.getName());
        pkg.setDescription(request.getDescription());
        pkg.setPrice(request.getPrice());
        pkg.setDiscountPct(request.getDiscountPct());
        pkg.setActive(request.isActive());
        return toPackageResponse(packageRepository.save(pkg));
    }

    @Override
    @Transactional
    public void deletePackage(UUID id) {
        ServicePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package", "id", id));
        pkg.setDeleted(true);
        packageRepository.save(pkg);
    }

    private ServiceResponse toServiceResponse(SalonService s) {
        return ServiceResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .categoryId(s.getCategory().getId())
                .categoryName(s.getCategory().getName())
                .outletId(s.getOutlet() != null ? s.getOutlet().getId() : null)
                .durationMinutes(s.getDurationMinutes())
                .price(s.getPrice())
                .isActive(s.isActive())
                .build();
    }

    private PackageResponse toPackageResponse(ServicePackage p) {
        return PackageResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .outletId(p.getOutlet() != null ? p.getOutlet().getId() : null)
                .price(p.getPrice())
                .discountPct(p.getDiscountPct())
                .services(p.getServices().stream().map(this::toServiceResponse).toList())
                .isActive(p.isActive())
                .build();
    }
}
