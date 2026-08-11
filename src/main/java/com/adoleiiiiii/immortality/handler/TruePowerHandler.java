package com.adoleiiiiii.immortality.handler;

import com.adoleiiiiii.immortality.Immortality;
import com.adoleiiiiii.immortality.effect.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 真正的力量 — 持有者攻击任意目标时造成致命伤害，移除空手限制与白名单限制。
 * <p>
 * 多层击杀（参考 wudi-forge-1.20.1）：
 * <ol>
 *   <li><b>Phase.A</b> — {@link LivingDamageEvent} 设伤害为 {@link Float#MAX_VALUE}</li>
 *   <li><b>Phase.B</b>（每 tick）— {@code setHealth(0)} 持续压制回血/复活</li>
 *   <li><b>Phase.C</b>（tick ≥ 1）— {@code die()} 触发自然死亡流程</li>
 *   <li><b>Phase.D</b>（tick ≥ 8）— 反射保底移除，绕过拦截</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = Immortality.MOD_ID)
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
	}

	/** 反射缓存的 LivingEntity.dead 字段。 */
	private static Field DEAD_FIELD;

	/** 反射缓存的 LivingEntity.dropAllDeathLoot 方法。 */
	private static Method DROP_LOOT_METHOD;

	static {
		try {
			DEAD_FIELD = LivingEntity.class.getDeclaredField("dead");
			DEAD_FIELD.setAccessible(true);
		} catch (NoSuchFieldException e) {
			try {
				DEAD_FIELD = LivingEntity.class.getDeclaredField("f_20890_");
				DEAD_FIELD.setAccessible(true);
			} catch (NoSuchFieldException ignored) {
			}
		}
		try {
			DROP_LOOT_METHOD = LivingEntity.class.getDeclaredMethod("dropAllDeathLoot", DamageSource.class);
			DROP_LOOT_METHOD.setAccessible(true);
		} catch (NoSuchMethodException e) {
			try {
				DROP_LOOT_METHOD = LivingEntity.class.getDeclaredMethod("m_6668_", DamageSource.class);
				DROP_LOOT_METHOD.setAccessible(true);
			} catch (NoSuchMethodException ignored) {
			}
		}
	}

	/**
	 * 手动触发死亡流程（掉落、经验、击杀记录），在 {@link #forceKill} 之前调用。
	 */
	private static void manualDeath(LivingEntity entity, DamageSource source, @Nullable @SuppressWarnings("unused") Player player) {
		if (entity.level().isClientSide) return;
		if (!(entity.level() instanceof ServerLevel serverLevel)) return;

		entity.setHealth(0.0F);

		LivingEntity killCredit = entity.getKillCredit();
		int deathScore = 0;
		try {
			Field scoreField = LivingEntity.class.getDeclaredField("deathScore");
			scoreField.setAccessible(true);
			deathScore = scoreField.getInt(entity);
		} catch (ReflectiveOperationException ignored) {
		}
		if (deathScore >= 0 && killCredit != null) {
			killCredit.awardKillScore(entity, deathScore, source);
		}

		try {
			if (DEAD_FIELD != null) DEAD_FIELD.setBoolean(entity, true);
		} catch (ReflectiveOperationException ignored) {
		}

		entity.getCombatTracker().recheckStatus();

		try {
			if (DROP_LOOT_METHOD != null) {
				DROP_LOOT_METHOD.invoke(entity, source);
			}
		} catch (ReflectiveOperationException ignored) {
		}

		Entity attacker = source.getEntity();
		if (attacker == null || attacker.killedEntity(serverLevel, entity)) {
			entity.gameEvent(GameEvent.ENTITY_DIE);
		}

		serverLevel.broadcastEntityEvent(entity, (byte) 3);
		entity.setPose(Pose.DYING);
	}

	/**
	 * 终极保底：先正常移除，若被拦截则反射直接写 removalReason 字段。
	 */
	private static void forceKill(Entity entity, Player player) {
		if (entity == null || entity.isRemoved()) return;

		if (entity instanceof LivingEntity living) {
			living.setLastHurtByPlayer(player);
			living.setLastHurtByMob(player);
		}

		entity.remove(Entity.RemovalReason.KILLED);
		if (entity.isRemoved()) return;

		if (REMOVAL_REASON != null) {
			try {
				REMOVAL_REASON.set(entity, Entity.RemovalReason.KILLED);
				entity.stopRiding();
				entity.getPassengers().forEach(Entity::stopRiding);
			} catch (ReflectiveOperationException ignored) {
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerAttack(AttackEntityEvent event) {
		if (event.getEntity().level().isClientSide) return;
		if (!(event.getTarget() instanceof LivingEntity living)) return;

		Player player = event.getEntity();
		if (!player.hasEffect(ModEffects.TRUE_POWER_EFFECT)) return;

		PENDING_KILLS.put(player.getUUID(), new PendingEntry(living.getId()));
	}

	@SubscribeEvent
	public static void onLivingDamage(LivingDamageEvent event) {
		if (event.getEntity().level().isClientSide) return;

		if (event.getSource().getEntity() instanceof Player player
				&& player.hasEffect(ModEffects.TRUE_POWER_EFFECT)) {
			event.setAmount(Float.MAX_VALUE);
		}
	}

	/**
	 * 每 tick 对未死亡的待击杀目标执行血量压制→死亡流程→反射保底移除。
	 */
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		if (event.player.level().isClientSide) return;
		if (!(event.player.level() instanceof ServerLevel serverLevel)) return;

		Player player = event.player;
		PendingEntry entry = PENDING_KILLS.get(player.getUUID());
		if (entry == null) return;

		Entity raw = serverLevel.getEntity(entry.entityId);
		if (!(raw instanceof LivingEntity living) || !living.isAlive()) {
			PENDING_KILLS.remove(player.getUUID());
			return;
		}

		entry.ticks++;

		if (entry.ticks >= 1) {
			living.die(living.damageSources().playerAttack(player));
		}
		living.setHealth(0.0F);
		living.setHealth(0.0F);
		living.setHealth(0.0F);
		if (entry.ticks >= MAX_TICK - 2 && living.isAlive()) {
			manualDeath(living, living.damageSources().playerAttack(player), player);
			forceKill(living, player);
		}
		if (!living.isAlive() || entry.ticks >= MAX_TICK) {
			PENDING_KILLS.remove(player.getUUID());
		}
	}
}
