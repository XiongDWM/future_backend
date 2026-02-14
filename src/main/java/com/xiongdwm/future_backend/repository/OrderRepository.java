package com.xiongdwm.future_backend.repository;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.xiongdwm.future_backend.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, String>, JpaSpecificationExecutor<Order> {

    @Override
    @EntityGraph(attributePaths = {"palworld"})
    <S extends Order> Page<S> findAll(Example<S> example, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"palworld"})
    Page<Order> findAll(Specification<Order> spec, Pageable pageable);
}
