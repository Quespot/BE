package com.quespot.domain.tour.service;

import com.quespot.domain.tour.entity.SyncCheckpoint;
import com.quespot.domain.tour.entity.SyncCheckpointId;
import com.quespot.domain.tour.repository.SyncCheckpointRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// SyncCheckpoint의 상태 메서드를 호출만 하고 영속화하는 얇은 오케스트레이션
// 계층이다. 상태 전이 로직 자체는 엔티티가 갖는다.
@Component
public class CheckpointWriter {

    private final SyncCheckpointRepository syncCheckpointRepository;

    public CheckpointWriter(SyncCheckpointRepository syncCheckpointRepository) {
        this.syncCheckpointRepository = syncCheckpointRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncCheckpoint loadOrStart(String ldongRegnCd, Integer contentTypeId, LocalDateTime incrementalModifiedTime) {
        SyncCheckpoint checkpoint = findOrInit(ldongRegnCd, contentTypeId);

        if (checkpoint.isInitialLoadDone()) {
            checkpoint.startIncrementalRun(incrementalModifiedTime);
        }

        return syncCheckpointRepository.save(checkpoint);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void advance(String ldongRegnCd, Integer contentTypeId, int page) {
        SyncCheckpoint checkpoint = require(ldongRegnCd, contentTypeId);
        checkpoint.advance(page);
        syncCheckpointRepository.save(checkpoint);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String ldongRegnCd, Integer contentTypeId) {
        SyncCheckpoint checkpoint = require(ldongRegnCd, contentTypeId);
        checkpoint.complete();
        syncCheckpointRepository.save(checkpoint);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void suspendByQuota(String ldongRegnCd, Integer contentTypeId) {
        SyncCheckpoint checkpoint = require(ldongRegnCd, contentTypeId);
        checkpoint.suspendByQuota();
        syncCheckpointRepository.save(checkpoint);
    }

    // collect()의 catch 블록에서 호출될 수 있는데, 그 시점엔 loadOrStart가 아직
    // 한 번도 성공 못했을 수도 있어(예: loadOrStart 자체가 예외를 던진 경우)
    // require()가 아니라 findOrInit()으로 방어적으로 만든다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(String ldongRegnCd, Integer contentTypeId) {
        SyncCheckpoint checkpoint = findOrInit(ldongRegnCd, contentTypeId);
        checkpoint.fail();
        syncCheckpointRepository.save(checkpoint);
    }

    private SyncCheckpoint findOrInit(String ldongRegnCd, Integer contentTypeId) {
        return syncCheckpointRepository.findById(new SyncCheckpointId(ldongRegnCd, contentTypeId))
                .orElseGet(() -> SyncCheckpoint.init(ldongRegnCd, contentTypeId));
    }

    private SyncCheckpoint require(String ldongRegnCd, Integer contentTypeId) {
        return syncCheckpointRepository.findById(new SyncCheckpointId(ldongRegnCd, contentTypeId))
                .orElseThrow(() -> new IllegalStateException(
                        "존재하지 않는 체크포인트입니다: " + ldongRegnCd + "/" + contentTypeId));
    }
}
