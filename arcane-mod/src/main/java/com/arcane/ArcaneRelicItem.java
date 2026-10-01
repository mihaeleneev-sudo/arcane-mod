package com.arcane;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Осколок (расходуется) и Кристалл (не расходуется) Аркана. ПКМ - стать Саиром. */
public class ArcaneRelicItem extends Item {
    private final boolean consumable;

    public ArcaneRelicItem(Settings settings, boolean consumable) {
        super(settings);
        this.consumable = consumable;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);

        if (user instanceof ServerPlayerEntity sp) {
            if (SairForm.activate(sp)) {
                if (consumable && !sp.getAbilities().creativeMode) stack.decrement(1);
            } else {
                sp.sendMessage(Text.translatable("message.arcane.already_sair"), true);
                return TypedActionResult.fail(stack);
            }
        }
        return TypedActionResult.success(stack);
    }
}
