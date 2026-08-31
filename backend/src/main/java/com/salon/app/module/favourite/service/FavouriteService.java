package com.salon.app.module.favourite.service;

import com.salon.app.module.staff.dto.response.StaffResponse;

import java.util.List;
import java.util.UUID;

public interface FavouriteService {
    void addFavourite(UUID customerId, UUID staffId);
    void removeFavourite(UUID customerId, UUID staffId);
    List<StaffResponse> getFavourites(UUID customerId);
}
