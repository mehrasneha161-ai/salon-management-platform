package com.salon.app.module.auth.repository;

import com.salon.app.module.auth.entity.User;
import com.salon.app.shared.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhoneNumberAndIsDeletedFalse(String phoneNumber);
    Optional<User> findByEmailAndIsDeletedFalse(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
    List<User> findByRoleAndIsDeletedFalse(UserRole role);
}
