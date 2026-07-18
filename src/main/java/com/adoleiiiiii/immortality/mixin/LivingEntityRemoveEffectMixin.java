package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.effect.ImmortalityEffect;
import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 保护不屈和真正的力量不被非自然移除。
 * <p>
 * 对标 Forge 版 {@code PlayerEntityMixin} 中 {@code removeEffect} / {@code removeAllEffects} 的保护逻辑。
 * <ul>
 *   <li>{@code removeStatusEffect} 定向移除 → 拦截（除非 {@code isRefreshingBuff}）。</li>
 *   <li>{@code clearStatusEffects} 批量清除 → {@code protected = true} 时从备份恢复；{@code protected = false}
 *       （牛奶桶已撤销）则允许通过，由 {@link ImmortalityEffect#onRemoved} 结算惩罚。</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityRemoveEffectMixin {

	@Unique
	private StatusEffectInstance immortality$backup;

	@Unique
	private StatusEffectInstance truePower$backup;

	@Inject(method = "removeStatusEffect", at = @At("HEAD"), cancellable = true)
	private void onRemoveStatusEffect(StatusEffect type, CallbackInfoReturnable<Boolean> cir) {
		if (!((LivingEntity) (Object) this instanceof PlayerEntity player)) return;

		if (type == ModEffects.IMMORTALITY) {
			if (player instanceof ImmortalityPlayerAccess access && access.immortality$isRefreshingBuff()) return;
			cir.setReturnValue(true);
			cir.cancel();
			return;
		}
		if (type == ModEffects.TRUE_POWER) {
			cir.setReturnValue(true);
			cir.cancel();
		}
	}

	@Inject(method = "clearStatusEffects", at = @At("HEAD"))
	private void onClearStatusEffectsHead(CallbackInfoReturnable<Boolean> cir) {
		if (((LivingEntity) (Object) this) instanceof PlayerEntity player
				&& player instanceof ImmortalityPlayerAccess access) {
			this.immortality$backup = player.getStatusEffect(ModEffects.IMMORTALITY);
			this.truePower$backup = player.getStatusEffect(ModEffects.TRUE_POWER);
			if (access.immortality$isProtected()) {
				access.immortality$setRefreshingBuff(true);
			}
		}
	}

	@Inject(method = "clearStatusEffects", at = @At("RETURN"))
	private void onClearStatusEffectsReturn(CallbackInfoReturnable<Boolean> cir) {
		if (!(((LivingEntity) (Object) this) instanceof PlayerEntity player)) return;
		if (!(player instanceof ImmortalityPlayerAccess access)) return;

		access.immortality$setRefreshingBuff(false);

		if (access.immortality$isProtected()) {
			if (this.immortality$backup != null && !player.hasStatusEffect(ModEffects.IMMORTALITY)) {
				player.addStatusEffect(this.immortality$backup);
			}
			if (this.truePower$backup != null && !player.hasStatusEffect(ModEffects.TRUE_POWER)) {
				player.addStatusEffect(this.truePower$backup);
			}
		}
		this.immortality$backup = null;
		this.truePower$backup = null;
	}
}
