package com.adoleiiiiii.immortality.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * 真正的力量：当力量等级达到 XL 级时代替原版力量效果被激活，持有者攻击造成致命伤害。
 * <p>
 * 保护由 Mixin 层（removeStatusEffect 拦截 + clearStatusEffects 备份）承担。
 */
public class TruePowerEffect extends StatusEffect {

	public TruePowerEffect() {
		super(StatusEffectCategory.BENEFICIAL, 0xFF4500);
	}
}
