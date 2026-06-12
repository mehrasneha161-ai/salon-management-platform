package com.salon.app.module.gallery.service;

import com.salon.app.module.auth.entity.User;
import com.salon.app.module.auth.repository.UserRepository;
import com.salon.app.module.gallery.dto.GalleryItemResponse;
import com.salon.app.module.gallery.entity.GalleryItem;
import com.salon.app.module.gallery.repository.GalleryRepository;
import com.salon.app.module.service.entity.ServiceCategory;
import com.salon.app.module.service.repository.ServiceCategoryRepository;
import com.salon.app.shared.exception.ResourceNotFoundException;
import com.salon.app.shared.util.S3FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GalleryService {

    private final GalleryRepository galleryRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final S3FileUploadService s3FileUploadService;

    public List<GalleryItemResponse> getGallery(UUID categoryId) {
        List<GalleryItem> items = categoryId != null
                ? galleryRepository.findByCategoryIdAndIsPublishedTrueAndIsDeletedFalse(categoryId)
                : galleryRepository.findByIsPublishedTrueAndIsDeletedFalse();
        return items.stream().map(this::toResponse).toList();
    }

    @Transactional
    public GalleryItemResponse uploadGalleryItem(UUID categoryId, String title,
                                                  MultipartFile beforeFile, MultipartFile afterFile,
                                                  String adminPhone) throws IOException {
        log.info("Uploading gallery item for category: {}", categoryId);
        ServiceCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", "id", categoryId));
        User admin = userRepository.findByPhoneNumberAndIsDeletedFalse(adminPhone)
                .orElseThrow(() -> new ResourceNotFoundException("User", "phone", adminPhone));
        String beforeUrl = s3FileUploadService.uploadFile(beforeFile, "gallery/before");
        String afterUrl = s3FileUploadService.uploadFile(afterFile, "gallery/after");
        GalleryItem item = GalleryItem.builder()
                .category(category)
                .title(title)
                .beforeUrl(beforeUrl)
                .afterUrl(afterUrl)
                .uploadedBy(admin)
                .isPublished(true)
                .build();
        return toResponse(galleryRepository.save(item));
    }

    @Transactional
    public void deleteGalleryItem(UUID id) {
        GalleryItem item = galleryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GalleryItem", "id", id));
        item.setDeleted(true);
        galleryRepository.save(item);
    }

    private GalleryItemResponse toResponse(GalleryItem item) {
        return GalleryItemResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .beforeUrl(item.getBeforeUrl())
                .afterUrl(item.getAfterUrl())
                .categoryId(item.getCategory().getId())
                .categoryName(item.getCategory().getName())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
