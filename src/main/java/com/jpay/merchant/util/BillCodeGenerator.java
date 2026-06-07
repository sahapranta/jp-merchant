package com.jpay.merchant.util;

import com.jpay.merchant.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class BillCodeGenerator {

    private final BillRepository billRepository;

    @Value("${app.bill.code-prefix:BILL}")
    private String prefix;

    /**
     * Generates a unique bill code: BILL-202501-A3F9
     */
    public String generate() {
        String ym = LocalDate.now().getYear() +
                    String.format("%02d", LocalDate.now().getMonthValue());
        String candidate;
        int attempts = 0;
        do {
            candidate = prefix + "-" + ym + "-" + randomSuffix();
            if (++attempts > 20) throw new IllegalStateException("Could not generate unique bill code");
        } while (billRepository.existsByBillCode(candidate));
        return candidate;
    }

    private String randomSuffix() {
        int n = ThreadLocalRandom.current().nextInt(0, 0xFFFF);
        return String.format("%04X", n);
    }
}
