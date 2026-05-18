package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.util.ImmortalityPenaltyHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 removeStatusEffect 成功移除不屈时再次尝试结算惩罚（兜底，防止 onRemoved 未触发）。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityRemoveEffectMixin {

	@Inject(method = "removeStatusEffect", at = @At("RETURN"))
	private void immortality$onRemoveStatusEffect(StatusEffect effect, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() || effect != ModEffects.IMMORTALITY) {
			return;
		}
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof PlayerEntity player) {
			ImmortalityPenaltyHandler.handleEffectEnd(player);
		}
	}
}
