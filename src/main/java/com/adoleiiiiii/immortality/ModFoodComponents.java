package com.adoleiiiiii.immortality;

import net.minecraft.world.food.FoodProperties;

/**
 * 模组内自定义的食物属性定义。
 */
public final class ModFoodComponents {

	/** 不死图腾作为食物时的饱食度和饱和度（效果由事件处理）。 */
	public static final FoodProperties TOTEM_OF_UNDYING = new FoodProperties.Builder()
			.nutrition(6)
			.saturationMod(1.0f)
			.alwaysEat()
			.build();

	private ModFoodComponents() {
	}
}
