package com.jpay.merchant.repository;

import com.jpay.merchant.domain.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByBillerIdAndStatusNotOrderByCreatedAtDesc(Long billerId, String status);
    Optional<Bill> findByBillCode(String billCode);
    boolean existsByBillCode(String code);

    @Query("SELECT b FROM Bill b WHERE b.billerId = :billerId AND b.status = :status ORDER BY b.createdAt DESC")
    List<Bill> findByBillerAndStatus(@Param("billerId") Long billerId, @Param("status") String status);
}