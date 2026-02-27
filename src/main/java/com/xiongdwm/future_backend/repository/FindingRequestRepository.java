package com.xiongdwm.future_backend.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.xiongdwm.future_backend.entity.FindingRequest;
import com.xiongdwm.future_backend.entity.User;

@Repository
public interface FindingRequestRepository extends JpaRepository<FindingRequest, Long>, JpaSpecificationExecutor<FindingRequest> {
    @EntityGraph(attributePaths = {"palworld", "order"})
    List<FindingRequest> findByRequestedAtAfterOrderByRequestedAtDesc(Date since);

    @EntityGraph(attributePaths = {"palworld", "order"})
    List<FindingRequest> findByPalworldAndRequestedAtAfterOrderByRequestedAtDesc(User palworld, Date since);
}
