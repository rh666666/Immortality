package com.adoleiiiiii.immortality.util;

import com.adoleiiiiii.immortality.mixin.LivingEntityAccessor;
import com.adoleiiiiii.immortality.mixin.SynchedEntityDataAccessor;
import com.adoleiiiiii.immortality.player.TruePowerVictimAccess;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ForgeHooks;

/**
 * 真正的力量旁路击杀：开致命标记后补完死亡流程。
 * <p>
 * 不经 {@code hurt}/{@code actuallyHurt}；不强制 {@code Entity#remove}。
 */
public final class TruePowerKillHelper {

	/** 压制窗口（tick）。 */
	public static final int SUPPRESS_TICKS = 1200;

	private TruePowerKillHelper() {
	}

	/**
	 * 尽力将生命值压至 0；致命标记下由 ASM 保证 {@link LivingEntity#getHealth()} 语义为 0。
	 *
	 * @param target 目标生物
	 */
	public static void forceZeroHealth(LivingEntity target) {
		if (target.level().isClientSide) {
			return;
		}
		if (target instanceof TruePowerVictimAccess victim && !victim.immortality$isTruePowerLethal()) {
			victim.immortality$beginTruePowerLethal(SUPPRESS_TICKS);
		}

		writeHealthDataItem(target, 0.0F);
		try {
			target.setHealth(0.0F);
		} catch (Throwable ignored) {
			// 覆写抛错时仍依赖 ASM 致命语义
		}
	}

	/**
	 * 直写生命值 DataItem。
	 *
	 * @param living 目标
	 * @param health 生命值
	 */
	private static void writeHealthDataItem(LivingEntity living, float health) {
		SynchedEntityData data = living.getEntityData();
		EntityDataAccessor<Float> key = LivingEntityAccessor.immortality$getHealthDataId();
		SynchedEntityDataAccessor access = (SynchedEntityDataAccessor) (Object) data;
		SynchedEntityData.DataItem<Float> item = access.immortality$callGetItem(key);
		item.setValue(health);
		item.setDirty(true);
		access.immortality$setDirty(true);
		living.onSyncedDataUpdated(key);
	}

	/**
	 * 读取生命值 DataItem。
	 *
	 * @param living 目标
	 * @return 同步生命值
	 */
	private static float readHealthDataItem(LivingEntity living) {
		SynchedEntityData data = living.getEntityData();
		EntityDataAccessor<Float> key = LivingEntityAccessor.immortality$getHealthDataId();
		SynchedEntityDataAccessor access = (SynchedEntityDataAccessor) (Object) data;
		return access.immortality$callGetItem(key).getValue();
	}

	/**
	 * 施加致命旁路击杀。
	 *
	 * @param target   受击生物
	 * @param attacker 持有效果的玩家
	 */
	public static void applyLethal(LivingEntity target, Player attacker) {
		if (target.level().isClientSide) {
			return;
		}
		if (!(target instanceof TruePowerVictimAccess victim)) {
			return;
		}

		DamageSource source = target.damageSources().playerAttack(attacker);
		victim.immortality$beginTruePowerLethal(SUPPRESS_TICKS);
		target.setLastHurtByPlayer(attacker);
		target.setLastHurtByMob(attacker);

		float recorded = Math.max(1.0F, readHealthDataItem(target));
		target.getCombatTracker().recordDamage(source, recorded);
		forceZeroHealth(target);
		completeDeath(target, source);
	}

	/**
	 * 维持致命压制；生命语义为 0 且未死时补完死亡。
	 *
	 * @param target   受击生物
	 * @param attacker 持有效果的玩家
	 */
	public static void maintainLethal(LivingEntity target, Player attacker) {
		if (target.level().isClientSide || target.isRemoved()) {
			return;
		}
		if (!(target instanceof TruePowerVictimAccess victim)) {
			return;
		}

		LivingEntityAccessor accessor = (LivingEntityAccessor) target;
		if (accessor.immortality$isDead()) {
			return;
		}

		victim.immortality$beginTruePowerLethal(SUPPRESS_TICKS);
		forceZeroHealth(target);

		if (target.getHealth() <= 0.0F) {
			completeDeath(target, target.damageSources().playerAttack(attacker));
		}
	}

	/**
	 * 补完死亡：优先 {@code die}；未置 {@code dead} 则强制标记并掉落。
	 *
	 * @param target 目标
	 * @param source 致死来源
	 */
	public static void completeDeath(LivingEntity target, DamageSource source) {
		if (target.level().isClientSide) {
			return;
		}

		if (target instanceof TruePowerVictimAccess victim && !victim.immortality$isTruePowerLethal()) {
			victim.immortality$beginTruePowerLethal(SUPPRESS_TICKS);
		}
		forceZeroHealth(target);

		LivingEntityAccessor accessor = (LivingEntityAccessor) target;
		if (target.getHealth() > 0.0F) {
			target.die(source);
			return;
		}

		target.die(source);

		if (!accessor.immortality$isDead()) {
			if (ForgeHooks.onLivingDeath(target, source)) {
				return;
			}
			accessor.immortality$setDead(true);
			target.getCombatTracker().recheckStatus();

			if (target.level() instanceof ServerLevel serverLevel) {
				var killer = source.getEntity();
				if (killer == null || killer.killedEntity(serverLevel, target)) {
					target.gameEvent(GameEvent.ENTITY_DIE);
					accessor.immortality$dropAllDeathLoot(source);
				}
				serverLevel.broadcastEntityEvent(target, (byte) 3);
			}
			target.setPose(Pose.DYING);
		}
	}

	/**
	 * 击杀跟踪是否可结束。
	 *
	 * @param target 目标；{@code null} 视为可结束
	 * @return 应停止跟踪时为 true
	 */
	public static boolean shouldStopTracking(LivingEntity target) {
		if (target == null || target.isRemoved()) {
			return true;
		}
		LivingEntityAccessor accessor = (LivingEntityAccessor) target;
		return accessor.immortality$isDead() && target.getHealth() <= 0.0F;
	}
}
