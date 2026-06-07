package com.jpay.merchant.service;

import com.jpay.merchant.dto.request.BillRequest;
import com.jpay.merchant.dto.response.BillResponse;
import java.util.List;

public interface BillService {
    BillResponse createDraft(BillRequest request, Long billerId);
    BillResponse updateDraft(Long billId, BillRequest request, Long billerId);
    BillResponse publish(Long billId, Long billerId);
    BillResponse getById(Long billId);
    List<BillResponse> listByBiller(Long billerId, String status);
    void delete(Long billId, Long billerId);
}
