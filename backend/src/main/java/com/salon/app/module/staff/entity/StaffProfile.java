package com.salon.app.module.staff.entity;

import com.salon.app.module.auth.entity.User;
import com.salon.app.module.outlet.entity.Outlet;
import com.salon.app.shared.entity.BaseEntity;
import com.salon.app.shared.enums.StaffStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "staff_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outlet_id", nullable = false)
    private Outlet outlet;

    @Column(length = 200)
    private String specialization;

    @Column
    private String bio;

    @Column(name = "profile_pic_url")
    private String profilePicUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffStatus status = StaffStatus.AVAILABLE;

    // Optional per-staff shift. NULL means the staff follows the outlet hours.
    @Column(name = "shift_start")
    private LocalTime shiftStart;

    @Column(name = "shift_end")
    private LocalTime shiftEnd;
}
