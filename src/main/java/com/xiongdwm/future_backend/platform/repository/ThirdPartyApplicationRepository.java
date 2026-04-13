package com.xiongdwm.future_backend.platform.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xiongdwm.future_backend.platform.entity.ThirdPartyApplication;

@Repository
public interface ThirdPartyApplicationRepository extends JpaRepository<ThirdPartyApplication, Long> {

    List<ThirdPartyApplication> findByListingIdOrderByAppliedAtDesc(Long listingId);

    List<ThirdPartyApplication> findByStudioIdOrderByAppliedAtDesc(Long studioId);

    boolean existsByListingIdAndStudioId(Long listingId, Long studioId);

    int countByListingId(Long listingId);

    List<ThirdPartyApplication> findByListingIdAndStatus(Long listingId, ThirdPartyApplication.Status status);

    java.util.Optional<ThirdPartyApplication> findByOrderId(String orderId);
}
