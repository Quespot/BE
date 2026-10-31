package com.quespot.domain.item.service;

import com.quespot.domain.item.repository.UserItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 기본 보유 아이템(is_default) 지연 지급(#62). 가입 훅은 회원 도메인(허건우)이라 건드리지 않고,
// 보유 목록·퀘스티 조회·장착 진입 시 미보유분을 채운다. 호출부 트랜잭션에 합류한다(REQUIRED) —
// 지급과 이어지는 조회가 어긋날 이유가 없고, 격리가 필요한 독립 항목도 아니다.
@Component
@RequiredArgsConstructor
public class DefaultItemGranter {

    private final UserItemRepository userItemRepository;

    @Transactional
    public void grantMissing(Long userId) {
        userItemRepository.grantDefaultItems(userId);
    }
}
