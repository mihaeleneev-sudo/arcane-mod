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
            new ArcaneRelicItem(new FabricItemSettings().maxCount(16).rarity(Rarity.RARE), true));
    public static final Item ARCANE_CRYSTAL = register("arcane_crystal",
            new ArcaneRelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC).fireproof(), false));
    public static final Item SAIR_KATANA = register("sair_katana", new KatanaItem());

    private ModItems() {}

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(ArcaneMod.MOD_ID, name), item);
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(e -> {
            e.add(ARCANE_SHARD);
            e.add(ARCANE_CRYSTAL);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(e -> e.add(SAIR_KATANA));
    }
}
