package com.xiongdwm.future_backend.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.xiongdwm.future_backend.entity.LeaveRecord;
import com.xiongdwm.future_backend.entity.User;

@Repository
public interface LeaveRecordRepository extends JpaRepository<LeaveRecord, Long>, JpaSpecificationExecutor<LeaveRecord> {

    @EntityGraph(attributePaths = {"user"})
    List<LeaveRecord> findByUserOrderByIdDesc(User user);

    List<LeaveRecord> findByUserAndCancelDateIsNullOrderByIdDesc(User user);

    @EntityGraph(attributePaths = {"user"})
    List<LeaveRecord> findByCancelDateIsNullAndStartDateAfter(Date date);

    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<LeaveRecord> findAll(Specification<LeaveRecord> spec, Pageable pageable);
}
