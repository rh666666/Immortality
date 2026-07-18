package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.damage.ImmortalityDamageTypes;
import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.handler.TruePowerHandler;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import com.adoleiiiiii.immortality.util.ImmortalityDamageHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 在玩家拥有不屈 buff 且已触发过死亡抵抗时，按公式减免所受伤害；
 * 攻击者拥有「真正的力量」时，将伤害设为最大值并注册多层击杀。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

	/**
	 * 根据不屈 buff 期间的死亡次数，缩放即将结算的伤害值。
	 * 攻击者拥有「真正的力量」时，伤害设为 {@link Float#MAX_VALUE}。
	 */
	@ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
	private float immortality$modifyIncomingDamage(float amount, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;

		// ── 攻击者持有 TRUE_POWER：伤害 MAX_VALUE + 注册击杀 ──
		if (source.getAttacker() instanceof PlayerEntity attacker
				&& attacker.hasStatusEffect(ModEffects.TRUE_POWER)) {
			TruePowerHandler.registerKill(attacker, self);
			return Float.MAX_VALUE;
		}

		// ── 不屈减伤（仅玩家自己受到的伤害） ──
		if (!(self instanceof PlayerEntity player)) {
			return amount;
		}
		if (source.isOf(ImmortalityDamageTypes.BURN_OUT)) {
			return amount;
		}
		if (!player.hasStatusEffect(ModEffects.IMMORTALITY)) {
			return amount;
		}

		ImmortalityPlayerAccess access = (ImmortalityPlayerAccess) player;
		if (access.immortality$isEffectEndSettled()) {
			return amount;
		}
		int deathCount = access.immortality$getDeathCount();
		if (deathCount <= 0) {
			return amount;
		}

		float multiplier = ImmortalityDamageHelper.computeDamageMultiplier(deathCount, player.getMaxHealth());
		return amount * multiplier;
	}
}
