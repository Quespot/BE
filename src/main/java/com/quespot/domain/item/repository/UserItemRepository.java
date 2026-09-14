package com.quespot.domain.item.repository;

import com.quespot.domain.item.entity.UserItem;
import com.quespot.domain.item.enums.ItemCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    // 비활성 아이템은 보유 목록·퀘스티에서 뺀다(행은 남긴다, #62).
    @Query("""
            select ui from UserItem ui
              join fetch ui.item i
            where ui.userId = :userId
              and i.isActive = true
            order by ui.purchasedAt desc
            """)
    List<UserItem> findAllByUserIdWithItem(@Param("userId") Long userId);

    Optional<UserItem> findByUserIdAndItem_Id(Long userId, Long itemId);

    // 장착 대상 조회. 비활성 아이템은 "보유하지 않음"으로 취급한다.
    Optional<UserItem> findByUserIdAndItem_IdAndItem_IsActiveTrue(Long userId, Long itemId);

    boolean existsByUserIdAndItem_Id(Long userId, Long itemId);

    // 같은 유저·카테고리의 UserItem 전체를 잠가 장착 슬롯 한도 위반을 막는다 (동시 장착 요청 대비).
    // 비활성 아이템은 슬롯을 차지하지 않는다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ui from UserItem ui
            where ui.userId = :userId
              and ui.item.category = :category
              and ui.item.isActive = true
            """)
    List<UserItem> lockAllByUserIdAndItem_Category(@Param("userId") Long userId, @Param("category") ItemCategory category);

    // 활성 기본 아이템 중 미보유분을 한 번에 지급한다(#62). UNIQUE(user_id, item_id) +
    // ON DUPLICATE KEY UPDATE id = id로 멱등 — 동시 호출도 예외 없이 no-op. 같은 트랜잭션에서
    // UNIQUE 위반을 catch하면 rollback-only가 되므로(CLAUDE.md) 예외 경로 자체를 없앤다.
    // 가격 0이라 포인트 원장·활동 기록은 남기지 않는다.
    // INSERT ... SELECT에서는 ON DUPLICATE KEY UPDATE의 id가 SELECT 쪽 테이블과 모호해지므로
    // 테이블명으로 한정한다(실DB에서 "Column 'id' in field list is ambiguous" 확인).
    @Modifying
    @Query(value = """
            INSERT INTO user_items (user_id, item_id, is_equipped, purchased_at, created_at, updated_at)
            SELECT :userId, si.id, 0, NOW(), NOW(), NOW()
              FROM shop_items si
             WHERE si.is_default = 1
               AND si.is_active = 1
            ON DUPLICATE KEY UPDATE user_items.id = user_items.id
            """, nativeQuery = true)
    int grantDefaultItems(@Param("userId") Long userId);
}
