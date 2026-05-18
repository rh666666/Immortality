package com.adoleiiiiii.immortality.util;

import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 不屈图腾触发时的状态效果处理工具。
 */
public final class ImmortalityEffectHelper {

	private ImmortalityEffectHelper() {
	}

	/**
	 * 仅移除负面状态效果，保留不屈等增益效果。
	 *
	 * @param player 玩家实体
	 */
	public static void clearHarmfulStatusEffects(PlayerEntity player) {
		List<StatusEffect> toRemove = new ArrayList<>();
		for (StatusEffectInstance instance : player.getStatusEffects()) {
			StatusEffect effect = instance.getEffectType();
			if (effect.getCategory() == StatusEffectCategory.HARMFUL) {
				toRemove.add(effect);
			}
		}
		for (StatusEffect effect : toRemove) {
			player.removeStatusEffect(effect);
		}
	}

	/**
	 * 图腾免死后将生命恢复至当前真实上限（清除 buff 期间不应存在的残留惩罚修饰符）。
	 *
	 * @param player 玩家实体
	 */
	public static void restoreHealthToMax(PlayerEntity player) {
		if (player instanceof ImmortalityPlayerAccess access) {
			access.immortality$clearMaxHealthPenalty();
		}
		float maxHealth = player.getMaxHealth();
		player.setHealth(maxHealth);
	}
}
