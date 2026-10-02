package com.arcane;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Предмет, дающий игроку силу (персонажа). ПКМ - получить силу. Дальше превращение по Alt. */
public class CharacterItem extends Item {
    private final Form form;
    private final boolean consumable;

    public CharacterItem(Settings settings, Form form, boolean consumable) {
        super(settings);
        this.form = form;
        this.consumable = consumable;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);

        if (user instanceof ServerPlayerEntity sp) {
            if (Forms.character(sp) == form) {
                sp.sendMessage(Text.translatable("message.arcane.already_has"), true);
                return TypedActionResult.fail(stack);
            }
            Forms.setCharacter(sp, form);
            if (consumable && !sp.getAbilities().creativeMode) stack.decrement(1);
        }
        return TypedActionResult.success(stack);
    }
}
