package com.adoleiiiiii.immortality;

import com.adoleiiiiii.immortality.effect.ModEffects;
import net.minecraft.SharedConstants;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;

/**
 * 模组内自定义的食物属性定义。
 * <p>
 * 在 1.21.1 中通过 {@link net.neoforged.neoforge.event.ModifyDefaultComponentsEvent}
 * 将食物属性附加到不死图腾上。
 */
public final class ModFoodComponents {

    /** 不死图腾作为食物时的饱食度、饱和度和效果。 */
    public static final FoodProperties TOTEM_OF_UNDYING = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(1.0f)
            .alwaysEdible()
            .effect(() -> new MobEffectInstance(ModEffects.IMMORTALITY_EFFECT,
                    5 * SharedConstants.TICKS_PER_MINUTE), 1.0f)
            .build();

    private ModFoodComponents() {
    }
}
