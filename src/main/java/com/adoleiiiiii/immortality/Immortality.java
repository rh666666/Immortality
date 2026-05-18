package com.adoleiiiiii.immortality;

import com.adoleiiiiii.immortality.config.ImmortalityConfig;
import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.mixin.ItemAccessor;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class Immortality implements ModInitializer {

	public static final String MOD_ID = "immortality";

	@Override
	public void onInitialize() {
		ImmortalityConfig.load();
		ModEffects.initialize();

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (newPlayer instanceof ImmortalityPlayerAccess access) {
				access.immortality$clearMaxHealthPenalty();
				access.immortality$setBuffSessionActive(false);
				access.immortality$setEffectEndSettled(false);
			}
		});

		Item totem = Items.TOTEM_OF_UNDYING;
		if (totem instanceof ItemAccessor accessor) {
			accessor.setFoodComponent(ModFoodComponents.TOTEM_OF_UNDYING);
		}
	}
}
