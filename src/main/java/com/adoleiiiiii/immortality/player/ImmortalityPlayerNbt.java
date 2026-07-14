package com.adoleiiiiii.immortality.player;

/**
 * 玩家不屈状态在 NBT 中的键名。
 */
public final class ImmortalityPlayerNbt {

    /** 根 compound 标签名。 */
    public static final String ROOT = "ImmortalityPlayer";

    /** buff 期间累计死亡次数。 */
    public static final String DEATH_COUNT = "DeathCount";

    /** 是否处于不屈 buff 会话中。 */
    public static final String BUFF_SESSION_ACTIVE = "BuffSessionActive";

    /** 本次 buff 结束惩罚是否已结算。 */
    public static final String EFFECT_END_SETTLED = "EffectEndSettled";

    private ImmortalityPlayerNbt() {
    }
}
