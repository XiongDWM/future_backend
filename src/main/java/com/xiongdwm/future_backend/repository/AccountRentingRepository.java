package com.xiongdwm.future_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.xiongdwm.future_backend.entity.AccountRenting;

@Repository
public interface AccountRentingRepository extends JpaRepository<AccountRenting, Long>, JpaSpecificationExecutor<AccountRenting> {

}
