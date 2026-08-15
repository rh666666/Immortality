package com.adoleiiiiii.immortality.effect;

import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import com.adoleiiiiii.immortality.util.ImmortalityDeathGate;
import com.adoleiiiiii.immortality.util.ImmortalityPenaltyHandler;
import com.adoleiiiiii.immortality.util.ImmortalityTotemHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;

import static net.minecraft.SharedConstants.TICKS_PER_MINUTE;

/**
 * 不屈状态效果：期间原版死亡链路返空；结束时按死亡次数扣除生命上限。
 */
@SuppressWarnings("all")
public class ImmortalityEffect extends MobEffect {

	public ImmortalityEffect() {
		super(MobEffectCategory.BENEFICIAL, 0xFFD700);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		if (!(entity instanceof Player player)) {
			return;
		}
		if (!(player instanceof ImmortalityPlayerAccess access)) {
			return;
		}

		MobEffectInstance effect = player.getEffect(ModEffects.IMMORTALITY_EFFECT);
		if (effect != null) {
			access.immortality$setImmortalityDuration(effect.getDuration());
		}

		if (ImmortalityDeathGate.shouldVoidDeath(player)) {
			ImmortalityTotemHelper.ensureSurviving(player);
		}

		if (effect != null && effect.getDuration() <= 2 && access.immortality$isProtected()) {
			access.immortality$setProtected(false);
		}
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		super.addAttributeModifiers(entity, attributes, amplifier);

		if (entity instanceof Player player) {
			ImmortalityPenaltyHandler.handleEffectApplied(player);

			if (player instanceof ImmortalityPlayerAccess access) {
				access.immortality$setProtected(true);
				MobEffectInstance effect = player.getEffect(ModEffects.IMMORTALITY_EFFECT);
				if (effect != null) {
					access.immortality$setImmortalityDuration(effect.getDuration());
				}
			}
		}
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		super.removeAttributeModifiers(entity, attributes, amplifier);

		if (!(entity instanceof Player player)) {
			return;
		}
		if (!(player instanceof ImmortalityPlayerAccess access)) {
			return;
		}

		if (access.immortality$isRefreshingBuff()) {
			return;
		}

		if (access.immortality$isProtected()) {
			int duration = access.immortality$getImmortalityDuration();
			if (duration <= 0) {
				duration = TICKS_PER_MINUTE;
			}
			player.addEffect(new MobEffectInstance(
					ModEffects.IMMORTALITY_EFFECT, duration, amplifier, false, false, true));
			return;
		}

		ImmortalityPenaltyHandler.handleEffectEnd(player);
	}
}
