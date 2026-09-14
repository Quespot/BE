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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 앱 기동 시 퀘스티 꾸미기 마스터 데이터를 시드한다.
// 슬롯 제한: 카테고리별로 없는 것만 추가.
// 아이템: 코드 기준으로 없으면 추가, 있으면 정의가 다를 때 갱신, 목록에서 빠진 활성 아이템은
//         비활성화(#62, RewardMasterDataSeeder.seedBadges와 같은 패턴). image_url은 관리하지 않는다.
@Component
@RequiredArgsConstructor
public class ItemMasterDataSeeder implements CommandLineRunner {

    private final EquipSlotLimitRepository equipSlotLimitRepository;
    private final ShopItemRepository shopItemRepository;

    record ShopItemDefinition(
            String code,
            String name,
            ItemCategory category,
            ItemRarity rarity,
            int price,
            boolean isDefault,
            int sortOrder
    ) {
        ShopItem toEntity() {
            return ShopItem.seed(code, name, category, rarity, price, isDefault, false, sortOrder);
        }
    }

    // 기본 보유 7종(가격 0, is_default) + 상점 배경 5종(#62). 프론트 화면과 1:1.
    // is_featured(카드 별표)는 아직 쓰는 화면이 없어 정의에 두지 않고 시더가 전부 false로 고정한다.
    static final List<ShopItemDefinition> SHOP_ITEMS = List.of(
            new ShopItemDefinition("EXPLORER_HAT", "탐험가 모자", ItemCategory.HAT, ItemRarity.NORMAL, 0, true, 1),
            new ShopItemDefinition("PURPLE_SUNGLASSES", "보라 선글라스", ItemCategory.ACCESSORY, ItemRarity.NORMAL, 0, true, 2),
            new ShopItemDefinition("RED_SCARF", "빨간 스카프", ItemCategory.ACCESSORY, ItemRarity.NORMAL, 0, true, 3),
            new ShopItemDefinition("YELLOW_RAINCOAT", "노란 우비", ItemCategory.OUTFIT, ItemRarity.NORMAL, 0, true, 4),
            new ShopItemDefinition("EXPLORER_VEST", "탐험 조끼", ItemCategory.OUTFIT, ItemRarity.NORMAL, 0, true, 5),
            new ShopItemDefinition("TRAVEL_BAG", "여행 가방", ItemCategory.ITEM, ItemRarity.NORMAL, 0, true, 6),
            new ShopItemDefinition("EXPLORER_MAGNIFIER", "탐험 돋보기", ItemCategory.ITEM, ItemRarity.NORMAL, 0, true, 7),
            new ShopItemDefinition("COZY_CAFE", "포근한 카페", ItemCategory.BACKGROUND, ItemRarity.NORMAL, 320, false, 11),
            new ShopItemDefinition("SUNNY_CLASSROOM", "햇살 교실", ItemCategory.BACKGROUND, ItemRarity.NORMAL, 320, false, 12),
            new ShopItemDefinition("COZY_ROOM", "포근한 방", ItemCategory.BACKGROUND, ItemRarity.NORMAL, 280, false, 13),
            new ShopItemDefinition("BEACH_CAMPING", "바닷가 캠핑", ItemCategory.BACKGROUND, ItemRarity.NORMAL, 360, false, 14),
            new ShopItemDefinition("STARLIGHT_OBSERVATORY", "별빛 관측소", ItemCategory.BACKGROUND, ItemRarity.NORMAL, 280, false, 15)
    );

    @Override
    @Transactional
    public void run(String... args) {
        seedEquipSlotLimits();
        seedShopItems();
    }

    private void seedEquipSlotLimits() {
        List<EquipSlotLimit> missing = Stream.of(
                        EquipSlotLimit.seed(ItemCategory.HAT, 1, 1),
                        EquipSlotLimit.seed(ItemCategory.ACCESSORY, 2, 2),
                        EquipSlotLimit.seed(ItemCategory.OUTFIT, 1, 3),
                        EquipSlotLimit.seed(ItemCategory.ITEM, 1, 4),
                        EquipSlotLimit.seed(ItemCategory.BACKGROUND, 1, 5)
                )
                .filter(limit -> !equipSlotLimitRepository.existsById(limit.getCategory()))
                .toList();

        if (!missing.isEmpty()) {
            equipSlotLimitRepository.saveAll(missing);
        }
    }

    private void seedShopItems() {
        List<ShopItem> missing = new ArrayList<>();
        for (ShopItemDefinition def : SHOP_ITEMS) {
            Optional<ShopItem> existing = shopItemRepository.findByCode(def.code());
            if (existing.isEmpty()) {
                missing.add(def.toEntity());
            } else if (needsUpdate(existing.get(), def)) {
                existing.get().updateMaster(def.name(), def.category(), def.rarity(), def.price(),
                        def.isDefault(), false, def.sortOrder());
            }
        }
        if (!missing.isEmpty()) {
            shopItemRepository.saveAll(missing);
        }

        Set<String> knownCodes = SHOP_ITEMS.stream().map(ShopItemDefinition::code).collect(Collectors.toSet());
        shopItemRepository.findByIsActiveTrue().stream()
                .filter(item -> !knownCodes.contains(item.getCode()))
                .forEach(ShopItem::deactivate);
    }

    private boolean needsUpdate(ShopItem item, ShopItemDefinition def) {
        return !def.name().equals(item.getName())
                || def.category() != item.getCategory()
                || def.rarity() != item.getRarity()
                || def.price() != item.getPrice()
                || def.isDefault() != item.getIsDefault()
                || Boolean.TRUE.equals(item.getIsFeatured())
                || def.sortOrder() != item.getSortOrder()
                || !Boolean.TRUE.equals(item.getIsActive());
    }
}
