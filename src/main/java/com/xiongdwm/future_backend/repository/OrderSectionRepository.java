package com.xiongdwm.future_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.xiongdwm.future_backend.entity.OrderSection;

@Repository
public interface OrderSectionRepository extends JpaRepository<OrderSection, String>, JpaSpecificationExecutor<OrderSection> {
    List<OrderSection> findByOrderOrderId(String orderId);
    List<OrderSection> findByOrderOrderIdAndFinishedFalse(String orderId);
}
