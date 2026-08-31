package com.salon.app.module.outlet.service;

import com.salon.app.module.outlet.dto.request.OutletRequest;
import com.salon.app.module.outlet.dto.response.OutletResponse;
import com.salon.app.module.outlet.entity.Outlet;
import com.salon.app.module.outlet.repository.OutletRepository;
import com.salon.app.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutletServiceImpl implements OutletService {

    private final OutletRepository outletRepository;

    @Override
    public List<OutletResponse> getAllActiveOutlets() {
        return outletRepository.findByIsActiveTrueAndIsDeletedFalse()
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<OutletResponse> getAllOutlets() {
        return outletRepository.findByIsDeletedFalse()
                .stream().map(this::toResponse).toList();
    }

    @Override
    public OutletResponse getOutletById(UUID id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional
    public OutletResponse createOutlet(OutletRequest request) {
        log.info("Creating outlet: {}", request.getName());
        Outlet outlet = Outlet.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .phone(request.getPhone())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isActive(request.isActive())
                .openingTime(request.getOpeningTime() != null ? request.getOpeningTime() : LocalTime.of(9, 0))
                .closingTime(request.getClosingTime() != null ? request.getClosingTime() : LocalTime.of(20, 0))
                .build();
        return toResponse(outletRepository.save(outlet));
    }

    @Override
    @Transactional
    public OutletResponse updateOutlet(UUID id, OutletRequest request) {
        log.info("Updating outlet: {}", id);
        Outlet outlet = findById(id);
        outlet.setName(request.getName());
        outlet.setAddress(request.getAddress());
        outlet.setCity(request.getCity());
        outlet.setPhone(request.getPhone());
        outlet.setLatitude(request.getLatitude());
        outlet.setLongitude(request.getLongitude());
        outlet.setActive(request.isActive());
        if (request.getOpeningTime() != null) outlet.setOpeningTime(request.getOpeningTime());
        if (request.getClosingTime() != null) outlet.setClosingTime(request.getClosingTime());
        return toResponse(outletRepository.save(outlet));
    }

    @Override
    @Transactional
    public void deleteOutlet(UUID id) {
        log.info("Soft-deleting outlet: {}", id);
        Outlet outlet = findById(id);
        outlet.setDeleted(true);
        outletRepository.save(outlet);
    }

    private Outlet findById(UUID id) {
        return outletRepository.findById(id)
                .filter(o -> !o.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Outlet", "id", id));
    }

    private OutletResponse toResponse(Outlet outlet) {
        return OutletResponse.builder()
                .id(outlet.getId())
                .name(outlet.getName())
                .address(outlet.getAddress())
                .city(outlet.getCity())
                .phone(outlet.getPhone())
                .latitude(outlet.getLatitude())
                .longitude(outlet.getLongitude())
                .isActive(outlet.isActive())
                .openingTime(outlet.getOpeningTime())
                .closingTime(outlet.getClosingTime())
                .createdAt(outlet.getCreatedAt())
                .build();
    }
}
