package com.salon.app.module.gallery.repository;

import com.salon.app.module.gallery.entity.GalleryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GalleryRepository extends JpaRepository<GalleryItem, UUID> {
    List<GalleryItem> findByIsPublishedTrueAndIsDeletedFalse();
    List<GalleryItem> findByCategoryIdAndIsPublishedTrueAndIsDeletedFalse(UUID categoryId);
}
