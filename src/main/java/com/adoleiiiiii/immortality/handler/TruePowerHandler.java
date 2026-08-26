package com.adoleiiiiii.immortality.handler;

import com.adoleiiiiii.immortality.Immortality;
import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.util.TruePowerKillHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 真正的力量事件入口：满额 {@code hurt} 优先；未致死时改由 {@link TruePowerKillHelper} 旁路致命。
 */
@Mod.EventBusSubscriber(modid = Immortality.MOD_ID)
public class TruePowerHandler {

	/** 单次跟踪最长 tick。 */
	private static final int MAX_TRACK_TICKS = TruePowerKillHelper.SUPPRESS_TICKS;

	/**
	 * 满额 hurt 后的观察宽限（tick）。
	 * <p>
	 * 宽限结束时目标仍存活，则转入旁路致命。
	 */
	private static final int HURT_GRACE_TICKS = 2;

	private static final Map<UUID, PendingEntry> PENDING_KILLS = new HashMap<>();

	/** 击杀阶段。 */
	private enum KillPhase {
		/** 原版 {@code hurt} 满额伤害。 */
		HURT,
		/** 旁路致命。 */
		BYPASS
	}

	private static final class PendingEntry {
		/** 目标实体网络 ID。 */
		final int entityId;
		/** 已跟踪 tick。 */
		int ticks;
		/** 当前击杀阶段。 */
		KillPhase phase;

		/**
		 * @param entityId 目标实体网络 ID
		 */
		PendingEntry(int entityId) {
			this.entityId = entityId;
			this.ticks = 0;
			this.phase = KillPhase.HURT;
		}
	}

	/**
	 * 持有者攻击生物时登记跟踪目标。
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
		if (living instanceof Player) {
			return;
		}

		Player player = event.getEntity();
		if (!player.hasEffect(ModEffects.TRUE_POWER_EFFECT)) {
			return;
		}

		PENDING_KILLS.put(player.getUUID(), new PendingEntry(living.getId()));
	}

	/**
	 * 将持有者造成的结算伤害设为 {@link Float#MAX_VALUE}。
	 *
	 * @param event 生物受伤结算事件
	 */
	@SubscribeEvent
	public static void onLivingDamage(LivingDamageEvent event) {
		if (event.getEntity().level().isClientSide) {
			return;
		}
		if (!(event.getSource().getEntity() instanceof Player player)) {
			return;
		}
		if (!player.hasEffect(ModEffects.TRUE_POWER_EFFECT)) {
			return;
		}
		event.setAmount(Float.MAX_VALUE);
	}

	/**
	 * 观察满额 hurt 结果；未致死则旁路致命并维持至结束条件。
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

		if (living == null || living.isRemoved() || living instanceof Player || entry.ticks >= MAX_TRACK_TICKS) {
			PENDING_KILLS.remove(player.getUUID());
			return;
		}

		if (entry.phase == KillPhase.HURT) {
			if (TruePowerKillHelper.isHurtPhaseResolved(living)) {
				PENDING_KILLS.remove(player.getUUID());
				return;
			}
			if (entry.ticks < HURT_GRACE_TICKS) {
				return;
			}
			entry.phase = KillPhase.BYPASS;
			TruePowerKillHelper.applyLethal(living, player);
			if (TruePowerKillHelper.shouldStopTracking(living)) {
				PENDING_KILLS.remove(player.getUUID());
			}
			return;
		}

		if (TruePowerKillHelper.shouldStopTracking(living)) {
			PENDING_KILLS.remove(player.getUUID());
			return;
		}

		TruePowerKillHelper.maintainLethal(living, player);
		if (TruePowerKillHelper.shouldStopTracking(living)) {
			PENDING_KILLS.remove(player.getUUID());
		}
	}
}
