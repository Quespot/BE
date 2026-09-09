package com.quespot.domain.user.repository;

import com.quespot.domain.user.entity.UserSocialAccount;
import com.quespot.domain.user.enums.LoginProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

    Optional<UserSocialAccount> findByProviderAndProviderUserId(
            LoginProvider provider,
            String providerUserId
    );

    Optional<UserSocialAccount> findByUserIdAndProvider(Long userId, LoginProvider provider);

    List<UserSocialAccount> findAllByUserId(Long userId);

    long countByUserId(Long userId);

    void deleteByUserId(Long userId);
}
