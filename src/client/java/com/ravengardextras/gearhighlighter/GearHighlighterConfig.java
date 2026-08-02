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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Three threshold/color tiers. Higher tier number = higher bar; the highest one met wins. */
public class GearHighlighterConfig {
	/** Sentinel color (alpha byte 0x01, never used by a real preset) meaning "animate rainbow". */
	public static final int RAINBOW = 0x01FFFFFF;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("ravengardextras.json");

	public boolean enabled = false;
	public long tier1Threshold = 8;
	public int tier1Color = 0xFF55FFFF; // ARGB, default light blue
	public long tier2Threshold = 12;
	public int tier2Color = 0xFFFFFF55; // ARGB, default yellow
	public long tier3Threshold = 18;
	public int tier3Color = RAINBOW; // default RGB/rainbow

	public boolean healEnabled = true;
	public int healColor = 0x5000FF00; // translucent green wash, overrides crown tier when it matches
	public List<String> healItemNames = List.of("Bandage", "Apple", "Health Potion");
	/** Names (from healItemNames) the user has unchecked in the dashboard; empty = everything highlights. */
	public Set<String> healUncheckedItems = new HashSet<>();

	public static GearHighlighterConfig load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				GearHighlighterConfig cfg = GSON.fromJson(reader, GearHighlighterConfig.class);
				if (cfg != null) {
					if (cfg.healUncheckedItems == null) {
						cfg.healUncheckedItems = new HashSet<>();
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

	/** Returns ARGB outline color for the given crown value, or 0 if below every threshold. */
	public int colorFor(long crowns) {
		if (!enabled) {
			return 0;
		}
		if (crowns >= tier3Threshold) {
			return tier3Color;
		}
		if (crowns >= tier2Threshold) {
			return tier2Color;
		}
		if (crowns >= tier1Threshold) {
			return tier1Color;
		}
		return 0;
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
