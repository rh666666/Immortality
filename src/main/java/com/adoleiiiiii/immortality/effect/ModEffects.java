package com.adoleiiiiii.immortality.effect;

import com.adoleiiiiii.immortality.Immortality;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEffects {
    /** 不屈buff */
    public static final StatusEffect IMMORTALITY = new ImmortalityEffect();

    public static void initialize() {
        Registry.register(Registries.STATUS_EFFECT, new Identifier(Immortality.MOD_ID, "immortality"), IMMORTALITY);
    }
}
