package com.quespot.domain.item.entity;

import com.quespot.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_item", columnNames = {"user_id", "item_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ShopItem item;

    @Column(name = "is_equipped", nullable = false)
    private Boolean isEquipped;

    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt;

    @Column(name = "equipped_at")
    private LocalDateTime equippedAt;

    private UserItem(Long userId, ShopItem item) {
        this.userId = userId;
        this.item = item;
        this.isEquipped = false;
        this.purchasedAt = LocalDateTime.now();
    }

    public static UserItem acquire(Long userId, ShopItem item) {
        return new UserItem(userId, item);
    }

    public void equip() {
        this.isEquipped = true;
        this.equippedAt = LocalDateTime.now();
    }

    public void unequip() {
        this.isEquipped = false;
    }
}
