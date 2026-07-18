package com.adoleiiiiii.immortality;

import com.adoleiiiiii.immortality.advancement.ModAdvancements;
import com.adoleiiiiii.immortality.config.ImmortalityConfig;
import com.adoleiiiiii.immortality.damage.ImmortalityDamageTypes;
import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.mixin.ItemAccessor;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
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
	 * 食用不死图腾时叠加不屈等级与时长。
	 * <p>
	 * 每吃一个，不屈等级 +1（最高 VIII 级），时长叠加规律：<br>
	 * 无→I +5:00，I→II +2:30，II→III +1:15……每级新增时长减半，非整 tick 向上取整。
	 */
	@SubscribeEvent
	public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
		if (event.getItem().getItem() != Items.TOTEM_OF_UNDYING) return;
		if (!(event.getEntity() instanceof Player player)) return;
		if (player.level().isClientSide) return;
		if (!(player instanceof ImmortalityPlayerAccess access)) return;

		MobEffectInstance current = player.getEffect(ModEffects.IMMORTALITY_EFFECT);
		int curAmp = current != null ? current.getAmplifier() : -1;
		int oldDuration = current != null ? current.getDuration() : 0;

		int addDuration = (int) Math.ceil(6000.0 / Math.pow(2, curAmp + 1));
		int newAmp = Math.min(curAmp + 1, 7);

		access.immortality$setRefreshingBuff(true);
		try {
			player.removeEffect(ModEffects.IMMORTALITY_EFFECT);
			player.addEffect(new MobEffectInstance(
					ModEffects.IMMORTALITY_EFFECT, oldDuration + addDuration, newAmp, false, false, true));
			if (player.hasEffect(ModEffects.TRUE_POWER_EFFECT)) {
				player.addEffect(new MobEffectInstance(
						ModEffects.TRUE_POWER_EFFECT, oldDuration + addDuration, 0, false, false, true));
			}
		} finally {
			access.immortality$setRefreshingBuff(false);
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
