package com.xiongdwm.future_backend.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.xiongdwm.future_backend.bo.BookOrderParam;
import com.xiongdwm.future_backend.entity.BookOrder;
import com.xiongdwm.future_backend.entity.User;
import com.xiongdwm.future_backend.repository.BookOrderRepository;
import com.xiongdwm.future_backend.service.BookOrderService;

import jakarta.persistence.criteria.Predicate;

@Service
public class BookOrderServiceImpl implements BookOrderService {
    @Autowired
    private BookOrderRepository bookOrderRepository;

    @Override
    public boolean createBookOrder(BookOrderParam param,User user) {
        var bookOrder=new BookOrder();
        BeanUtils.copyProperties(param, bookOrder);
        bookOrder.setCreateTime(new Date());
        bookOrder.setRemaining(param.amount());
        bookOrder.setPalworld(user);
        bookOrder.setPid(user.getId());
        bookOrderRepository.save(bookOrder);
        return true; 
    }

    @Override
    public Page<BookOrder> listBookOrders(int page, int size, String customer, String customerId, Long pid) {
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Specification<BookOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (customer != null && !customer.isBlank()) {
                predicates.add(cb.like(root.get("customer"), "%" + customer + "%"));
            }
            if (customerId != null && !customerId.isBlank()) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            if (pid != null) {
                predicates.add(cb.equal(root.get("pid"), pid));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return bookOrderRepository.findAll(spec, pageable);
    }
}
