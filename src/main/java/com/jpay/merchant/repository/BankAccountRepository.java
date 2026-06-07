package com.jpay.merchant.repository;

import com.jpay.merchant.domain.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    List<BankAccount> findByBillerIdAndIsActiveTrueOrderBySortOrderAsc(Long billerId);
}