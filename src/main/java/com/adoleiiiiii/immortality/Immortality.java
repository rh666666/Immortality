package com.adoleiiiiii.immortality;

import com.adoleiiiiii.immortality.advancement.ModAdvancements;
import com.adoleiiiiii.immortality.config.ImmortalityConfig;
import com.adoleiiiiii.immortality.damage.ImmortalityDamageTypes;
import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.mixin.ItemAccessor;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Immortality 模组 Forge 入口。
 */
@Mod(Immortality.MOD_ID)
public class Immortality {

	/** 模组 ID。 */
	public static final String MOD_ID = "immortality";

	/**
	 * 注册模组内容并订阅 Forge 事件。
	 *
	 * @param context Forge 模组加载上下文（由 FML 注入）
	 */
	public Immortality(FMLJavaModLoadingContext context) {
		IEventBus modEventBus = context.getModEventBus();
		ModEffects.MOB_EFFECTS.register(modEventBus);

		ImmortalityConfig.load();
		MinecraftForge.EVENT_BUS.register(this);

		Item totem = Items.TOTEM_OF_UNDYING;
		if (totem instanceof ItemAccessor accessor) {
			accessor.setFood(ModFoodComponents.TOTEM_OF_UNDYING);
		}
	}

	/**
	 * 玩家重生后清除不屈 buff 期间的生命上限惩罚与会话状态。
	 *
	 * @param event 玩家重生事件
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
	 *
	 * @param event 生物死亡事件
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
}
