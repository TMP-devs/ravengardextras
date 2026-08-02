package com.ravengardextras.runtools;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the per-run tally in the top-left corner while a run is active: net Crowns
 * gained (gold) and XP earned (green), each shown only if its calculator is enabled.
 * A translucent backing panel keeps the text legible over the world.
 */
public final class RunHudRenderer {
	private static final Identifier ID = Identifier.fromNamespaceAndPath("ravengardextras", "run_hud");
	private static final int MARGIN = 4;
	private static final int PADDING = 4;
	private static final int LINE_HEIGHT = 10;
	private static final int CROWN_COLOR = 0xFFFFD24A;
	private static final int XP_COLOR = 0xFF66FF66;

	private RunHudRenderer() {
	}

	public static void register(RunTracker tracker, RunToolsConfig config) {
		HudElementRegistry.addLast(ID, (graphics, delta) -> render(graphics, tracker, config));
	}

	private static void render(net.minecraft.client.gui.GuiGraphicsExtractor graphics, RunTracker tracker, RunToolsConfig config) {
		if (!tracker.isRunActive()) {
			return;
		}

		List<String> lines = new ArrayList<>(2);
		List<Integer> colors = new ArrayList<>(2);
		if (config.crownCalcEnabled) {
			lines.add("Crowns: " + RunTracker.formatSigned(tracker.netCrowns()));
			colors.add(CROWN_COLOR);
		}
		if (config.xpCalcEnabled) {
			lines.add("XP: " + RunTracker.formatSigned(tracker.runXp()));
			colors.add(XP_COLOR);
		}
		if (lines.isEmpty()) {
			return;
		}

		Font font = Minecraft.getInstance().font;
		int textWidth = 0;
		for (String line : lines) {
			textWidth = Math.max(textWidth, font.width(line));
		}

		int x = MARGIN;
		int y = MARGIN;
		int panelW = textWidth + PADDING * 2;
		int panelH = lines.size() * LINE_HEIGHT + PADDING * 2 - (LINE_HEIGHT - 8);

		graphics.fill(x, y, x + panelW, y + panelH, 0x99000000);
		graphics.fill(x, y, x + panelW, y + 1, 0x33FFFFFF);

		int textX = x + PADDING;
		int textY = y + PADDING;
		for (int i = 0; i < lines.size(); i++) {
			graphics.text(font, lines.get(i), textX, textY + i * LINE_HEIGHT, colors.get(i));
		}
	}
}
