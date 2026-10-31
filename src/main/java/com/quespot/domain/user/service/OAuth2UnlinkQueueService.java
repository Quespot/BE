package com.quespot.domain.user.service;

import com.quespot.domain.user.entity.OAuth2UnlinkTask;
import com.quespot.domain.user.entity.UserSocialAccount;
import com.quespot.domain.user.repository.OAuth2UnlinkTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuth2UnlinkQueueService {

    private final OAuth2UnlinkTaskRepository oAuth2UnlinkTaskRepository;

    // 현재 트랜잭션에 외부 OAuth 연동 해제 재시도 작업을 함께 저장하는 로직
    public void enqueue(UserSocialAccount account) {
        oAuth2UnlinkTaskRepository.save(OAuth2UnlinkTask.create(account));
    }
}
