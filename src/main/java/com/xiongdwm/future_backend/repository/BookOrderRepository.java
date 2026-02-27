package com.xiongdwm.future_backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.xiongdwm.future_backend.entity.BookOrder;

public interface BookOrderRepository extends JpaRepository<BookOrder, Long>, JpaSpecificationExecutor<BookOrder> {

    @Override
    @EntityGraph(attributePaths = {"palworld"})
    Page<BookOrder> findAll(Specification<BookOrder> spec, Pageable pageable);
}