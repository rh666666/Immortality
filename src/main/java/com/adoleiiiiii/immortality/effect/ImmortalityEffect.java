package com.adoleiiiiii.immortality.effect;

import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import com.adoleiiiiii.immortality.util.ImmortalityPenaltyHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;

import static net.minecraft.SharedConstants.TICKS_PER_MINUTE;

/**
 * 不屈状态效果：buff 期间抵抗死亡并获得叠乘减伤与击退抗性，结束时按死亡次数扣除生命上限。
 */
public class ImmortalityEffect extends StatusEffect {

	public ImmortalityEffect() {
		super(StatusEffectCategory.BENEFICIAL, 0xFFD700);
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return true;
	}

	@Override
	public void applyUpdateEffect(LivingEntity entity, int amplifier) {
		if (!(entity instanceof PlayerEntity player)) return;
		if (!(player instanceof ImmortalityPlayerAccess access)) return;

		StatusEffectInstance effect = player.getStatusEffect(ModEffects.IMMORTALITY);
		if (effect != null) {
			access.immortality$setImmortalityDuration(effect.getDuration());
		}

		if (effect != null && effect.getDuration() <= 2 && access.immortality$isProtected()) {
			access.immortality$setProtected(false);
		}
	}

	@Override
	public void onApplied(LivingEntity entity, AttributeContainer attributes, int amplifier) {
		super.onApplied(entity, attributes, amplifier);

		if (entity instanceof PlayerEntity player) {
			ImmortalityPenaltyHandler.handleEffectApplied(player);

			if (player instanceof ImmortalityPlayerAccess access) {
				access.immortality$setProtected(true);
				StatusEffectInstance effect = player.getStatusEffect(ModEffects.IMMORTALITY);
				if (effect != null) {
					access.immortality$setImmortalityDuration(effect.getDuration());
				}
			}
		}
	}

	@Override
	public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
		super.onRemoved(entity, attributes, amplifier);

		if (!(entity instanceof PlayerEntity player)) return;
		if (!(player instanceof ImmortalityPlayerAccess access)) return;

		if (access.immortality$isRefreshingBuff()) {
			return;
		}

		if (access.immortality$isProtected()) {
			int duration = access.immortality$getImmortalityDuration();
			if (duration <= 0) {
				duration = TICKS_PER_MINUTE;
			}
			player.addStatusEffect(new StatusEffectInstance(
					ModEffects.IMMORTALITY, duration, amplifier, false, false, true));
			return;
		}

		ImmortalityPenaltyHandler.handleEffectEnd(player);
	}
}
