package com.jpay.merchant.repository;

import com.jpay.merchant.domain.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeeItemCatalogueRepository extends JpaRepository<FeeItemCatalogue, Long> {
    List<FeeItemCatalogue> findByBillerIdAndIsActiveTrueAndApplicableSectionInOrderBySortOrderAsc(
        Long billerId, List<String> sections);
}