package com.adoleiiiiii.immortality;

import com.adoleiiiiii.immortality.advancement.ModAdvancements;
import com.adoleiiiiii.immortality.damage.ImmortalityDamageTypes;
import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import com.adoleiiiiii.immortality.util.ImmortalityPenaltyHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Immortality 模组 NeoForge 入口。
 */
@Mod(Immortality.MODID)
public class Immortality {

    /** 模组 ID。 */
    public static final String MODID = "immortality";

    /** 日志记录器。 */
    public static final Logger LOGGER = LoggerFactory.getLogger(Immortality.MODID);

    /**
     * 注册模组内容并订阅事件。
     */
    public Immortality(IEventBus modEventBus, ModContainer modContainer) {
        // 注册状态效果
        ModEffects.MOB_EFFECTS.register(modEventBus);

        // 注册配置
        modContainer.registerConfig(ModConfig.Type.COMMON, ImmortalityConfig.SPEC);

        // 为不死图腾添加食物属性（可食用触发不屈 buff）
        modEventBus.addListener(this::modifyDefaultComponents);

        // 监听 Forge 全局事件
        NeoForge.EVENT_BUS.register(this);
    }

    /**
     * 为不死图腾附加食物数据组件，使其可食用并获得不屈 buff。
     */
    private void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        event.modify(net.minecraft.world.item.Items.TOTEM_OF_UNDYING, builder ->
                builder.set(net.minecraft.core.component.DataComponents.FOOD, ModFoodComponents.TOTEM_OF_UNDYING)
        );
    }

    /**
     * 玩家重生后清除不屈 buff 期间的生命上限惩罚与会话状态。
     */
    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ImmortalityPlayerAccess access) {
            access.immortality$clearMaxHealthPenalty();
            access.immortality$setBuffSessionActive(false);
            access.immortality$setEffectEndSettled(false);
        }
    }

    /**
     * 玩家因「燃尽」惩罚伤害死亡时授予「燃尽此身」进度。
     */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getSource().is(ImmortalityDamageTypes.BURN_OUT)) {
            ModAdvancements.grantBurnOutBody(player);
        }
    }

    /**
     * 不屈效果自然到期时结算生命上限惩罚。
     * <p>
     * 注意：效果到期只触发 {@link MobEffectEvent.Expired}，<b>不</b>触发 Remove。
     */
    @SubscribeEvent
    public void onMobEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance == null || !effectInstance.getEffect().is(ModEffects.IMMORTALITY_EFFECT.getKey())) {
            return;
        }
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        ImmortalityPenaltyHandler.handleEffectEnd(player);
    }

    /**
     * 不屈效果被移除（手动/牛奶/命令）时结算生命上限惩罚。
     */
    @SubscribeEvent
    public void onMobEffectRemoved(MobEffectEvent.Remove event) {
        if (!event.getEffect().is(ModEffects.IMMORTALITY_EFFECT.getKey())) {
            return;
        }
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        ImmortalityPenaltyHandler.handleEffectEnd(player);
    }
}
