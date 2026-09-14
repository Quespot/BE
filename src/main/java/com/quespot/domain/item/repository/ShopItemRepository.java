package com.quespot.domain.item.repository;

import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.enums.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {

    List<ShopItem> findByIsActiveTrueOrderBySortOrderAsc();

    List<ShopItem> findByCategoryAndIsActiveTrueOrderBySortOrderAsc(ItemCategory category);

    Optional<ShopItem> findByCode(String code);

    List<ShopItem> findByIsActiveTrue();

    Optional<ShopItem> findByIdAndIsActiveTrue(Long id);
}
