package com.arcane;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Логика формы "Саир". Состояние хранится в командных тегах игрока (сохраняется в мире). */
public final class SairForm {
    public static final String TAG = "arcane_sair";

    private SairForm() {}

    public static boolean isSair(PlayerEntity p) {
        return p.getCommandTags().contains(TAG);
    }

    /** @return false, если игрок уже в форме Саира. */
    public static boolean activate(ServerPlayerEntity p) {
        if (isSair(p)) return false;
        p.addCommandTag(TAG);

        ServerWorld w = p.getServerWorld();
        w.spawnParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 120, 0.5, 1.0, 0.5, 0.8);
        w.spawnParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1.0, 0.6, 0.5);
        w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 0.7f);

        p.getInventory().offerOrDrop(new ItemStack(ModItems.SAIR_KATANA));
        applyEffects(p);
        p.sendMessage(Text.translatable("message.arcane.transformed"), false);
        return true;
    }

    public static void revert(ServerPlayerEntity p) {
           p.removeScoreboardTag(TAG);
        p.removeStatusEffect(StatusEffects.SPEED);
        p.removeStatusEffect(StatusEffects.STRENGTH);
        p.sendMessage(Text.translatable("message.arcane.reverted"), false);
    }

    /** Пассивные бонусы формы. Вызывается каждые 2 секунды. */
    public static void applyEffects(ServerPlayerEntity p) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 120, 1, true, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 120, 0, true, false, true));
    }

    /**
     * Разрез воздуха: поражает всех в конусе перед игроком.
     * @param exclude цель, которую не нужно бить повторно (при обычном ударе), или null
     */
    public static void airSlash(ServerPlayerEntity p, double range, float damage, Entity exclude) {
        ServerWorld w = p.getServerWorld();
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1.0f).normalize();

        Box box = p.getBoundingBox().stretch(look.multiply(range)).expand(range * 0.5, 1.5, range * 0.5);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> x != p && x != exclude && x.isAlive())) {
            Vec3d to = e.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > range || dist < 0.01) continue;
            if (to.normalize().dotProduct(look) < 0.6) continue; // угол около 53 градусов
            e.damage(p.getDamageSources().playerAttack(p), damage);
            e.takeKnockback(0.5, -look.x, -look.z);
        }

        // визуальный след "разреза"
        for (int i = 1; i <= (int) range; i++) {
            Vec3d pt = eye.add(look.multiply(i));
            w.spawnParticles(ParticleTypes.SWEEP_ATTACK, pt.x, pt.y - 0.3, pt.z, 1, 0, 0, 0, 0);
            w.spawnParticles(ParticleTypes.CRIT, pt.x, pt.y - 0.3, pt.z, 3, 0.3, 0.3, 0.3, 0.1);
        }
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2f, 1.3f);
    }
}
