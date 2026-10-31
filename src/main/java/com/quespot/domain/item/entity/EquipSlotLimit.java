package com.quespot.domain.item.entity;

import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "equip_slot_limits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipSlotLimit extends BaseEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private ItemCategory category;

    @Column(name = "max_equip", nullable = false)
    private Integer maxEquip;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    private EquipSlotLimit(ItemCategory category, Integer maxEquip, Integer sortOrder) {
        this.category = category;
        this.maxEquip = maxEquip;
        this.sortOrder = sortOrder;
    }

    public static EquipSlotLimit seed(ItemCategory category, Integer maxEquip, Integer sortOrder) {
        return new EquipSlotLimit(category, maxEquip, sortOrder);
    }
}
