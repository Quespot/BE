package com.quespot.domain.item.repository;

import com.quespot.domain.item.entity.UserItem;
import com.quespot.domain.item.enums.ItemCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    @Query("select ui from UserItem ui join fetch ui.item where ui.userId = :userId order by ui.purchasedAt desc")
    List<UserItem> findAllByUserIdWithItem(@Param("userId") Long userId);

    Optional<UserItem> findByUserIdAndItem_Id(Long userId, Long itemId);

    // 같은 유저·카테고리의 UserItem 전체를 잠가 장착 슬롯 한도 위반을 막는다 (동시 장착 요청 대비)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ui from UserItem ui where ui.userId = :userId and ui.item.category = :category")
    List<UserItem> lockAllByUserIdAndItem_Category(@Param("userId") Long userId, @Param("category") ItemCategory category);
}
