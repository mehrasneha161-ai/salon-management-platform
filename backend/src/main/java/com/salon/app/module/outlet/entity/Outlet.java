package com.salon.app.module.outlet.entity;

import com.salon.app.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "outlets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Outlet extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 15)
    private String phone;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime = LocalTime.of(9, 0);

    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime = LocalTime.of(20, 0);
}
