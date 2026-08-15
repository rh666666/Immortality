package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.util.ImmortalityDeathGate;
import com.adoleiiiiii.immortality.util.ImmortalityHealthDataSanitizer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家 tick 入口：不屈闸门开启时清理会毒害读血的荒谬负向同步浮点。
 * <p>
 * 不注入 {@code getHealth}：原版 {@link Player} 未声明该方法，他模组重写时也无法在编译期确定描述符。
 */
@Mixin(value = Player.class, priority = 10000)
public abstract class PlayerImmortalityTickMixin {

	/**
	 * 每 tick 清理荒谬负向同步浮点（仅死亡闸门开启时）。
	 *
	 * @param ci 回调
	 */
	@Inject(method = "tick", at = @At("HEAD"))
	private void immortality$sanitizeHealthOffsets(CallbackInfo ci) {
		Player player = (Player) (Object) this;
		if (!ImmortalityDeathGate.shouldVoidDeath(player)) {
			return;
		}
		ImmortalityHealthDataSanitizer.clearAbsurdFloatOffsets(player);
	}
}
