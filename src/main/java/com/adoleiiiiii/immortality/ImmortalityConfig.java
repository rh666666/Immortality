package com.adoleiiiiii.immortality;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 不屈效果模组配置（NeoForge ModConfigSpec）。
 * <p>
 * 减伤公式: R = 1 - 1/(1 + k · D/H_max)
 * 默认 k = 20：H_max = 20、D = 19 时 R = 95%。
 */
public final class ImmortalityConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(Immortality.MODID);

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** 减伤系数 k（浮点，默认 20.0）。 */
    public static final ModConfigSpec.DoubleValue DAMAGE_REDUCTION_K = BUILDER
            .comment(
                    "Damage reduction coefficient k. Formula: R = 1 - 1/(1 + k * D / H_max)",
                    "Default k=20: at H_max=20, D=19 gives R=95%. Higher = faster ramp-up.",
                    "Must be positive."
            )
            .defineInRange("damageReductionK", 20.0, 0.001, Double.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();

    private ImmortalityConfig() {
    }

    public static float getDamageReductionK() {
        return DAMAGE_REDUCTION_K.get().floatValue();
    }
}
