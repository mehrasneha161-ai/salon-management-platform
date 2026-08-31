package com.salon.app.module.favourite.controller;

import com.salon.app.module.auth.repository.UserRepository;
import com.salon.app.module.favourite.service.FavouriteService;
import com.salon.app.module.staff.dto.response.StaffResponse;
import com.salon.app.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/favourites")
@RequiredArgsConstructor
public class FavouriteController {

    private final FavouriteService favouriteService;
    private final UserRepository userRepository;

    @PostMapping("/{staffId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> addFavourite(
            @PathVariable UUID staffId, @AuthenticationPrincipal UserDetails userDetails) {
        favouriteService.addFavourite(resolveUserId(userDetails), staffId);
        return ResponseEntity.ok(ApiResponse.success("Added to favourites", null));
    }

    @DeleteMapping("/{staffId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> removeFavourite(
            @PathVariable UUID staffId, @AuthenticationPrincipal UserDetails userDetails) {
        favouriteService.removeFavourite(resolveUserId(userDetails), staffId);
        return ResponseEntity.ok(ApiResponse.success("Removed from favourites", null));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<StaffResponse>>> getFavourites(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(favouriteService.getFavourites(resolveUserId(userDetails))));
    }

    private UUID resolveUserId(UserDetails userDetails) {
        return userRepository.findByPhoneNumberAndIsDeletedFalse(userDetails.getUsername())
                .orElseThrow().getId();
    }
}
