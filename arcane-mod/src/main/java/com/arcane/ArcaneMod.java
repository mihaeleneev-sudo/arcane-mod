package com.arcane;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class ArcaneMod implements ModInitializer {
    public static final String MOD_ID = "arcane";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Сундуки, где может найтись осколок. */
    private static final Set<Identifier> SHARD_TABLES = Set.of(
            LootTables.SIMPLE_DUNGEON_CHEST,
            LootTables.ABANDONED_MINESHAFT_CHEST,
            LootTables.STRONGHOLD_CORRIDOR_CHEST,
            LootTables.STRONGHOLD_CROSSING_CHEST,
            LootTables.STRONGHOLD_LIBRARY_CHEST,
            LootTables.DESERT_PYRAMID_CHEST,
            LootTables.JUNGLE_TEMPLE_CHEST,
            LootTables.ANCIENT_CITY_CHEST,
            LootTables.WOODLAND_MANSION_CHEST
    );

    @Override
    public void onInitialize() {
        ArcConfig.load();
        ModItems.init();

        // 1) Осколок Аркана с малым шансом в подземных сундуках
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (source.isBuiltin() && SHARD_TABLES.contains(id)) {
                tableBuilder.pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(ArcConfig.shardChance))
                        .with(ItemEntry.builder(ModItems.ARCANE_SHARD)));
            }
        });

        // 2) Убийство легенды Арка другим игроком -> выпадает Кристалл Аркана
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayerEntity victim)) return;
            if (!victim.getGameProfile().getName().equalsIgnoreCase(ArcConfig.legendName)) return;
            if (!(damageSource.getAttacker() instanceof ServerPlayerEntity killer) || killer == victim) return;

            ItemScatterer.spawn(victim.getServerWorld(), victim.getX(), victim.getY(), victim.getZ(),
                    new ItemStack(ModItems.ARCANE_CRYSTAL));
            victim.getServer().getPlayerManager().broadcast(
                    Text.translatable("message.arcane.legend_fell", killer.getDisplayName(), victim.getDisplayName()),
                    false);
        });

        // 3) Обычный удар катаной в форме Саира бьёт по площади
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp
                    && SairForm.isSair(sp) && sp.getMainHandStack().getItem() instanceof KatanaItem) {
                SairForm.airSlash(sp, 4.5, 5.0f, entity);
            }
            return ActionResult.PASS;
        });

        // 4) Форма сохраняется после смерти, бонусы обновляются
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (SairForm.isSair(oldPlayer)) newPlayer.addCommandTag(SairForm.TAG);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 40 != 0) return;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (SairForm.isSair(p)) SairForm.applyEffects(p);
            }
        });

        // 5) /arcane revert - выйти из формы
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("arcane")
                        .then(CommandManager.literal("revert").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            SairForm.revert(p);
                            return Command.SINGLE_SUCCESS;
                        }))));

        LOGGER.info("Arcane загружен. Легенда: {}", ArcConfig.legendName);
    }
}
