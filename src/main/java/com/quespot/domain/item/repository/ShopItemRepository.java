package com.quespot.domain.item.repository;

import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.enums.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {

    // 상점 목록. 기본 보유 아이템(is_default)은 지급 대상이지 판매 대상이 아니라 뺀다(#62).
    List<ShopItem> findByIsActiveTrueAndIsDefaultFalseOrderBySortOrderAsc();

    List<ShopItem> findByCategoryAndIsActiveTrueAndIsDefaultFalseOrderBySortOrderAsc(ItemCategory category);

    Optional<ShopItem> findByCode(String code);

    List<ShopItem> findByIsActiveTrue();

    Optional<ShopItem> findByIdAndIsActiveTrue(Long id);
}
