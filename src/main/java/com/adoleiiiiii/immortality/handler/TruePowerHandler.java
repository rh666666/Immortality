package com.adoleiiiiii.immortality.handler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 真正的力量 — 持有者攻击任意目标时造成致命伤害。
 * <p>
 * 多层击杀：Phase.A(Float.MAX_VALUE) → Phase.B(setHealth(0)) → Phase.C(die()) → Phase.D(反射保底)
 */
public class TruePowerHandler {

	private static class PendingEntry {
		final int entityId;
		int ticks;

		PendingEntry(int entityId) {
			this.entityId = entityId;
			this.ticks = 0;
		}
	}

	private static final Map<UUID, PendingEntry> PENDING_KILLS = new HashMap<>();
	private static final int MAX_TICK = 10;

	private static Field DEAD_FIELD;
	private static Method DROP_LOOT_METHOD;
	private static Field SCORE_AMOUNT_FIELD;
	private static Method ON_KILLED_BY_METHOD;

	private static final Field REMOVAL_REASON;

	static {
		Field found = null;
		for (Field f : Entity.class.getDeclaredFields()) {
			if (f.getType() == Entity.RemovalReason.class) {
				f.setAccessible(true);
				found = f;
				break;
			}
		}
		REMOVAL_REASON = found;
		try {
			DEAD_FIELD = LivingEntity.class.getDeclaredField("dead");
			DEAD_FIELD.setAccessible(true);
		} catch (NoSuchFieldException ignored) {
		}
		try {
			DROP_LOOT_METHOD = LivingEntity.class.getDeclaredMethod("dropLoot", DamageSource.class, boolean.class);
			DROP_LOOT_METHOD.setAccessible(true);
		} catch (NoSuchMethodException ignored) {
		}
		try {
			SCORE_AMOUNT_FIELD = LivingEntity.class.getDeclaredField("scoreAmount");
			SCORE_AMOUNT_FIELD.setAccessible(true);
		} catch (NoSuchFieldException ignored) {
		}
		try {
			ON_KILLED_BY_METHOD = LivingEntity.class.getDeclaredMethod("onKilledBy", LivingEntity.class);
			ON_KILLED_BY_METHOD.setAccessible(true);
		} catch (NoSuchMethodException ignored) {
		}
	}

	private TruePowerHandler() {
	}

	public static void registerKill(PlayerEntity player, LivingEntity target) {
		PENDING_KILLS.put(player.getUuid(), new PendingEntry(target.getId()));
	}

	public static void processPending(PlayerEntity player) {
		if (player.getWorld().isClient) return;
		if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

		PendingEntry entry = PENDING_KILLS.get(player.getUuid());
		if (entry == null) return;

		Entity raw = serverWorld.getEntityById(entry.entityId);
		if (!(raw instanceof LivingEntity living) || !living.isAlive()) {
			PENDING_KILLS.remove(player.getUuid());
			return;
		}

		entry.ticks++;

		if (entry.ticks >= 1) {
			living.onDeath(player.getDamageSources().playerAttack(player));
		}
		living.setHealth(0.0F);
		living.setHealth(0.0F);
		living.setHealth(0.0F);
		if (entry.ticks >= MAX_TICK - 2 && living.isAlive()) {
			manualDeath(living, player.getDamageSources().playerAttack(player), player);
			forceKill(living, player);
		}
		if (!living.isAlive() || entry.ticks >= MAX_TICK) {
			PENDING_KILLS.remove(player.getUuid());
		}
	}

	private static void manualDeath(LivingEntity entity, DamageSource source, @Nullable PlayerEntity player) {
		if (entity.getWorld().isClient) return;
		if (!(entity.getWorld() instanceof ServerWorld serverWorld)) return;

		entity.setHealth(0.0F);

		// 1. 击杀进度与归属（对应原版死亡流程中 livingEntity.updateKilledAdvancementCriterion）
		if (player != null) {
			int score = 0;
			try {
				if (SCORE_AMOUNT_FIELD != null) {
					score = SCORE_AMOUNT_FIELD.getInt(entity);
				}
			} catch (ReflectiveOperationException ignored) {
			}
			player.updateKilledAdvancementCriterion(entity, score, source);
		}

		// 2. dead 标志（防止重复死亡处理）
		try {
			if (DEAD_FIELD != null) DEAD_FIELD.setBoolean(entity, true);
		} catch (ReflectiveOperationException ignored) {
		}

		// 3. 被击杀回调（经验球等）
		if (player != null) {
			try {
				if (ON_KILLED_BY_METHOD != null) {
					ON_KILLED_BY_METHOD.invoke(entity, player);
				}
			} catch (ReflectiveOperationException ignored) {
			}
		}

		// 4. 掉落物
		try {
			if (DROP_LOOT_METHOD != null) {
				DROP_LOOT_METHOD.invoke(entity, source, player != null);
			}
		} catch (ReflectiveOperationException ignored) {
		}

		// 5. 死亡游戏事件
		entity.emitGameEvent(GameEvent.ENTITY_DIE);

		// 6. 死亡动画广播（对应 Forge 版 broadcastEntityEvent(entity, (byte)3)）
		serverWorld.sendEntityStatus(entity, (byte) 3);

		// 7. 死亡姿态
		entity.setPose(EntityPose.DYING);
	}

	private static void forceKill(Entity entity, PlayerEntity player) {
		if (entity == null || entity.isRemoved()) return;

		if (entity instanceof LivingEntity living) {
			living.setAttacker(player);
			living.setAttacking(player);
		}

		entity.remove(Entity.RemovalReason.KILLED);
		if (entity.isRemoved()) return;

		if (REMOVAL_REASON != null) {
			try {
				REMOVAL_REASON.set(entity, Entity.RemovalReason.KILLED);
				entity.stopRiding();
				entity.getPassengerList().forEach(Entity::stopRiding);
			} catch (ReflectiveOperationException ignored) {
			}
		}
	}
}
