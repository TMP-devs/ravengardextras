package com.ravengardextras.debug;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Appends investigation output to a single plain-text file (never chat, never the game screen).
 * Each entry is wrapped in a clearly labelled, ruled, timestamped section so it's easy to jump to
 * with Cmd+F. The file lives at {@code <game dir>/ravengardextras/debug.txt} (next to the profile's
 * {@code logs} folder).
 */
public final class DebugFile {

	private static final Logger LOGGER = LoggerFactory.getLogger("ravengardextras");
	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final String RULE = "============================================================";

	private static final Path FILE =
			FabricLoader.getInstance().getGameDir().resolve("ravengardextras").resolve("debug.txt");

	private DebugFile() {
	}

	/** Path of the debug file, so callers can log where output is going. */
	public static Path path() {
		return FILE;
	}

	/** Appends a titled, ruled section containing the given lines. */
	public static synchronized void section(String title, List<String> lines) {
		StringBuilder sb = new StringBuilder();
		sb.append('\n').append(RULE).append('\n');
		sb.append(">>> ").append(title).append("  @ ").append(LocalDateTime.now().format(TIMESTAMP)).append('\n');
		sb.append(RULE).append('\n');
		for (String line : lines) {
			sb.append(line).append('\n');
		}
		write(sb.toString());
	}

	private static void write(String text) {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, text, StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			LOGGER.error("Failed to write debug file {}", FILE, e);
		}
	}
}
