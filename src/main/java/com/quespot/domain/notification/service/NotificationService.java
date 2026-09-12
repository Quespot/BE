package com.quespot.domain.notification.service;

import com.quespot.domain.notification.dto.NotificationCommand;
import com.quespot.domain.notification.entity.Notification;
import com.quespot.domain.notification.event.NotificationCreatedEvent;
import com.quespot.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 알림 생성의 유일한 진입점. 호출부 트랜잭션에 합류(REQUIRED)해 행을 저장하고 이벤트를
// 발행한다 — 실제 푸시는 NotificationPushListener가 커밋 뒤에 보낸다. 호출부에 트랜잭션이
// 없으면 이 메서드가 새로 열므로 리스너는 그 커밋 뒤에 돈다. 다른 작업(미션 완료 등)과 알림이
// 원자적이어야 하면 그 작업의 트랜잭션 안에서 호출해야 한다.
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Notification notify(NotificationCommand command) {
        Notification saved = notificationRepository.save(Notification.create(
                command.userId(), command.type(), command.title(), command.body(),
                command.referenceType(), command.referenceId()
        ));
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                saved.getId(), saved.getUserId(), saved.getTitle(), saved.getBody(),
                saved.getType(), saved.getReferenceType(), saved.getReferenceId()
        ));
        return saved;
    }
}
