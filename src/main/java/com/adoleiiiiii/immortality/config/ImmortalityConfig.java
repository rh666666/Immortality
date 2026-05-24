package com.adoleiiiiii.immortality.config;

import com.adoleiiiiii.immortality.Immortality;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 不屈效果模组配置（{@code config/immortality.yml}）。
 * <p>
 * 公式与调参说明见配置文件内注释；逻辑与 {@link com.adoleiiiiii.immortality.util.ImmortalityDamageHelper} 一致。
 */
public final class ImmortalityConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(Immortality.MOD_ID);
	private static final Path CONFIG_PATH =
			FMLPaths.CONFIGDIR.get().resolve("immortality.yml");
	private static final String DEFAULT_CONFIG_RESOURCE = "/immortality-default.yml";

	/** 默认减伤系数：H_max=20、D=19 时 R=95%。 */
	public static final float DEFAULT_DAMAGE_REDUCTION_K = 20.0f;

	private static float damageReductionK = DEFAULT_DAMAGE_REDUCTION_K;

	private ImmortalityConfig() {
	}

	/**
	 * 从 {@code config/immortality.yml} 加载配置；不存在则从模板复制默认文件（含注释）。
	 */
	public static void load() {
		if (!Files.exists(CONFIG_PATH)) {
			copyDefaultConfig();
		}

		if (!Files.exists(CONFIG_PATH)) {
			LOGGER.warn("Config file missing after copy attempt, using defaults");
			damageReductionK = DEFAULT_DAMAGE_REDUCTION_K;
			return;
		}

		try {
			String content = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
			damageReductionK = parseDamageReductionK(content);
			validate();
			LOGGER.info("Loaded config: damageReductionK={}", damageReductionK);
		} catch (Exception e) {
			LOGGER.error("Failed to load config, using defaults", e);
			damageReductionK = DEFAULT_DAMAGE_REDUCTION_K;
		}
	}

	/**
	 * 获取当前减伤系数 k。
	 *
	 * @return 减伤系数 k
	 */
	public static float getDamageReductionK() {
		return damageReductionK;
	}

	/**
	 * 从 YAML 文本解析 {@code damageReductionK}（忽略注释与空行）。
	 *
	 * @param content 配置文件全文
	 * @return 减伤系数 k
	 * @throws IllegalArgumentException 未找到有效键值时抛出
	 */
	private static float parseDamageReductionK(String content) {
		for (String line : content.split("\\R")) {
			String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("#")) {
				continue;
			}
			if (trimmed.startsWith("damageReductionK:")) {
				String value = trimmed.substring("damageReductionK:".length()).trim();
				return Float.parseFloat(value);
			}
		}
		throw new IllegalArgumentException("damageReductionK not found in config");
	}

	private static void copyDefaultConfig() {
		try (InputStream input = ImmortalityConfig.class.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
			if (input == null) {
				LOGGER.error("Default config resource not found: {}", DEFAULT_CONFIG_RESOURCE);
				return;
			}
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.copy(input, CONFIG_PATH);
			LOGGER.info("Created default config at {}", CONFIG_PATH);
		} catch (IOException e) {
			LOGGER.error("Failed to write default config", e);
		}
	}

	private static void validate() {
		if (damageReductionK <= 0.0f || Float.isNaN(damageReductionK) || Float.isInfinite(damageReductionK)) {
			LOGGER.warn("Invalid damageReductionK={}, resetting to {}", damageReductionK, DEFAULT_DAMAGE_REDUCTION_K);
			damageReductionK = DEFAULT_DAMAGE_REDUCTION_K;
		}
	}
}
