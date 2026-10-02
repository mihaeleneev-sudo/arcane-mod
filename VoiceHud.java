package com.arcane.mixin;
import com.arcane.Forms; import net.minecraft.entity.player.PlayerEntity; import net.minecraft.item.ItemStack; import net.minecraft.server.network.ServerPlayerEntity; import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.injection.At; import org.spongepowered.asm.mixin.injection.Inject; import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemStack.class) public class ItemStackMixin {
 @Inject(method="onCraft",at=@At("HEAD"),require=0) private void arcane$jesterCraft(World world,PlayerEntity player,int amount,CallbackInfo ci){if(!world.isClient&&player instanceof ServerPlayerEntity sp)Forms.onCraft(sp,(ItemStack)(Object)this);}
}
