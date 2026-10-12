package com.oreseer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

public class OreSeerConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static OreSeerConfig instance = new OreSeerConfig();

	// --- general ---
	public boolean enabled = true;
	public boolean debrisOnly = false;
	public boolean hideExposed = false;
	public int scanRadius = 40;
	public int verticalRadius = 40;
	public int columnsPerTick = 3;
	public int maxHighlights = 140;

	// --- effects ---
	public float opacity = 0.8f;
	public float pulseSpeed = 1.0f;
	public boolean distanceFade = true;
	public int renderDistance = 64;
	public boolean tracerAll = false;

	// --- ancient debris extras ---
	public boolean debrisPillar = true;
	public boolean debrisTracer = true;
	public boolean debrisVeinFlash = true;

	// --- hud ---
	public boolean showHud = true;
	public boolean showRadar = true;

	public Map<String, OreSetting> ores = new LinkedHashMap<>();

	public static OreSeerConfig get() {
		return instance;
	}

	public OreSetting setting(OreType type) {
		return ores.computeIfAbsent(type.name(), k -> OreSetting.defaultsFor(type));
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("oreseer.json");
	}

	public static void load() {
		Path p = path();
		if (Files.exists(p)) {
			try (Reader r = Files.newBufferedReader(p)) {
				OreSeerConfig loaded = GSON.fromJson(r, OreSeerConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (Exception e) {
				OreSeerClient.LOGGER.warn("Could not read config, using defaults", e);
			}
		}
		if (instance.ores == null) {
			instance.ores = new LinkedHashMap<>();
		}
		for (OreType t : OreType.ALL) {
			instance.setting(t);
		}
		save();
	}

	public static void save() {
		try {
			Path p = path();
			Files.createDirectories(p.getParent());
			try (Writer w = Files.newBufferedWriter(p)) {
				GSON.toJson(instance, w);
			}
		} catch (IOException e) {
			OreSeerClient.LOGGER.warn("Could not save config", e);
		}
	}

	public static void reset() {
		instance = new OreSeerConfig();
		for (OreType t : OreType.ALL) {
			instance.setting(t);
		}
		save();
	}
}
