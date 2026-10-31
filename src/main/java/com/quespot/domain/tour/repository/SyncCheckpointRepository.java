package com.quespot.domain.tour.repository;

import com.quespot.domain.tour.entity.SyncCheckpoint;
import com.quespot.domain.tour.entity.SyncCheckpointId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncCheckpointRepository extends JpaRepository<SyncCheckpoint, SyncCheckpointId> {
}
