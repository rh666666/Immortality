package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import com.adoleiiiiii.immortality.util.ImmortalityDamageHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 在玩家拥有不屈 buff 且已触发过死亡抵抗时，按公式减免所受伤害。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

	/**
	 * 根据不屈 buff 期间的死亡次数，缩放即将结算的伤害值。
	 */
	@ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
	private float immortality$modifyIncomingDamage(float amount, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof PlayerEntity player)) {
			return amount;
		}
		if (!player.hasStatusEffect(ModEffects.IMMORTALITY)) {
			return amount;
		}

		ImmortalityPlayerAccess access = (ImmortalityPlayerAccess) player;
		int deathCount = access.immortality$getDeathCount();
		if (deathCount <= 0) {
			return amount;
		}

		float multiplier = ImmortalityDamageHelper.computeDamageMultiplier(deathCount, player.getMaxHealth());
		return amount * multiplier;
	}
}
