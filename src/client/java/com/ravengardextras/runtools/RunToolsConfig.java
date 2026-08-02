package com.ravengardextras.runtools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings for the three "run tools" delivered together: the Crown Calculator and
 * XP Calculator (a per-run HUD) and the Stronghold sound alert. Kept in one file
 * ({@code ravengardextras-runtools.json}) because they share the run lifecycle.
 */
public class RunToolsConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("ravengardextras-runtools.json");

	/** Crown Calculator: show net Crowns gained this run on the HUD. */
	public boolean crownCalcEnabled = true;
	/** XP Calculator: read XP gains from chat/action bar and total them for the run. */
	public boolean xpCalcEnabled = true;
	/** Stronghold alert: play a dragon growl when chat announces the stronghold opened. */
	public boolean strongholdAlertEnabled = true;
	/** Growl loudness, 0.0–1.0. Default is a medium 0.6. */
	public float strongholdVolume = 0.6F;

	public static RunToolsConfig load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				RunToolsConfig cfg = GSON.fromJson(reader, RunToolsConfig.class);
				if (cfg != null) {
					return cfg;
				}
			} catch (IOException | RuntimeException ignored) {
				// fall through to defaults
			}
		}
		RunToolsConfig cfg = new RunToolsConfig();
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
}
