package com.quespot.domain.tour.entity;

import com.quespot.domain.tour.enums.SyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_checkpoints")
@IdClass(SyncCheckpointId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncCheckpoint {

    @Id
    @Column(name = "ldong_regn_cd", columnDefinition = "CHAR(2)")
    private String ldongRegnCd;

    @Id
    @Column(name = "content_type_id")
    private Integer contentTypeId;

    @Column(name = "last_page_no", nullable = false)
    private Integer lastPageNo;

    @Column(name = "last_modified_time")
    private LocalDateTime lastModifiedTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SyncStatus status;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private SyncCheckpoint(String ldongRegnCd, Integer contentTypeId) {
        this.ldongRegnCd = ldongRegnCd;
        this.contentTypeId = contentTypeId;
        this.lastPageNo = 1;
        this.lastModifiedTime = null;
        this.status = SyncStatus.RUNNING;
        this.updatedAt = LocalDateTime.now();
    }

    public static SyncCheckpoint init(String ldongRegnCd, Integer contentTypeId) {
        return new SyncCheckpoint(ldongRegnCd, contentTypeId);
    }

    public int nextPageNo() {
        return lastPageNo;
    }

    public void advance(int page) {
        this.lastPageNo = page + 1;
        this.status = SyncStatus.RUNNING;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        this.lastPageNo = 0;
        this.status = SyncStatus.DONE;
        this.updatedAt = LocalDateTime.now();
    }

    public void suspendByQuota() {
        this.status = SyncStatus.QUOTA_EXCEEDED;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = SyncStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isInitialLoadDone() {
        return this.status == SyncStatus.DONE;
    }

    // 전량 적재가 끝난 뒤 다음 실행이 증분 모드로 전환될 때 호출한다. last_page_no를
    // 1로 되돌리고(complete()가 0으로 내려놓은 값을 재시작 지점으로), 이번 실행에서
    // 실제로 사용할 modifiedtime 하한을 감사 목적으로 기록한다. 다음 실행이 이 값을
    // 다시 읽어 쓰지는 않는다 — 매번 "실행 시점 기준 어제"로 새로 계산한다.
    public void startIncrementalRun(LocalDateTime modifiedTime) {
        this.lastPageNo = 1;
        this.lastModifiedTime = modifiedTime;
        this.status = SyncStatus.RUNNING;
        this.updatedAt = LocalDateTime.now();
    }
}
