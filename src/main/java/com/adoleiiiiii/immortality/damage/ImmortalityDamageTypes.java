package com.adoleiiiiii.immortality.damage;

import com.adoleiiiiii.immortality.Immortality;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * 模组自定义伤害类型注册键。
 */
public final class ImmortalityDamageTypes {

	/** 不屈 buff 惩罚致死（死亡信息显示为「燃尽了」）。 */
	public static final RegistryKey<DamageType> BURN_OUT =
			RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier(Immortality.MOD_ID, "burn_out"));

	private ImmortalityDamageTypes() {
	}
}
