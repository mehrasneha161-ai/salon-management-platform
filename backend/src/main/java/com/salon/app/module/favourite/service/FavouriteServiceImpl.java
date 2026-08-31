package com.salon.app.module.favourite.service;

import com.salon.app.module.favourite.entity.FavoriteStaff;
import com.salon.app.module.favourite.repository.FavoriteStaffRepository;
import com.salon.app.module.staff.dto.response.StaffResponse;
import com.salon.app.module.staff.service.StaffService;
import com.salon.app.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavouriteServiceImpl implements FavouriteService {

    private final FavoriteStaffRepository favoriteStaffRepository;
    private final StaffService staffService;

    @Override
    @Transactional
    public void addFavourite(UUID customerId, UUID staffId) {
        // Validates the staff exists (throws ResourceNotFoundException otherwise).
        staffService.getStaffById(staffId);
        if (!favoriteStaffRepository.existsByCustomerIdAndStaffId(customerId, staffId)) {
            favoriteStaffRepository.save(FavoriteStaff.builder()
                    .customerId(customerId).staffId(staffId).build());
            log.info("Customer {} favourited staff {}", customerId, staffId);
        }
    }

    @Override
    @Transactional
    public void removeFavourite(UUID customerId, UUID staffId) {
        favoriteStaffRepository.deleteByCustomerIdAndStaffId(customerId, staffId);
        log.info("Customer {} un-favourited staff {}", customerId, staffId);
    }

    @Override
    public List<StaffResponse> getFavourites(UUID customerId) {
        List<StaffResponse> result = new ArrayList<>();
        for (FavoriteStaff fav : favoriteStaffRepository.findByCustomerId(customerId)) {
            try {
                result.add(staffService.getStaffById(fav.getStaffId()));
            } catch (ResourceNotFoundException ignored) {
                // Skip favourites whose staff profile is no longer available.
            }
        }
        return result;
    }
}
