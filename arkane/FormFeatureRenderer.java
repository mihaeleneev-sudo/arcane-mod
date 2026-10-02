package com.arcane;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public final class ModItems {
    public static final Item ARCANE_SHARD = register("arcane_shard",
            new CharacterItem(new FabricItemSettings().maxCount(16).rarity(Rarity.RARE), Form.SAIR, true));
    public static final Item ARCANE_CRYSTAL = register("arcane_crystal",
            new CharacterItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC).fireproof(), Form.SAIR, false));
    public static final Item EIRO_HEART = register("eiro_heart",
            new CharacterItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC).fireproof(), Form.EIRO, true));
    public static final Item FOOL_CARD = register("fool_card",
            new CharacterItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC).fireproof(), Form.JESTER, true));
    public static final Item SAIR_KATANA = register("sair_katana", new KatanaItem());

    private ModItems() {}

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(ArcaneMod.MOD_ID, name), item);
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(e -> {
            e.add(ARCANE_SHARD);
            e.add(ARCANE_CRYSTAL);
            e.add(EIRO_HEART);
            e.add(FOOL_CARD);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(e -> e.add(SAIR_KATANA));
    }
}
