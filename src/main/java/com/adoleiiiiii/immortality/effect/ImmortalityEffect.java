package com.adoleiiiiii.immortality.effect;

import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import com.adoleiiiiii.immortality.util.ImmortalityPenaltyHandler;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;

import static net.minecraft.SharedConstants.TICKS_PER_MINUTE;

/**
 * 不屈状态效果：buff 期间抵抗死亡并获得叠乘减伤与击退抗性，结束时按死亡次数扣除生命上限。
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
		if (!(entity instanceof Player player)) return;
		if (!(player instanceof ImmortalityPlayerAccess access)) return;

		// 每 tick 同步实际剩余时长。
		MobEffectInstance effect = player.getEffect(ModEffects.IMMORTALITY_EFFECT);
		if (effect != null) {
			access.immortality$setImmortalityDuration(effect.getDuration());
		}

		// 自然到期前撤销保护标记。
		if (effect != null && effect.getDuration() <= 2 && access.immortality$isProtected()) {
			access.immortality$setProtected(false);
		}
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		super.addAttributeModifiers(entity, attributes, amplifier);

		if (entity instanceof Player player) {
			ImmortalityPenaltyHandler.handleEffectApplied(player);

			// 标记为受保护状态，并保存时长用于恢复。
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

		if (!(entity instanceof Player player)) return;
		if (!(player instanceof ImmortalityPlayerAccess access)) return;

		// 刷新中（吃图腾）→ 不干涉。
		if (access.immortality$isRefreshingBuff()) {
			return;
		}

		// 受保护状态 → 非自然移除（命令/其他模组），恢复不屈效果并跳过惩罚。
		if (access.immortality$isProtected()) {
			int duration = access.immortality$getImmortalityDuration();
			if (duration <= 0) {
				duration = TICKS_PER_MINUTE;
			}
			player.addEffect(new MobEffectInstance(
					ModEffects.IMMORTALITY_EFFECT, duration, amplifier, false, false, true));
			return;
		}

		// 已授权移除（自然到期/牛奶桶）→ 正常结算惩罚。
		ImmortalityPenaltyHandler.handleEffectEnd(player);
	}
}
