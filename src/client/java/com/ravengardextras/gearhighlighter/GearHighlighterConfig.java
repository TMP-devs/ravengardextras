package com.ravengardextras.gearhighlighter;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Heal-related settings. Gear outline highlighting itself now lives entirely in the gearrules package. */
public class GearHighlighterConfig {
	/** Sentinel color (alpha byte 0x01, never used by a real preset) meaning "animate rainbow". */
	public static final int RAINBOW = 0x01FFFFFF;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("ravengardextras.json");

	public boolean healEnabled = true;
	public int healColor = 0x5000FF00; // translucent green wash, overrides crown tier when it matches
	public List<String> healItemNames = List.of("Bandage", "Basic Bandage", "Apple", "Health Potion");
	/** Names (from healItemNames) the user has unchecked in the dashboard; empty = everything highlights. */
	public Set<String> healUncheckedItems = new HashSet<>();

	/** Replaces a healing item's stack-count corner with its HP amount (e.g. "120") everywhere it's rendered. */
	public boolean healAmountEnabled = true;

	public static GearHighlighterConfig load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				GearHighlighterConfig cfg = GSON.fromJson(reader, GearHighlighterConfig.class);
				if (cfg != null) {
					if (cfg.healUncheckedItems == null) {
						cfg.healUncheckedItems = new HashSet<>();
					}
					if (cfg.healItemNames == null) {
						cfg.healItemNames = new ArrayList<>(List.of("Bandage", "Basic Bandage", "Apple", "Health Potion"));
					} else if (!cfg.healItemNames.contains("Basic Bandage")) {
						cfg.healItemNames = new ArrayList<>(cfg.healItemNames);
						cfg.healItemNames.add("Basic Bandage");
						cfg.save();
					}
					return cfg;
				}
			} catch (IOException | RuntimeException ignored) {
				// fall through to defaults
			}
		}
		GearHighlighterConfig cfg = new GearHighlighterConfig();
		cfg.save();
		return cfg;
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

	/** Case-insensitive match against the configured heal item names, honoring per-item checkboxes. */
	public boolean isHealItem(String hoverName) {
		if (!healEnabled || hoverName == null) {
			return false;
		}
		for (String name : healItemNames) {
			if (hoverName.equalsIgnoreCase(name)) {
				return !healUncheckedItems.contains(name);
			}
		}
		return false;
	}
}
