package com.quespot.domain.item.config;

import com.quespot.domain.item.entity.EquipSlotLimit;
import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.enums.ItemRarity;
import com.quespot.domain.item.repository.EquipSlotLimitRepository;
import com.quespot.domain.item.repository.ShopItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 앱 기동 시 퀘스티 꾸미기 마스터 데이터를 시드한다. 이미 데이터가 있으면 건너뛴다.
@Component
@RequiredArgsConstructor
public class ItemMasterDataSeeder implements CommandLineRunner {

    private final EquipSlotLimitRepository equipSlotLimitRepository;
    private final ShopItemRepository shopItemRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedEquipSlotLimits();
        seedShopItems();
    }

    private void seedEquipSlotLimits() {
        if (equipSlotLimitRepository.count() > 0) {
            return;
        }

        equipSlotLimitRepository.saveAll(List.of(
                EquipSlotLimit.seed(ItemCategory.HAT, 1, 1),
                EquipSlotLimit.seed(ItemCategory.ACCESSORY, 2, 2),
                EquipSlotLimit.seed(ItemCategory.OUTFIT, 1, 3),
                EquipSlotLimit.seed(ItemCategory.ITEM, 1, 4)
        ));
    }

    private void seedShopItems() {
        if (shopItemRepository.count() > 0) {
            return;
        }

        shopItemRepository.saveAll(List.of(
                ShopItem.seed("EXPLORER_HAT", "탐험가 모자", ItemCategory.HAT, ItemRarity.NORMAL, 0, true, false),
                ShopItem.seed("SUNGLASSES", "선글라스", ItemCategory.ACCESSORY, ItemRarity.NORMAL, 0, true, false),
                ShopItem.seed("BLUE_SCARF", "파란 스카프", ItemCategory.ACCESSORY, ItemRarity.NORMAL, 0, true, false),
                ShopItem.seed("TRAVEL_BAG", "여행 가방", ItemCategory.ITEM, ItemRarity.NORMAL, 0, true, false),
                ShopItem.seed("GOLDEN_CROWN", "황금 왕관", ItemCategory.HAT, ItemRarity.LEGENDARY, 300, false, false),
                ShopItem.seed("TRAVELER_CAP", "여행자 캡", ItemCategory.HAT, ItemRarity.NORMAL, 150, false, false),
                ShopItem.seed("GRADUATION_CAP", "학사모", ItemCategory.HAT, ItemRarity.RARE, 200, false, false)
        ));
    }
}
