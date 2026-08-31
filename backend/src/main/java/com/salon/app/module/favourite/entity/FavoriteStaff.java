package com.salon.app.module.favourite.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A customer's favourite stylist. Composite PK (customer_id, staff_id). */
@Entity
@Table(name = "favorite_staff")
@IdClass(FavoriteStaffId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteStaff {

    @Id
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Id
    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
