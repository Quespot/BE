package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.repository.MissionCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionCandidateWriter {

    private final MissionCandidateRepository missionCandidateRepository;

    // 청크를 하나의 트랜잭션으로 저장한다. 한 건이라도 실패하면 청크 전체가 롤백된다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAll(List<MissionCandidate> candidates) {
        missionCandidateRepository.saveAll(candidates);
        missionCandidateRepository.flush();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(MissionCandidate candidate) {
        missionCandidateRepository.saveAndFlush(candidate);
    }
}
