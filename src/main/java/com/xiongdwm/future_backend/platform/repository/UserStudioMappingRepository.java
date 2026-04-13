package com.xiongdwm.future_backend.platform.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xiongdwm.future_backend.platform.entity.UserStudioMapping;

public interface UserStudioMappingRepository extends JpaRepository<UserStudioMapping, Long> {
    Optional<UserStudioMapping> findByUsername(String username);
    boolean existsByUsername(String username);
    long countByStudioId(Long studioId);
}
