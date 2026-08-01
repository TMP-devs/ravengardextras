package com.ravengardextras.ping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Party Ping settings. Lives in its own file so the Gear Highlighter config format stays untouched. */
public class PingConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("ravengardextras-ping.json");

	public boolean enabled = true;
	/** Chat command (no slash) the ping is sent through, e.g. "pc" -> "/pc RGE-PING @ x, y, z". Blank = local-only ping. */
	public String partyCommand = "pc";
	public int pingDurationSeconds = 60;
	public int maxPingDistance = 160;

	public static PingConfig load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				PingConfig cfg = GSON.fromJson(reader, PingConfig.class);
				if (cfg != null) {
					cfg.sanitize();
					return cfg;
				}
			} catch (IOException | RuntimeException ignored) {
				// fall through to defaults
			}
		}
		PingConfig cfg = new PingConfig();
		cfg.save();
		return cfg;
	}

	public void save() {
		sanitize();
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException ignored) {
			// non-fatal, config just won't persist this run
		}
	}

	private void sanitize() {
		if (partyCommand == null) {
			partyCommand = "";
		}
		partyCommand = partyCommand.strip();
		if (partyCommand.startsWith("/")) {
			partyCommand = partyCommand.substring(1);
		}
		pingDurationSeconds = Math.max(5, Math.min(600, pingDurationSeconds));
		maxPingDistance = Math.max(8, Math.min(512, maxPingDistance));
	}
}
