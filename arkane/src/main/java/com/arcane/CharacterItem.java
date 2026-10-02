package com.arcane;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class ArcaneMod implements ModInitializer {
    public static final String MOD_ID = "arcane";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

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

    /** Данжи Нижнего мира: адская крепость и бастионы. Здесь может найтись Сердце Эйро. */
    private static final Set<Identifier> HEART_TABLES = Set.of(
            LootTables.NETHER_BRIDGE_CHEST,
            LootTables.BASTION_TREASURE_CHEST,
            LootTables.BASTION_OTHER_CHEST,
            LootTables.BASTION_BRIDGE_CHEST,
            LootTables.BASTION_HOGLIN_STABLE_CHEST
    );

    @Override
    public void onInitialize() {
        ArcConfig.load();
        ModItems.init();

        // 1) Осколок Аркана в подземных сундуках, Сердце Эйро в сундуках Нижнего мира
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (source.isBuiltin() && SHARD_TABLES.contains(id)) {
                tableBuilder.pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(ArcConfig.shardChance))
                        .with(ItemEntry.builder(ModItems.ARCANE_SHARD)));
            }
            if (source.isBuiltin() && id.getNamespace().equals("minecraft") && id.getPath().startsWith("chests/village/")) {
                tableBuilder.pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(ArcConfig.jesterCardChance))
                        .with(ItemEntry.builder(ModItems.FOOL_CARD)));
            }
            if (source.isBuiltin() && HEART_TABLES.contains(id)) {
                tableBuilder.pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(ArcConfig.eiroHeartChance))
                        .with(ItemEntry.builder(ModItems.EIRO_HEART)));
            }
        });

        // 2) Смерть игрока: форма сбрасывается; убийство легенды Арка -> Кристалл Аркана
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayerEntity victim)) return;
            Forms.deactivate(victim, false);

            if (!victim.getGameProfile().getName().equalsIgnoreCase(ArcConfig.legendName)) return;
            if (!(damageSource.getAttacker() instanceof ServerPlayerEntity killer) || killer == victim) return;

            ItemScatterer.spawn(victim.getServerWorld(), victim.getX(), victim.getY(), victim.getZ(),
                    new ItemStack(ModItems.ARCANE_CRYSTAL));
            victim.getServer().getPlayerManager().broadcast(
                    Text.translatable("message.arcane.legend_fell", killer.getDisplayName(), victim.getDisplayName()),
                    false);
        });

        // 3) Удары: Саир бьёт катаной по площади, Эйро поджигает цель
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) {
                if (Forms.isActive(sp, Form.SAIR) && sp.getMainHandStack().getItem() instanceof KatanaItem) {
                    Forms.airSlash(sp, 4.5, 5.0f, entity);
                }
                if (Forms.isActive(sp, Form.EIRO) && entity instanceof LivingEntity target) {
                    target.setOnFireFor(4);
                }
            }
            return ActionResult.PASS;
        });

        // 4) Сеть: клавиша превращения и синхронизация внешнего вида
        ServerPlayNetworking.registerGlobalReceiver(Packets.TOGGLE,
                (server, player, handler, buf, responseSender) -> server.execute(() -> Forms.toggle(player)));

        ServerPlayNetworking.registerGlobalReceiver(Packets.ABILITY,
                (server, player, handler, buf, responseSender) -> server.execute(() -> Forms.useAbility(player)));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            Forms.syncAllTo(server, handler.player);
            Forms.broadcast(server, handler.player);
        });

        // 5) Сила сохраняется после смерти, форма - нет
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> Forms.restoreCharacter(oldPlayer, newPlayer));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                Forms.broadcast(newPlayer.getServer(), newPlayer));

        ServerTickEvents.END_SERVER_TICK.register(Forms::tick);

        // 6) /arcane character <sair|eiro|none> - выдать или забрать силу (для операторов)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("arcane")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("character")
                                .then(CommandManager.literal("sair").executes(ctx -> {
                                    Forms.setCharacter(ctx.getSource().getPlayerOrThrow(), Form.SAIR);
                                    return 1;
                                }))
                                .then(CommandManager.literal("eiro").executes(ctx -> {
                                    Forms.setCharacter(ctx.getSource().getPlayerOrThrow(), Form.EIRO);
                                    return 1;
                                }))
                                .then(CommandManager.literal("jester").executes(ctx -> {
                                    Forms.setCharacter(ctx.getSource().getPlayerOrThrow(), Form.JESTER);
                                    return 1;
                                }))
                                .then(CommandManager.literal("none").executes(ctx -> {
                                    Forms.setCharacter(ctx.getSource().getPlayerOrThrow(), Form.NONE);
                                    return 1;
                                })))));

        LOGGER.info("Arcane загружен. Легенда: {}", ArcConfig.legendName);
    }
}
