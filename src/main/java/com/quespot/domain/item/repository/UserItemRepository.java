package com.quespot.domain.item.repository;

import com.quespot.domain.item.entity.UserItem;
import com.quespot.domain.item.enums.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    List<UserItem> findByUserIdOrderByPurchasedAtDesc(Long userId);

    Optional<UserItem> findByUserIdAndItem_Id(Long userId, Long itemId);

    List<UserItem> findByUserIdAndItem_CategoryAndIsEquippedTrueOrderByEquippedAtAsc(
            Long userId, ItemCategory category
    );
}
