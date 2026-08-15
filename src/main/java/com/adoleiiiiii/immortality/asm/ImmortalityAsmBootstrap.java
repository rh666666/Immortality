package com.adoleiiiiii.immortality.asm;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 将 {@link ImmortalityLaunchPlugin} 反射注册进 ModLauncher（{@code META-INF/services} 未必进启动层）。
 */
public final class ImmortalityAsmBootstrap {

	private static final Logger LOGGER = LoggerFactory.getLogger("immortality.asm");

	private static boolean registered;

	private ImmortalityAsmBootstrap() {
	}

	/** 幂等注册 LaunchPlugin。 */
	public static synchronized void register() {
		if (registered) {
			return;
		}
		try {
			ILaunchPluginService plugin = new ImmortalityLaunchPlugin();
			Field launchPluginsField = Launcher.class.getDeclaredField("launchPlugins");
			launchPluginsField.setAccessible(true);
			Object handler = launchPluginsField.get(Launcher.INSTANCE);
			Field pluginsField = handler.getClass().getDeclaredField("plugins");
			pluginsField.setAccessible(true);
			@SuppressWarnings("unchecked")
			Map<String, ILaunchPluginService> plugins =
					(Map<String, ILaunchPluginService>) pluginsField.get(handler);
			plugins.put(plugin.name(), plugin);
			registered = true;
			LOGGER.info("Registered launch plugin '{}' (Phase.AFTER).", plugin.name());
		} catch (Throwable t) {
			LOGGER.error("Failed to register launch plugin", t);
		}
	}
}
