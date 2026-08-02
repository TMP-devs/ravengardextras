package com.ravengardextras.runtools;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

import java.util.Locale;

/**
 * Detects a live Ravengard dungeon run. In the dungeon the server swaps the sidebar
 * scoreboard to an objective whose name starts with "dungeon" ("dungeon_sb"); the hub
 * uses "hub_sb". The Room Navigator feature relies on the same signal, so it is a proven
 * in-match indicator. Kept self-contained here so the run tools do not depend on the
 * navigator package.
 */
public final class RavengardRunDetector {
	private RavengardRunDetector() {
	}

	/** Match on the "dungeon" prefix so a rename like "dungeon_sb2" does not silently break it. */
	static boolean isDungeonObjective(String objectiveName) {
		return objectiveName != null && objectiveName.toLowerCase(Locale.ROOT).startsWith("dungeon");
	}

	public static boolean inRavengard(Minecraft client) {
		if (client == null || client.level == null) {
			return false;
		}
		Scoreboard scoreboard = client.level.getScoreboard();
		Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		return sidebar != null && isDungeonObjective(sidebar.getName());
	}

	/** The current sidebar objective name, or null. Handy for logging what the server actually uses. */
	public static String sidebarObjectiveName(Minecraft client) {
		if (client == null || client.level == null) {
			return null;
		}
		Objective sidebar = client.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
		return sidebar == null ? null : sidebar.getName();
	}
}
