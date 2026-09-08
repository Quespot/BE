package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.repository.MissionCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MissionCandidateWriter {

    private final MissionCandidateRepository missionCandidateRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(MissionCandidate candidate) {
        missionCandidateRepository.saveAndFlush(candidate);
    }
}
