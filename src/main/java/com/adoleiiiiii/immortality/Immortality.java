package com.adoleiiiiii.immortality;

import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.mixin.ItemAccessor;
import net.fabricmc.api.ModInitializer;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Immortality implements ModInitializer {
	public static final String MOD_ID = "immortality";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ModEffects.initialize();

		Item totem = Items.TOTEM_OF_UNDYING;
		if (totem instanceof ItemAccessor accessor) {
			accessor.setFoodComponent(ModFoodComponents.TOTEM_OF_UNDYING);
		}

		LOGGER.info("Hello Fabric world!");
	}
}