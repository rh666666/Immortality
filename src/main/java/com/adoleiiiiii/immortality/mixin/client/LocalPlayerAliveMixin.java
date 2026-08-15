package com.adoleiiiiii.immortality.mixin.client;

import com.adoleiiiiii.immortality.util.ImmortalityDeathGate;
import com.adoleiiiiii.immortality.util.ImmortalityTotemHelper;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端本地玩家：不屈期间 {@code tickDeath} 返空。
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerAliveMixin {

	/**
	 * 不屈时取消客户端独立实现的死亡 tick。
	 */
	@Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
	private void immortality$voidLocalTickDeath(CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		if (!ImmortalityDeathGate.shouldVoidDeath(self)) {
			return;
		}
		ImmortalityTotemHelper.ensureSurviving(self);
		ci.cancel();
	}
}
