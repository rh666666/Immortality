package com.adoleiiiiii.immortality.mixin.client;

import com.adoleiiiiii.immortality.util.ImmortalityDeathGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 不屈期间点「重生」时服务端玩家仍存活，发包会导致连接异常；改为仅关闭死亡 UI。
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerRespawnMixin {

	/**
	 * 死亡链路返空时取消重生包，只清死亡界面。
	 */
	@Inject(method = "respawn", at = @At("HEAD"), cancellable = true)
	private void immortality$respawnClosesDeathUiOnly(CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		if (!ImmortalityDeathGate.shouldVoidDeath(self)) {
			return;
		}
		ci.cancel();
		Minecraft.getInstance().setScreen(null);
	}
}
