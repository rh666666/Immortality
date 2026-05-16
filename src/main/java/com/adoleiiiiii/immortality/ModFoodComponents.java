package com.adoleiiiiii.immortality;

import com.adoleiiiiii.immortality.effect.ModEffects;
import net.minecraft.SharedConstants;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.FoodComponent;

/**
 * 模组内自定义的食物属性定义。
 */
public class ModFoodComponents {

	/** 不死图腾作为食物时的饱食度、饱和度和效果。 */
	public static final FoodComponent TOTEM_OF_UNDYING = new FoodComponent.Builder()
            .hunger(6)
            .saturationModifier(1.0f)
            .alwaysEdible()
			.statusEffect(new StatusEffectInstance(ModEffects.IMMORTALITY, 5 * SharedConstants.TICKS_PER_MINUTE), 1.0f)
            .build();
}
