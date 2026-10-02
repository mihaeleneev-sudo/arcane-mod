package com.arcane.mixin;
import com.arcane.Form; import com.arcane.Forms;
import net.minecraft.entity.LivingEntity; import net.minecraft.entity.damage.DamageSource; import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.injection.At; import org.spongepowered.asm.mixin.injection.ModifyVariable;
@Mixin(LivingEntity.class) public abstract class LivingEntityMixin {
 @ModifyVariable(method="damage",at=@At("HEAD"),argsOnly=true,require=0)
 private float arcane$jesterDamage(float amount,DamageSource source){LivingEntity self=(LivingEntity)(Object)this;if(!(source.getAttacker() instanceof ServerPlayerEntity attacker))return amount;if(attacker==self||!Forms.isActive(attacker,Form.JESTER))return amount;return Forms.rollDamage(attacker,self,amount);}
}
