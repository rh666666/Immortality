package com.adoleiiiiii.immortality.effect;

import com.adoleiiiiii.immortality.Immortality;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组状态效果注册入口。
 */
public final class ModEffects {

    /** 状态效果延迟注册器。 */
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Immortality.MODID);

    /** 不屈 buff 注册对象（Holder 同时作为静态引用使用）。 */
    public static final DeferredHolder<MobEffect, ImmortalityEffect> IMMORTALITY_EFFECT =
            MOB_EFFECTS.register("immortality", ImmortalityEffect::new);

    private ModEffects() {
    }
}
