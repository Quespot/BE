package com.quespot.domain.tour.service;

import com.quespot.domain.tour.repository.ApiQuotaUsageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class QuotaGuard {

    private final ApiQuotaUsageRepository apiQuotaUsageRepository;
    private final int dailyLimit;

    public QuotaGuard(
            ApiQuotaUsageRepository apiQuotaUsageRepository,
            @Value("${app.tour-api.daily-limit}") int dailyLimit
    ) {
        this.apiQuotaUsageRepository = apiQuotaUsageRepository;
        this.dailyLimit = dailyLimit;
    }

    // 수집이 롤백돼도 이미 소비한 호출은 되돌릴 수 없으므로 카운터는 독립
    // 커밋되어야 실제 사용량과 일치한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryConsume() {
        LocalDate today = LocalDate.now();
        apiQuotaUsageRepository.insertIfAbsent(today);
        int updated = apiQuotaUsageRepository.tryIncrement(today, dailyLimit);
        return updated > 0;
    }
}
