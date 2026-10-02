package com.arcane;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Катана Саира. ПКМ - разрез воздуха (только в форме Саира). */
public class KatanaItem extends SwordItem {
    public static final double SLASH_RANGE = 7.0;
    public static final float SLASH_DAMAGE = 8.0f;
    public static final int COOLDOWN_TICKS = 30;

    public KatanaItem() {
        super(ToolMaterials.NETHERITE, 3, -2.0f,
                new FabricItemSettings().maxCount(1).rarity(Rarity.RARE).fireproof());
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!Forms.isActive(user, Form.SAIR)) {
            if (!world.isClient) user.sendMessage(Text.translatable("message.arcane.not_sair"), true);
            return TypedActionResult.fail(stack);
        }
        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            Forms.airSlash(sp, SLASH_RANGE, SLASH_DAMAGE, null);
            user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        }
        user.swingHand(hand);
        return TypedActionResult.success(stack, world.isClient());
    }
}
