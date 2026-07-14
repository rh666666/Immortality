package com.adoleiiiiii.immortality;

import net.minecraft.resources.ResourceLocation;

/**
 * 不屈效果相关的常量定义。
 */
public final class ImmortalityConstants {

    private ImmortalityConstants() {
    }

    /** 效果结束时扣除生命上限所使用的属性修饰符 ID。 */
    public static final ResourceLocation MAX_HEALTH_PENALTY_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Immortality.MODID, "max_health_penalty");

    /** 不屈 buff 叠乘减伤对应的击退抗性属性修饰符 ID。 */
    public static final ResourceLocation KNOCKBACK_RESISTANCE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Immortality.MODID, "knockback_resistance");
}
