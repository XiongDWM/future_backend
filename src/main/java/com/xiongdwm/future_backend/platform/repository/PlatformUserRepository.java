package com.xiongdwm.future_backend.platform.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xiongdwm.future_backend.platform.entity.PlatformUser;

public interface PlatformUserRepository extends JpaRepository<PlatformUser, Long> {
    Optional<PlatformUser> findByUsernameAndDeletedFalse(String username);
    List<PlatformUser> findAllByDeletedFalse();
}
