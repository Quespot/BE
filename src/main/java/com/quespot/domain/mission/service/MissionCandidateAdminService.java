package com.quespot.domain.mission.service;

import com.quespot.domain.mission.converter.MissionConverter;
import com.quespot.domain.mission.dto.req.RejectMissionCandidateRequestDTO;
import com.quespot.domain.mission.dto.req.UpdateMissionCandidateRequestDTO;
import com.quespot.domain.mission.dto.res.MissionCandidateBatchResponseDTO;
import com.quespot.domain.mission.dto.res.MissionCandidateListResponseDTO;
import com.quespot.domain.mission.dto.res.MissionCandidateResponseDTO;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.enums.MissionCandidateStatus;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.MissionCandidateRepository;
import com.quespot.domain.mission.repository.MissionRepository;
import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.enums.UserRole;
import com.quespot.domain.user.enums.UserStatus;
import com.quespot.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MissionCandidateAdminService {

    private final MissionCandidateRepository missionCandidateRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;

    // 미션 후보 목록 조회 로직
    @Transactional(readOnly = true)
    public MissionCandidateListResponseDTO getCandidates(
            MissionCandidateStatus status,
            MissionCategory category,
            int page,
            int size
    ) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<MissionCandidate> candidates = findCandidates(status, category, pageable);

        return MissionConverter.toCandidateListResponse(candidates);
    }

    // 미션 후보 상세 조회 로직
    @Transactional(readOnly = true)
    public MissionCandidateResponseDTO getCandidate(Long candidateId) {
        return MissionConverter.toCandidateResponse(findCandidate(candidateId));
    }

    // 미션 후보 수정 로직
    @Transactional
    public MissionCandidateResponseDTO updateCandidate(
            Long candidateId,
            UpdateMissionCandidateRequestDTO request
    ) {
        MissionCandidate candidate = findCandidateForUpdate(candidateId);
        validateDraft(candidate);
        candidate.update(
                strip(request.title()),
                strip(request.description()),
                request.category(),
                request.rewardPoint(),
                request.estimatedMinutes()
        );

        return MissionConverter.toCandidateResponse(candidate);
    }

    // 미션 후보 일괄 승인 로직
    @Transactional
    public MissionCandidateBatchResponseDTO approveAll(MissionCategory category, Long reviewerId) {
        User reviewer = findAdminReviewer(reviewerId);
        List<MissionCandidate> candidates = missionCandidateRepository.findAllByStatusAndCategoryForUpdate(
                MissionCandidateStatus.DRAFT,
                category
        );

        candidates.forEach(candidate -> candidate.approve(reviewer));

        return new MissionCandidateBatchResponseDTO(candidates.size(), candidates.size(), 0);
    }

    // 미션 후보 반려 로직
    @Transactional
    public MissionCandidateResponseDTO reject(
            Long candidateId,
            Long reviewerId,
            RejectMissionCandidateRequestDTO request
    ) {
        MissionCandidate candidate = findCandidateForUpdate(candidateId);
        validateDraft(candidate);
        candidate.reject(findAdminReviewer(reviewerId), request.reason().trim());

        return MissionConverter.toCandidateResponse(candidate);
    }

    // 미션 일괄 발행 로직
    @Transactional
    public MissionCandidateBatchResponseDTO publishAll(MissionCategory category) {
        List<MissionCandidate> candidates = missionCandidateRepository.findAllByStatusAndCategoryWithSpotForUpdate(
                MissionCandidateStatus.APPROVED,
                category
        );
        if (candidates.isEmpty()) {
            return new MissionCandidateBatchResponseDTO(0, 0, 0);
        }

        List<Long> candidateIds = candidates.stream().map(MissionCandidate::getId).toList();
        Set<Long> alreadyPublishedIds = new HashSet<>(
                missionRepository.findPublishedCandidateIds(candidateIds)
        );
        List<Mission> missions = candidates.stream()
                .filter(candidate -> !alreadyPublishedIds.contains(candidate.getId()))
                .map(Mission::publish)
                .toList();

        missionRepository.saveAll(missions);
        candidates.forEach(MissionCandidate::markPublished);

        return new MissionCandidateBatchResponseDTO(
                candidates.size(),
                missions.size(),
                alreadyPublishedIds.size()
        );
    }

    private MissionCandidate findCandidate(Long candidateId) {
        return missionCandidateRepository.findDetailById(candidateId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.CANDIDATE_NOT_FOUND));
    }

    private Page<MissionCandidate> findCandidates(
            MissionCandidateStatus status,
            MissionCategory category,
            PageRequest pageable
    ) {
        if (status != null && category != null) {
            return missionCandidateRepository.findAllByStatusAndSuggestedCategory(status, category, pageable);
        }
        if (status != null) {
            return missionCandidateRepository.findAllByStatus(status, pageable);
        }
        if (category != null) {
            return missionCandidateRepository.findAllBySuggestedCategory(category, pageable);
        }
        return missionCandidateRepository.findAllBy(pageable);
    }

    private MissionCandidate findCandidateForUpdate(Long candidateId) {
        return missionCandidateRepository.findByIdForUpdate(candidateId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.CANDIDATE_NOT_FOUND));
    }

    private User findAdminReviewer(Long reviewerId) {
        return userRepository.findById(reviewerId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new MissionException(MissionErrorCode.REVIEWER_NOT_FOUND));
    }

    private void validateDraft(MissionCandidate candidate) {
        if (candidate.getStatus() != MissionCandidateStatus.DRAFT) {
            throw new MissionException(MissionErrorCode.CANDIDATE_NOT_DRAFT);
        }
    }

    private String strip(String value) {
        return value == null ? null : value.strip();
    }
}
