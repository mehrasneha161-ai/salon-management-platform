package com.salon.app.module.gallery.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class GalleryItemResponse {
    private UUID id;
    private String title;
    private String beforeUrl;
    private String afterUrl;
    private UUID categoryId;
    private String categoryName;
    private Instant createdAt;
}
