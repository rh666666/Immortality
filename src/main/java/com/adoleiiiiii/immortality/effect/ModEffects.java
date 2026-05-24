package com.adoleiiiiii.immortality.effect;

import com.adoleiiiiii.immortality.Immortality;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组状态效果注册入口。
 */
public final class ModEffects {

	/** 状态效果延迟注册器。 */
	public static final DeferredRegister<MobEffect> MOB_EFFECTS =
			DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Immortality.MOD_ID);

	/** 不屈 buff 实例（注册前即可用于食物属性等静态引用）。 */
	public static final ImmortalityEffect IMMORTALITY_EFFECT = new ImmortalityEffect();

	/** 不屈 buff 注册对象。 */
	public static final RegistryObject<MobEffect> IMMORTALITY =
			MOB_EFFECTS.register("immortality", () -> IMMORTALITY_EFFECT);

	private ModEffects() {
	}
}
