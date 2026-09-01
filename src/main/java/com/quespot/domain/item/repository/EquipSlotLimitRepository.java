package com.quespot.domain.item.repository;

import com.quespot.domain.item.entity.EquipSlotLimit;
import com.quespot.domain.item.enums.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipSlotLimitRepository extends JpaRepository<EquipSlotLimit, ItemCategory> {
}
