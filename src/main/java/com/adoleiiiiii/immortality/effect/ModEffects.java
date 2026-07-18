package com.adoleiiiiii.immortality.effect;

import com.adoleiiiiii.immortality.Immortality;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * 模组状态效果注册入口。
 */
public class ModEffects {

	/* 不屈 buff。 */
	public static final StatusEffect IMMORTALITY = new ImmortalityEffect();

	/* 真正的力量。 */
	public static final StatusEffect TRUE_POWER = new TruePowerEffect();

	private ModEffects() {
	}

	/**
	 * 向游戏注册所有状态效果。
	 */
	public static void initialize() {
		Registry.register(Registries.STATUS_EFFECT, new Identifier(Immortality.MOD_ID, "immortality"), IMMORTALITY);
		Registry.register(Registries.STATUS_EFFECT, new Identifier(Immortality.MOD_ID, "true_power"), TRUE_POWER);
	}
}
