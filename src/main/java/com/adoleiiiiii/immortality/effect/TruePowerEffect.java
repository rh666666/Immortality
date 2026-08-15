package com.adoleiiiiii.immortality.effect;

import com.adoleiiiiii.immortality.handler.TruePowerHandler;
import com.adoleiiiiii.immortality.util.TruePowerKillHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 真正的力量：力量 XL 激活；攻击旁路致命见 {@link TruePowerHandler}/{@link TruePowerKillHelper}。
 */
public class TruePowerEffect extends MobEffect {

	public TruePowerEffect() {
		super(MobEffectCategory.BENEFICIAL, 0xFF4500);
	}
}
