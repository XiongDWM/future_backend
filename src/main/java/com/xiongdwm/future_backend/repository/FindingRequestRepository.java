package com.xiongdwm.future_backend.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.xiongdwm.future_backend.entity.FindingRequest;

@Repository
public interface FindingRequestRepository extends JpaRepository<FindingRequest, Long>, JpaSpecificationExecutor<FindingRequest> {
    @Override
    @EntityGraph(attributePaths = {"palworld", "order"})
    List<FindingRequest> findAll(Sort sort);
}
