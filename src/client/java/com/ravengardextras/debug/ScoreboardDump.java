package com.ravengardextras.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Captures the sidebar scoreboard exactly as the game renders it (same team-objective resolution,
 * sort order, and per-line name/score formatting as {@code Hud#displayScoreboardSidebar}), so it
 * can be dumped to the debug file for reading server-sent data such as ability cooldowns.
 */
public final class ScoreboardDump {

	private ScoreboardDump() {
	}

	/** Mirrors vanilla's sidebar ordering: by score value, then owner name (case-insensitive). */
	private static final Comparator<PlayerScoreEntry> DISPLAY_ORDER =
			Comparator.comparing(PlayerScoreEntry::value)
					.thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);

	/** Returns the sidebar as text lines, top-to-bottom, header first. Never null/empty. */
	public static List<String> sidebarLines() {
		List<String> lines = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			lines.add("(no world loaded)");
			return lines;
		}

		Scoreboard scoreboard = mc.level.getScoreboard();

		// Team-coloured sidebar takes precedence over the generic SIDEBAR slot, matching vanilla.
		Objective objective = null;
		PlayerTeam playerTeam = scoreboard.getPlayersTeam(mc.player.getScoreboardName());
		if (playerTeam != null && playerTeam.getColor().isPresent()) {
			objective = scoreboard.getDisplayObjective(playerTeam.getColor().get().displaySlot());
		}
		if (objective == null) {
			objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		}
		if (objective == null) {
			lines.add("(no sidebar objective is being shown)");
			return lines;
		}

		NumberFormat scoreFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
		lines.add("== " + objective.getDisplayName().getString() + " ==");

		scoreboard.listPlayerScores(objective).stream()
				.filter(score -> !score.isHidden())
				.sorted(DISPLAY_ORDER)
				.limit(15L)
				.forEach(score -> {
					PlayerTeam team = scoreboard.getPlayersTeam(score.owner());
					String name = PlayerTeam.formatNameForTeam(team, score.ownerName()).getString();
					String value = score.formatValue(scoreFormat).getString();
					lines.add(name + "  |  score=" + score.value() + "  display=\"" + value + "\"");
				});

		if (lines.size() == 1) {
			lines.add("(objective present but has no visible lines)");
		}
		return lines;
	}
}
