package com.adoleiiiiii.immortality.effect;

import com.adoleiiiiii.immortality.util.ImmortalityPenaltyHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;

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
		// 不屈效果是被动触发的，不需要每 tick 更新
	}

	@Override
	public void onApplied(LivingEntity entity, AttributeContainer attributes, int amplifier) {
		super.onApplied(entity, attributes, amplifier);

		if (entity instanceof PlayerEntity player) {
			ImmortalityPenaltyHandler.handleEffectApplied(player);
		}
	}

	@Override
	public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
		super.onRemoved(entity, attributes, amplifier);

		if (entity instanceof PlayerEntity player) {
			ImmortalityPenaltyHandler.handleEffectEnd(player);
		}
	}
}
