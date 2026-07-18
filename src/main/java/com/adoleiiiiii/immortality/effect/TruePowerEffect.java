package com.adoleiiiiii.immortality.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 真正的力量：当力量等级达到 XL 级时代替原版力量效果被激活，持有者攻击造成致命伤害。
 * <p>
 * 保护由 Mixin 层（{@code removeEffect} 拦截 + {@code removeAllEffects} 备份）承担。
 */
public class TruePowerEffect extends MobEffect {

	public TruePowerEffect() {
		super(MobEffectCategory.BENEFICIAL, 0xFF4500);
	}
}
