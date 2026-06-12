package com.salon.app.module.gallery.controller;

import com.salon.app.module.gallery.dto.GalleryItemResponse;
import com.salon.app.module.gallery.service.GalleryService;
import com.salon.app.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gallery")
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryService galleryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GalleryItemResponse>>> getGallery(
            @RequestParam(required = false) UUID categoryId) {
        return ResponseEntity.ok(ApiResponse.success(galleryService.getGallery(categoryId)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GalleryItemResponse>> uploadGalleryItem(
            @RequestParam UUID categoryId,
            @RequestParam(required = false) String title,
            @RequestPart MultipartFile beforeImage,
            @RequestPart MultipartFile afterImage,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        GalleryItemResponse response = galleryService.uploadGalleryItem(
                categoryId, title, beforeImage, afterImage, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gallery item uploaded", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGalleryItem(@PathVariable UUID id) {
        galleryService.deleteGalleryItem(id);
        return ResponseEntity.ok(ApiResponse.success("Gallery item deleted", null));
    }
}
