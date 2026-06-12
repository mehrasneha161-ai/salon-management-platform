package com.salon.app.module.service.entity;

import com.salon.app.module.outlet.entity.Outlet;
import com.salon.app.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "service_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCategory extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
