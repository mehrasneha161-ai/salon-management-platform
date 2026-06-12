package com.salon.app.module.gallery.entity;

import com.salon.app.module.auth.entity.User;
import com.salon.app.module.service.entity.ServiceCategory;
import com.salon.app.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gallery_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GalleryItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    @Column(length = 200)
    private String title;

    @Column(name = "before_url", nullable = false)
    private String beforeUrl;

    @Column(name = "after_url", nullable = false)
    private String afterUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished = true;
}
