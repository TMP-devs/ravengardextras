package com.ravengardextras.gearrules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Experimental: up to 4 named presets, each its own list of highlight cards. Only one applies at a time. */
public class GearRulesConfig {
	public static final int PRESET_COUNT = 4;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("ravengardextras_rules.json");

	/** Whichever preset tab is open is the one applied in-game - no separate per-preset toggle. */
	public boolean enabled = false;
	public int activePresetIndex = 0;
	public List<GearPreset> presets = new ArrayList<>();

	public GearPreset active() {
		return this.presets.get(Math.max(0, Math.min(this.activePresetIndex, this.presets.size() - 1)));
	}

	public static GearRulesConfig load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				GearRulesConfig cfg = GSON.fromJson(reader, GearRulesConfig.class);
				if (cfg != null && cfg.presets != null && !cfg.presets.isEmpty()) {
					normalize(cfg);
					return cfg;
				}
			} catch (IOException | RuntimeException ignored) {
				// fall through to defaults
			}
		}
		GearRulesConfig cfg = new GearRulesConfig();
		for (int i = 0; i < PRESET_COUNT; i++) {
			GearPreset preset = new GearPreset();
			preset.name = "Preset " + (i + 1);
			if (i == 0) {
				preset.cards.add(defaultCrownCard());
			}
			cfg.presets.add(preset);
		}
		cfg.save();
		return cfg;
	}

	/** Pads/truncates to PRESET_COUNT and guards against nulls from older or hand-edited files. */
	private static void normalize(GearRulesConfig cfg) {
		while (cfg.presets.size() < PRESET_COUNT) {
			GearPreset preset = new GearPreset();
			preset.name = "Preset " + (cfg.presets.size() + 1);
			cfg.presets.add(preset);
		}
		if (cfg.presets.size() > PRESET_COUNT) {
			cfg.presets = new ArrayList<>(cfg.presets.subList(0, PRESET_COUNT));
		}
		for (GearPreset preset : cfg.presets) {
			if (preset.cards == null) {
				preset.cards = new ArrayList<>();
			}
			if (preset.name == null || preset.name.isBlank()) {
				preset.name = "Preset";
			}
		}
	}

	private static GearCard defaultCrownCard() {
		GearCard card = new GearCard();
		card.name = "High Crown Value";
		card.rainbow = true;
		GearCondition condition = new GearCondition();
		condition.param = GearParam.CROWN_VALUE;
		condition.min = 75;
		condition.max = null;
		card.conditions.add(condition);
		return card;
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException ignored) {
			// non-fatal, config just won't persist this run
		}
	}
}
