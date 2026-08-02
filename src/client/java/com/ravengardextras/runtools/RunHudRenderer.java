package com.ravengardextras.runtools;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the per-run tally while a run is active: "Calculated Crowns" (gold) and
 * "Calculated EXP" (green). Positioned in the upper-right, roughly where the server's
 * empty info box sits, right-aligned and drawn with a text shadow so it stays legible
 * with no backing panel.
 *
 * <p>The position is a fraction of the scaled screen ({@link #RIGHT_FRAC}/{@link #TOP_FRAC})
 * so it scales with resolution — nudge those two constants to reposition.
 */
public final class RunHudRenderer {
	private static final Identifier ID = Identifier.fromNamespaceAndPath("ravengardextras", "run_hud");
	private static final float RIGHT_FRAC = 0.02f;
	private static final float TOP_FRAC = 0.38f;
	private static final int LINE_HEIGHT = 10;
	private static final int CROWN_COLOR = 0xFFFFD24A;
	private static final int XP_COLOR = 0xFF66FF66;

	private RunHudRenderer() {
	}

	public static void register(RunTracker tracker, RunToolsConfig config) {
		HudElementRegistry.addLast(ID, (graphics, delta) -> render(graphics, tracker, config));
	}

	private static void render(GuiGraphicsExtractor graphics, RunTracker tracker, RunToolsConfig config) {
		if (!tracker.isRunActive()) {
			return;
		}

		List<String> lines = new ArrayList<>(2);
		List<Integer> colors = new ArrayList<>(2);
		if (config.crownCalcEnabled) {
			lines.add("Calculated Crowns: " + RunTracker.formatSigned(tracker.netCrowns()));
			colors.add(CROWN_COLOR);
		}
		if (config.xpCalcEnabled) {
			lines.add("Calculated EXP: " + RunTracker.formatSigned(tracker.runXp()));
			colors.add(XP_COLOR);
		}
		if (lines.isEmpty()) {
			return;
		}

		Font font = Minecraft.getInstance().font;
		int rightX = graphics.guiWidth() - Math.round(graphics.guiWidth() * RIGHT_FRAC);
		int topY = Math.round(graphics.guiHeight() * TOP_FRAC);
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			int x = rightX - font.width(line);
			graphics.text(font, line, x, topY + i * LINE_HEIGHT, colors.get(i), true);
		}
	}
}
