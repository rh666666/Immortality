package com.adoleiiiiii.immortality.mixin;

import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问 {@link Item} 的食物组件字段，以便为已注册的原版物品补充食用属性。
 */
@Mixin(Item.class)
public interface ItemAccessor {

	/**
	 * 设置该物品的食物组件。
	 *
	 * @param foodComponent 食物属性；{@code null} 表示不可食用
	 */
	@Mutable
	@Accessor("foodComponent")
	void setFoodComponent(FoodComponent foodComponent);
}
