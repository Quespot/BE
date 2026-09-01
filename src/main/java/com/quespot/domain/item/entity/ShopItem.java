package com.quespot.domain.item.entity;

import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.enums.ItemRarity;
import com.quespot.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "shop_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_items_code", columnNames = "code")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShopItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private ItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", nullable = false, length = 20)
    private ItemRarity rarity;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    private ShopItem(
            String code,
            String name,
            ItemCategory category,
            ItemRarity rarity,
            Integer price,
            Boolean isDefault,
            Boolean isFeatured
    ) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.rarity = rarity;
        this.price = price;
        this.isDefault = isDefault;
        this.isFeatured = isFeatured;
        this.isActive = true;
        this.sortOrder = 0;
    }

    public static ShopItem seed(
            String code,
            String name,
            ItemCategory category,
            ItemRarity rarity,
            Integer price,
            Boolean isDefault,
            Boolean isFeatured
    ) {
        return new ShopItem(code, name, category, rarity, price, isDefault, isFeatured);
    }
}
