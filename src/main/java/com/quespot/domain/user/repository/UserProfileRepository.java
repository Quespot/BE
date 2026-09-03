package com.quespot.domain.user.repository;

import com.quespot.domain.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    boolean existsByUserId(Long userId);

    Optional<UserProfile> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
