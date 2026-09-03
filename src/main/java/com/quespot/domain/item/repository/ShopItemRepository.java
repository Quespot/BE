package com.quespot.domain.item.repository;

import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.enums.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {

    List<ShopItem> findByIsActiveTrueOrderBySortOrderAsc();

    List<ShopItem> findByCategoryAndIsActiveTrueOrderBySortOrderAsc(ItemCategory category);

    boolean existsByCode(String code);
}
