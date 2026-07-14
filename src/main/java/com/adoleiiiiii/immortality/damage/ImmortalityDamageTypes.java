package com.adoleiiiiii.immortality.damage;

import com.adoleiiiiii.immortality.Immortality;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/**
 * 模组自定义伤害类型注册键。
 */
public final class ImmortalityDamageTypes {

    /** 不屈 buff 惩罚致死（死亡信息显示为「燃尽了」）。 */
    public static final ResourceKey<DamageType> BURN_OUT =
            ResourceKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(Immortality.MODID, "burn_out"));

    private ImmortalityDamageTypes() {
    }
}
