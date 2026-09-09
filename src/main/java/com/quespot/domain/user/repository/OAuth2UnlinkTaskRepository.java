package com.quespot.domain.user.repository;

import com.quespot.domain.user.entity.OAuth2UnlinkTask;
import com.quespot.domain.user.enums.LoginProvider;
import com.quespot.domain.user.enums.OAuth2UnlinkTaskStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OAuth2UnlinkTaskRepository extends JpaRepository<OAuth2UnlinkTask, Long> {

    List<OAuth2UnlinkTask> findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            OAuth2UnlinkTaskStatus status,
            LocalDateTime nextAttemptAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from OAuth2UnlinkTask task where task.id = :taskId")
    Optional<OAuth2UnlinkTask> findByIdForUpdate(@Param("taskId") Long taskId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select task from OAuth2UnlinkTask task
            where task.provider = :provider and task.providerUserId = :providerUserId
            """)
    Optional<OAuth2UnlinkTask> findByProviderAndProviderUserIdForUpdate(
            @Param("provider") LoginProvider provider,
            @Param("providerUserId") String providerUserId
    );
}
