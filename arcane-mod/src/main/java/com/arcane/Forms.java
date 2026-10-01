package com.arcane;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Серверная логика персонажей и форм.
 * "Персонаж" (arcane_char_*) - какая сила у игрока. "Форма" (arcane_on_*) - включено ли превращение сейчас.
 * Всё хранится в командных тегах игрока, поэтому сохраняется вместе с миром.
 */
public final class Forms {
    public static final int COOLDOWN_TICKS = 30 * 20;
    public static final double EIRO_RADIUS = 5.0;
    private static final int AURA_TICKS = 60;

    private static final Map<UUID, Integer> COOLDOWN_UNTIL = new HashMap<>();
    private static final Map<UUID, Integer> AURA = new HashMap<>();

    private Forms() {}

    private static String charTag(Form f) { return "arcane_char_" + f.key; }
    private static String onTag(Form f) { return "arcane_on_" + f.key; }

    // ---------- состояние ----------

    public static Form character(PlayerEntity p) {
        if (p.getCommandTags().contains(charTag(Form.SAIR))) return Form.SAIR;
        if (p.getCommandTags().contains(charTag(Form.EIRO))) return Form.EIRO;
        return Form.NONE;
    }

    public static Form active(PlayerEntity p) {
        if (p.getCommandTags().contains(onTag(Form.SAIR))) return Form.SAIR;
        if (p.getCommandTags().contains(onTag(Form.EIRO))) return Form.EIRO;
        return Form.NONE;
    }

    public static boolean isActive(PlayerEntity p, Form f) {
        return p.getCommandTags().contains(onTag(f));
    }

    public static void setCharacter(ServerPlayerEntity p, Form f) {
        deactivate(p, false);
        p.removeScoreboardTag(charTag(Form.SAIR));
        p.removeScoreboardTag(charTag(Form.EIRO));
        if (f != Form.NONE) {
            p.addCommandTag(charTag(f));
            p.sendMessage(Text.translatable("message.arcane.unlocked_" + f.key), false);
        } else {
            p.sendMessage(Text.translatable("message.arcane.power_removed"), false);
        }
    }

    public static void restoreCharacter(ServerPlayerEntity oldP, ServerPlayerEntity newP) {
        Form f = character(oldP);
        if (f != Form.NONE) newP.addCommandTag(charTag(f));
    }

    // ---------- превращение ----------

    /** Нажатие Alt. */
    public static void toggle(ServerPlayerEntity p) {
        int now = p.getServer().getTicks();

        if (active(p) != Form.NONE) {
            deactivate(p, true);
            COOLDOWN_UNTIL.put(p.getUuid(), now + COOLDOWN_TICKS);
            return;
        }

        Form ch = character(p);
        if (ch == Form.NONE) {
            p.sendMessage(Text.translatable("message.arcane.no_power"), true);
            return;
        }

        int until = COOLDOWN_UNTIL.getOrDefault(p.getUuid(), 0);
        if (now < until) {
            int seconds = (until - now + 19) / 20;
            p.sendMessage(Text.translatable("message.arcane.cooldown", seconds), true);
            return;
        }
        activate(p, ch);
    }

    private static void activate(ServerPlayerEntity p, Form f) {
        p.addCommandTag(onTag(f));
        ServerWorld w = p.getServerWorld();

        if (f == Form.SAIR) {
            w.spawnParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 120, 0.5, 1.0, 0.5, 0.8);
            w.spawnParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1.0, 0.6, 0.5);
            w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 0.7f);
            p.getInventory().offerOrDrop(new ItemStack(ModItems.SAIR_KATANA));
        } else if (f == Form.EIRO) {
            AURA.put(p.getUuid(), AURA_TICKS);
            w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.5f, 0.6f);
        }

        applyEffects(p);
        broadcast(p.getServer(), p);
        p.sendMessage(Text.translatable("message.arcane.on_" + f.key), false);
    }

    public static void deactivate(ServerPlayerEntity p, boolean notify) {
        Form was = active(p);
        if (was == Form.NONE) return;

        p.removeScoreboardTag(onTag(was));
        AURA.remove(p.getUuid());
        p.removeStatusEffect(StatusEffects.SPEED);
        p.removeStatusEffect(StatusEffects.STRENGTH);
        p.removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
        removeKatana(p);
        broadcast(p.getServer(), p);
        if (notify) p.sendMessage(Text.translatable("message.arcane.off"), false);
    }

    private static void removeKatana(ServerPlayerEntity p) {
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(ModItems.SAIR_KATANA)) inv.setStack(i, ItemStack.EMPTY);
        }
    }

    private static void applyEffects(ServerPlayerEntity p) {
        Form f = active(p);
        if (f == Form.SAIR) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 120, 1, true, false, true));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 120, 0, true, false, true));
        } else if (f == Form.EIRO) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 120, 1, true, false, true));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 120, 0, true, false, true));
        }
    }

    // ---------- тик сервера ----------

    public static void tick(MinecraftServer server) {
        int t = server.getTicks();

        if (t % 40 == 0) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (active(p) != Form.NONE) applyEffects(p);
            }
        }

        // голоса: раз в секунду бросаем кубик (в форме чаще, чем без неё)
        if (t % 20 == 0) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (character(p) != Form.EIRO) continue;
                int chance = active(p) == Form.EIRO ? 20 : 120;
                if (p.getRandom().nextInt(chance) == 0) {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeInt(p.getRandom().nextInt(3));
                    ServerPlayNetworking.send(p, Packets.VOICE, buf);
                }
            }
        }

        tickThrown();

        // огненная волна Эйро
        Iterator<Map.Entry<UUID, Integer>> it = AURA.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
            if (p == null || !p.isAlive()) { it.remove(); continue; }

            ServerWorld w = p.getServerWorld();
            // только частицы: блоки не поджигаются
            w.spawnParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.8, p.getZ(), 30, 2.4, 0.8, 2.4, 0.04);
            w.spawnParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 0.8, p.getZ(), 6, 2.4, 0.8, 2.4, 0.02);

            int left = e.getValue();
            if (left % 10 == 0) igniteAround(p);
            if (left <= 1) it.remove(); else e.setValue(left - 1);
        }
    }

    /** Поджигает только живых существ (мобов и игроков), кроме самого носителя. */
    private static void igniteAround(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        Box box = p.getBoundingBox().expand(EIRO_RADIUS);
        double r2 = EIRO_RADIUS * EIRO_RADIUS;
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box,
                x -> x != p && x.isAlive() && x.squaredDistanceTo(p) <= r2)) {
            e.setOnFireFor(4);
        }
    }

    // ---------- способность Эйро: бросок блока ----------

    public static final int THROW_COOLDOWN_TICKS = 6 * 20;
    private static final double THROW_REACH = 6.0;
    private static final float THROW_DAMAGE = 10.0f;
    private static final Map<UUID, Integer> ABILITY_COOLDOWN = new HashMap<>();
    private static final List<Thrown> THROWN = new ArrayList<>();

    private static final class Thrown {
        final FallingBlockEntity entity;
        final ServerWorld world;
        final UUID thrower;
        int age = 0;

        Thrown(FallingBlockEntity entity, ServerWorld world, UUID thrower) {
            this.entity = entity;
            this.world = world;
            this.thrower = thrower;
        }
    }

    /** Нажатие клавиши способности. Эйро хватает блок, на который смотрит, и швыряет его вперёд. */
    public static void useAbility(ServerPlayerEntity p) {
        if (!isActive(p, Form.EIRO)) {
            p.sendMessage(Text.translatable("message.arcane.ability_eiro_only"), true);
            return;
        }
        int now = p.getServer().getTicks();
        int until = ABILITY_COOLDOWN.getOrDefault(p.getUuid(), 0);
        if (now < until) {
            p.sendMessage(Text.translatable("message.arcane.ability_cooldown", (until - now + 19) / 20), true);
            return;
        }

        ServerWorld w = p.getServerWorld();
        HitResult hit = p.raycast(THROW_REACH, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            p.sendMessage(Text.translatable("message.arcane.no_block"), true);
            return;
        }
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockState state = w.getBlockState(pos);
        if (state.isAir() || state.getHardness(w, pos) < 0 || w.getBlockEntity(pos) != null
                || !state.getFluidState().isEmpty()) {
            p.sendMessage(Text.translatable("message.arcane.bad_block"), true);
            return;
        }

        // блок убирается из мира и становится летящим блоком
        FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(w, pos, state);
        Vec3d look = p.getRotationVec(1.0f).normalize();
        Vec3d start = p.getEyePos().add(look.multiply(1.6)).add(0, -0.4, 0);
        fb.setPosition(start.x, start.y, start.z);
        fb.setVelocity(look.multiply(2.0));
        fb.velocityModified = true;

        THROWN.add(new Thrown(fb, w, p.getUuid()));
        ABILITY_COOLDOWN.put(p.getUuid(), now + THROW_COOLDOWN_TICKS);
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_RAVAGER_ATTACK, SoundCategory.PLAYERS, 1.2f, 0.7f);
    }

    /** Следит за летящими блоками: попадание в существо = урон, поджог и раскол блока. */
    private static void tickThrown() {
        Iterator<Thrown> it = THROWN.iterator();
        while (it.hasNext()) {
            Thrown t = it.next();
            FallingBlockEntity fb = t.entity;
            if (fb.isRemoved() || fb.isOnGround() || ++t.age > 200) {
                it.remove();
                continue;
            }
            ServerWorld w = t.world;
            w.spawnParticles(ParticleTypes.FLAME, fb.getX(), fb.getY() + 0.5, fb.getZ(), 3, 0.2, 0.2, 0.2, 0.01);

            List<LivingEntity> hits = w.getEntitiesByClass(LivingEntity.class, fb.getBoundingBox().expand(0.4),
                    e -> e.isAlive() && !e.getUuid().equals(t.thrower));
            if (hits.isEmpty()) continue;

            ServerPlayerEntity thrower = w.getServer().getPlayerManager().getPlayer(t.thrower);
            DamageSource src = thrower != null
                    ? thrower.getDamageSources().playerAttack(thrower)
                    : w.getDamageSources().generic();
            Vec3d dir = fb.getVelocity().normalize();
            for (LivingEntity e : hits) {
                e.damage(src, THROW_DAMAGE);
                e.takeKnockback(0.8, -dir.x, -dir.z);
                e.setOnFireFor(4);
            }
            w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, fb.getBlockState()),
                    fb.getX(), fb.getY() + 0.5, fb.getZ(), 40, 0.4, 0.4, 0.4, 0.15);
            w.spawnParticles(ParticleTypes.EXPLOSION, fb.getX(), fb.getY() + 0.5, fb.getZ(), 1, 0, 0, 0, 0);
            w.playSound(null, fb.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8f, 1.3f);
            fb.discard();
            it.remove();
        }
    }

    // ---------- синхронизация с клиентами ----------

    private static void sendSync(ServerPlayerEntity to, UUID who, int form) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(who);
        buf.writeInt(form);
        ServerPlayNetworking.send(to, Packets.SYNC, buf);
    }

    public static void broadcast(MinecraftServer server, ServerPlayerEntity p) {
        int form = active(p).id;
        for (ServerPlayerEntity to : server.getPlayerManager().getPlayerList()) {
            sendSync(to, p.getUuid(), form);
        }
    }

    public static void syncAllTo(MinecraftServer server, ServerPlayerEntity joiner) {
        for (ServerPlayerEntity o : server.getPlayerManager().getPlayerList()) {
            sendSync(joiner, o.getUuid(), active(o).id);
        }
    }

    // ---------- способность Саира ----------

    /**
     * Разрез воздуха: поражает всех в конусе перед игроком.
     * @param exclude цель, которую не нужно бить повторно (при обычном ударе), или null
     */
    public static void airSlash(ServerPlayerEntity p, double range, float damage, net.minecraft.entity.Entity exclude) {
        ServerWorld w = p.getServerWorld();
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1.0f).normalize();

        Box box = p.getBoundingBox().stretch(look.multiply(range)).expand(range * 0.5, 1.5, range * 0.5);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> x != p && x != exclude && x.isAlive())) {
            Vec3d to = e.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > range || dist < 0.01) continue;
            if (to.normalize().dotProduct(look) < 0.6) continue;
            e.damage(p.getDamageSources().playerAttack(p), damage);
            e.takeKnockback(0.5, -look.x, -look.z);
        }

        for (int i = 1; i <= (int) range; i++) {
            Vec3d pt = eye.add(look.multiply(i));
            w.spawnParticles(ParticleTypes.SWEEP_ATTACK, pt.x, pt.y - 0.3, pt.z, 1, 0, 0, 0, 0);
            w.spawnParticles(ParticleTypes.CRIT, pt.x, pt.y - 0.3, pt.z, 3, 0.3, 0.3, 0.3, 0.1);
        }
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2f, 1.3f);
    }
}
