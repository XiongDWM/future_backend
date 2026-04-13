package com.xiongdwm.future_backend.platform.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xiongdwm.future_backend.platform.entity.ThirdPartyListing;
import com.xiongdwm.future_backend.platform.entity.ThirdPartyListing.Status;

@Repository
public interface ThirdPartyListingRepository extends JpaRepository<ThirdPartyListing, Long> {

    Page<ThirdPartyListing> findByStatusOrderByPostedAtDesc(Status status, Pageable pageable);

    Page<ThirdPartyListing> findByStatusAndGameTypeOrderByPostedAtDesc(Status status, String gameType, Pageable pageable);

    /** 排除指定工作室自己的挂单 */
    Page<ThirdPartyListing> findByStatusAndStudioIdNotOrderByPostedAtDesc(Status status, Long studioId, Pageable pageable);

    Page<ThirdPartyListing> findByStatusAndGameTypeAndStudioIdNotOrderByPostedAtDesc(Status status, String gameType, Long studioId, Pageable pageable);

    List<ThirdPartyListing> findByStudioIdOrderByPostedAtDesc(Long studioId);
}
