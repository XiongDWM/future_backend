package com.xiongdwm.future_backend.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xiongdwm.future_backend.entity.OrderSection;

@Repository
public interface OrderSectionRepository extends JpaRepository<OrderSection, String>, JpaSpecificationExecutor<OrderSection> {
    List<OrderSection> findByOrderOrderId(String orderId);
    List<OrderSection> findByOrderOrderIdAndFinishedFalse(String orderId);
    List<OrderSection> findByOrderUserIdAndEndAtGreaterThanAndEndAtLessThan(Long userId, Date from, Date to);
    List<OrderSection> findByEndAtGreaterThanAndEndAtLessThan(Date from, Date to);

    @Query("SELECT s FROM OrderSection s JOIN FETCH s.order o WHERE o.userId IN :userIds AND s.endAt > :from AND s.endAt < :to")
    List<OrderSection> findByUserIdsAndEndAtBetween(@Param("userIds") List<Long> userIds, @Param("from") Date from, @Param("to") Date to);
}
