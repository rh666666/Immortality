package com.adoleiiiiii.immortality.handler;

import com.adoleiiiiii.immortality.Immortality;
import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.util.TruePowerKillHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 真正的力量事件入口：攻击触发旁路致命，并 tick 维持至 {@code dead} / 移除 / 超时。
 */
@Mod.EventBusSubscriber(modid = Immortality.MOD_ID)
public class TruePowerHandler {

	/** 单次跟踪最长 tick。 */
	private static final int MAX_TRACK_TICKS = TruePowerKillHelper.SUPPRESS_TICKS;

	private static final Map<UUID, PendingEntry> PENDING_KILLS = new HashMap<>();

	private static final class PendingEntry {
		/** 目标实体网络 ID。 */
		final int entityId;
		/** 已跟踪 tick。 */
		int ticks;

		PendingEntry(int entityId) {
			this.entityId = entityId;
			this.ticks = 0;
		}
	}

	/**
	 * 持有者攻击生物：登记跟踪、旁路致命，取消原版 {@code hurt}。
	 *
	 * @param event 玩家攻击实体事件
	 */
	@SubscribeEvent
	public static void onPlayerAttack(AttackEntityEvent event) {
		if (event.getEntity().level().isClientSide) {
			return;
		}
		if (!(event.getTarget() instanceof LivingEntity living)) {
			return;
		}

		Player player = event.getEntity();
		if (!player.hasEffect(ModEffects.TRUE_POWER_EFFECT)) {
			return;
		}

		PENDING_KILLS.put(player.getUUID(), new PendingEntry(living.getId()));
		TruePowerKillHelper.applyLethal(living, player);
		event.setCanceled(true);
	}

	/**
	 * 维持旁路击杀；不以 {@link LivingEntity#isAlive()} 为结束条件。
	 *
	 * @param event 玩家 tick 事件
	 */
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		if (event.player.level().isClientSide) {
			return;
		}
		if (!(event.player.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		Player player = event.player;
		PendingEntry entry = PENDING_KILLS.get(player.getUUID());
		if (entry == null) {
			return;
		}

		Entity raw = serverLevel.getEntity(entry.entityId);
		LivingEntity living = raw instanceof LivingEntity le ? le : null;
		entry.ticks++;

		if (TruePowerKillHelper.shouldStopTracking(living) || entry.ticks >= MAX_TRACK_TICKS) {
			PENDING_KILLS.remove(player.getUUID());
			return;
		}

		TruePowerKillHelper.maintainLethal(living, player);
		if (TruePowerKillHelper.shouldStopTracking(living)) {
			PENDING_KILLS.remove(player.getUUID());
		}
	}
}
