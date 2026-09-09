package com.quespot.domain.user.repository;

import com.quespot.domain.user.entity.OAuth2UnlinkTask;
import com.quespot.domain.user.enums.LoginProvider;
import com.quespot.domain.user.enums.OAuth2UnlinkTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OAuth2UnlinkTaskRepository extends JpaRepository<OAuth2UnlinkTask, Long> {

    List<OAuth2UnlinkTask> findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            OAuth2UnlinkTaskStatus status,
            LocalDateTime nextAttemptAt
    );

    void deleteByProviderAndProviderUserId(LoginProvider provider, String providerUserId);
}
