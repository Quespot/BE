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
            Boolean isFeatured,
            Integer sortOrder
    ) {
        validatePrice(price);
        this.code = code;
        this.name = name;
        this.category = category;
        this.rarity = rarity;
        this.price = price;
        this.isDefault = isDefault;
        this.isFeatured = isFeatured;
        this.isActive = true;
        this.sortOrder = sortOrder;
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
        return seed(code, name, category, rarity, price, isDefault, isFeatured, 0);
    }

    public static ShopItem seed(
            String code,
            String name,
            ItemCategory category,
            ItemRarity rarity,
            Integer price,
            Boolean isDefault,
            Boolean isFeatured,
            Integer sortOrder
    ) {
        return new ShopItem(code, name, category, rarity, price, isDefault, isFeatured, sortOrder);
    }

    // 마스터 정의가 바뀌었을 때 시더가 호출한다. 비활성이었던 아이템이 정의에 다시 들어오면 활성으로 되돌린다.
    // image_url은 시더가 관리하지 않으므로 여기서 건드리지 않는다.
    public void updateMaster(
            String name,
            ItemCategory category,
            ItemRarity rarity,
            Integer price,
            Boolean isDefault,
            Boolean isFeatured,
            Integer sortOrder
    ) {
        validatePrice(price);
        this.name = name;
        this.category = category;
        this.rarity = rarity;
        this.price = price;
        this.isDefault = isDefault;
        this.isFeatured = isFeatured;
        this.sortOrder = sortOrder;
        this.isActive = true;
    }

    // 정의 목록에서 빠진 아이템. 행은 남긴다(보유 이력 보존). 판매·지급·보유 목록·슬롯 계산에서 모두 빠진다.
    // user_items.is_equipped는 건드리지 않으므로, 비활성 아이템을 나중에 정의에 되살리면 장착 상태로
    // 다시 나타나 슬롯 한도를 넘길 수 있다 — 되살릴 땐 해당 아이템의 장착 행을 먼저 해제할 것.
    public void deactivate() {
        this.isActive = false;
    }

    // 음수 가격은 차감 없이 지급되는 경로가 된다(ItemPurchaseService는 price > 0일 때만 차감).
    // DB CHECK는 ddl-auto: update가 기존 테이블에 안 붙이므로 Flyway 도입 때 같이 건다.
    private static void validatePrice(Integer price) {
        if (price == null || price < 0) {
            throw new IllegalArgumentException("아이템 가격은 0 이상이어야 합니다: " + price);
        }
    }
}
