package com.adoleiiiiii.immortality.effect;

import com.adoleiiiiii.immortality.util.ImmortalityPenaltyHandler;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;

/**
 * 不屈状态效果：buff 期间抵抗死亡并获得叠乘减伤与击退抗性，结束时按死亡次数扣除生命上限。
 */
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
		// 不屈效果是被动触发的，不需要每 tick 更新
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		super.addAttributeModifiers(entity, attributes, amplifier);

		if (entity instanceof Player player) {
			ImmortalityPenaltyHandler.handleEffectApplied(player);
		}
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		super.removeAttributeModifiers(entity, attributes, amplifier);

		if (entity instanceof Player player) {
			ImmortalityPenaltyHandler.handleEffectEnd(player);
		}
	}
}
