package com.salon.app.module.favourite.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/** Composite primary key for {@link FavoriteStaff} (customer_id, staff_id). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteStaffId implements Serializable {
    private UUID customerId;
    private UUID staffId;
}
