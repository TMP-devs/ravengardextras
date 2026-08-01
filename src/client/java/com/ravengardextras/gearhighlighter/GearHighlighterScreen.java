package com.ravengardextras.gearhighlighter;

import com.ravengardextras.ColorSwatchButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Gear Highlighter feature screen. Each tier is a single "starts at" number;
 * tiers are kept in ascending order automatically (bumped up on save if you
 * type them out of order) so ranges never overlap, and a live preview shows
 * the actual N-to-N range each tier covers instead of just the raw number.
 */
public class GearHighlighterScreen extends Screen {
	private static final int[] PRESET_COLORS = {
			0xFFFFFFFF, 0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55,
			0xFF55FF55, 0xFF55FFFF, 0xFF5555FF, 0xFFFF55FF,
			0xFFAA00AA, 0xFF000000, GearHighlighterConfig.RAINBOW,
	};
	private static final int TIER_COUNT = 3;

	private final Screen parent;
	private final GearHighlighterConfig config;

	private final EditBox[] thresholdBoxes = new EditBox[TIER_COUNT];
	private final int[] tierColors = new int[TIER_COUNT];
	@SuppressWarnings("unchecked")
	private final List<ColorSwatchButton>[] tierSwatches = new List[TIER_COUNT];
	private String errorMessage = "";

	public GearHighlighterScreen(Screen parent, GearHighlighterConfig config) {
		super(Component.literal("Gear Highlighter"));
		this.parent = parent;
		this.config = config;
		this.tierColors[0] = this.config.tier1Color;
		this.tierColors[1] = this.config.tier2Color;
		this.tierColors[2] = this.config.tier3Color;
		for (int i = 0; i < TIER_COUNT; i++) {
			this.tierSwatches[i] = new ArrayList<>();
		}
	}

	private long getThreshold(int tier) {
		return switch (tier) {
			case 0 -> this.config.tier1Threshold;
			case 1 -> this.config.tier2Threshold;
			default -> this.config.tier3Threshold;
		};
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int rowY = this.height / 2 - 110;
		int gap = 24;

		this.addRenderableWidget(CycleButton.onOffBuilder(this.config.enabled)
				.create(centerX - 75, rowY, 150, 20, Component.literal("Outlines"),
						(button, value) -> this.config.enabled = value));
		rowY += gap;

		for (int tier = 0; tier < TIER_COUNT; tier++) {
			int tierIndex = tier;
			EditBox box = new EditBox(this.font, centerX - 95, rowY, 110, 20,
					Component.literal("Tier " + (tier + 1) + " starts at"));
			box.setValue(Long.toString(getThreshold(tier)));
			this.thresholdBoxes[tier] = box;
			this.addRenderableWidget(box);
			rowY += gap;

			this.tierSwatches[tier].clear();
			addSwatchRow(centerX, rowY, this.tierColors[tier], this.tierSwatches[tier], color -> {
				this.tierColors[tierIndex] = color;
				refreshSelection(this.tierSwatches[tierIndex], color);
			});
			rowY += gap + 6;
		}
		rowY += 10;

		this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> this.save())
				.bounds(centerX - 75, rowY, 72, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> this.onClose())
				.bounds(centerX + 3, rowY, 72, 20).build());
	}

	private void addSwatchRow(int centerX, int y, int currentColor, List<ColorSwatchButton> into, java.util.function.Consumer<Integer> onPick) {
		int size = 16;
		int spacing = 18;
		int totalWidth = PRESET_COLORS.length * spacing - (spacing - size);
		int startX = centerX - totalWidth / 2;
		for (int i = 0; i < PRESET_COLORS.length; i++) {
			int color = PRESET_COLORS[i];
			ColorSwatchButton swatch = new ColorSwatchButton(startX + i * spacing, y, size, color, picked -> onPick.accept(picked));
			swatch.setSelected(color == currentColor);
			into.add(swatch);
			this.addRenderableWidget(swatch);
		}
	}

	private static void refreshSelection(List<ColorSwatchButton> swatches, int selectedColor) {
		for (int i = 0; i < swatches.size(); i++) {
			swatches.get(i).setSelected(PRESET_COLORS[i] == selectedColor);
		}
	}

	/** Reads the boxes (falling back to current config value on a bad/empty entry) and bumps tiers upward so tier1 <= tier2 <= tier3. */
	private long[] readClampedThresholds() {
		long[] raw = new long[TIER_COUNT];
		for (int tier = 0; tier < TIER_COUNT; tier++) {
			try {
				raw[tier] = Long.parseLong(this.thresholdBoxes[tier].getValue().trim());
			} catch (NumberFormatException e) {
				raw[tier] = getThreshold(tier);
			}
		}
		for (int tier = 1; tier < TIER_COUNT; tier++) {
			if (raw[tier] < raw[tier - 1]) {
				raw[tier] = raw[tier - 1];
			}
		}
		return raw;
	}

	private void save() {
		try {
			for (EditBox box : this.thresholdBoxes) {
				Long.parseLong(box.getValue().trim());
			}
		} catch (NumberFormatException e) {
			this.errorMessage = "Threshold must be a whole number";
			return;
		}

		long[] clamped = readClampedThresholds();
		this.config.tier1Threshold = clamped[0];
		this.config.tier2Threshold = clamped[1];
		this.config.tier3Threshold = clamped[2];
		this.config.tier1Color = this.tierColors[0];
		this.config.tier2Color = this.tierColors[1];
		this.config.tier3Color = this.tierColors[2];
		this.config.save();
		this.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int centerX = this.width / 2;
		graphics.text(this.font, this.title, centerX - this.font.width(this.title) / 2, this.height / 2 - 130, 0xFFFFFF);

		long[] clamped = readClampedThresholds();
		for (int tier = 0; tier < TIER_COUNT; tier++) {
			String range = tier == TIER_COUNT - 1
					? clamped[tier] + "+"
					: clamped[tier] + " – " + (clamped[tier + 1] - 1);
			int boxY = this.thresholdBoxes[tier].getY();
			graphics.text(this.font, Component.literal(range), this.thresholdBoxes[tier].getX() + 120, boxY + 6, 0xAAAAAA);
		}

		if (!this.errorMessage.isEmpty()) {
			graphics.text(this.font, this.errorMessage, centerX - this.font.width(this.errorMessage) / 2, this.height / 2 + 100, 0xFF5555);
		}
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(this.parent);
	}
}
