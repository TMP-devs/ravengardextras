package com.ravengardextras;

import com.ravengardextras.gearhighlighter.GearHighlighterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Feature list for the mod, styled as a small panel. Currently just Gear Highlighter; more rows slot in later. */
public class RavengardExtrasMenuScreen extends Screen {
	private static final int PANEL_WIDTH = 240;
	private static final int PANEL_HEIGHT = 192;

	private final Screen parent;
	private int comingSoonY;

	public RavengardExtrasMenuScreen(Screen parent) {
		super(Component.literal("RavengardExtras"));
		this.parent = parent;
	}

	private int panelX() {
		return (this.width - PANEL_WIDTH) / 2;
	}

	private int panelY() {
		return (this.height - PANEL_HEIGHT) / 2;
	}

	@Override
	protected void init() {
		int px = panelX();
		int py = panelY();
		int rowY = py + 34;

		this.addRenderableWidget(new FeatureButton(px + 12, rowY, PANEL_WIDTH - 24, 36,
				"Gear Highlighter", "Outline gear by Crown value",
				new ItemStack(Items.GOLDEN_HELMET),
				() -> RavengardExtrasClient.CONFIG.enabled,
				() -> Minecraft.getInstance().gui.setScreen(new GearHighlighterScreen(this, RavengardExtrasClient.CONFIG))));
		rowY += 44;

		this.comingSoonY = rowY;
		rowY += 30;

		this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose())
				.bounds(px + PANEL_WIDTH / 2 - 40, py + PANEL_HEIGHT - 40, 80, 18).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int px = panelX();
		int py = panelY();
		int x1 = px + PANEL_WIDTH;
		int y1 = py + PANEL_HEIGHT;

		// Panel: drawn before super() so the buttons/rows super renders land on top of it.
		graphics.fill(px - 2, py - 2, x1 + 2, y1 + 2, 0xFF7A5FE0);
		graphics.fillGradient(px, py, x1, y1, 0xF0181820, 0xF00E0E14);
		graphics.fill(px, py, x1, py + 1, 0x33FFFFFF);

		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		String title = "RavengardExtras";
		int titleWidth = this.font.width(title);
		int titleX = px + PANEL_WIDTH / 2 - titleWidth / 2;
		int titleY = py + 12;
		long time = System.currentTimeMillis();
		int cursorX = titleX;
		for (int i = 0; i < title.length(); i++) {
			float hue = ((time / 6L + i * 40L) % 1000L) / 1000.0F;
			int color = 0xFF000000 | Mth.hsvToRgb(hue, 0.55F, 1.0F);
			String letter = String.valueOf(title.charAt(i));
			graphics.text(this.font, letter, cursorX, titleY, color);
			cursorX += this.font.width(letter);
		}

		drawComingSoonCard(graphics, px, x1);
		drawCredit(graphics, px, py, x1, y1);
	}

	private void drawComingSoonCard(GuiGraphicsExtractor graphics, int px, int x1) {
		int cardX0 = px + 12;
		int cardX1 = x1 - 12;
		int cardY0 = this.comingSoonY;
		int cardY1 = cardY0 + 24;

		graphics.fill(cardX0, cardY0, cardX1, cardY1, 0xFF2A2A38);
		graphics.fill(cardX0, cardY0, cardX1, cardY0 + 1, 0xFF4A4A5A);
		graphics.fill(cardX0, cardY1 - 1, cardX1, cardY1, 0xFF4A4A5A);
		graphics.fill(cardX0, cardY0, cardX0 + 1, cardY1, 0xFF4A4A5A);
		graphics.fill(cardX1 - 1, cardY0, cardX1, cardY1, 0xFF4A4A5A);

		String text = "More features coming!";
		int textWidth = this.font.width(text);
		int textX = px + (x1 - px) / 2 - textWidth / 2;
		int textY = cardY0 + (24 - this.font.lineHeight) / 2;
		graphics.text(this.font, text, textX, textY, 0xFF9A9AAA);
	}

	private void drawCredit(GuiGraphicsExtractor graphics, int px, int py, int x1, int y1) {
		String prefix = "a mod by ";
		String name1 = "chrrisk";
		String separator = " & ";
		String name2 = "scrolls";
		float scale = 0.65F;
		int fullWidth = this.font.width(prefix) + this.font.width(name1) + this.font.width(separator) + this.font.width(name2);
		int scaledWidth = (int) (fullWidth * scale);
		int drawX = px + (x1 - px) / 2 - scaledWidth / 2;
		int drawY = y1 - 12;

		graphics.pose().pushMatrix();
		graphics.pose().translate(drawX, drawY);
		graphics.pose().scale(scale, scale);

		graphics.text(this.font, prefix, 0, 0, 0x77FFFFFF);
		int cursorX = this.font.width(prefix);
		long time = System.currentTimeMillis();
		int rainbowIndex = 0;
		for (int i = 0; i < name1.length(); i++) {
			cursorX = drawRainbowLetter(graphics, name1.charAt(i), cursorX, time, rainbowIndex++);
		}

		graphics.text(this.font, separator, cursorX, 0, 0x77FFFFFF);
		cursorX += this.font.width(separator);

		for (int i = 0; i < name2.length(); i++) {
			cursorX = drawRainbowLetter(graphics, name2.charAt(i), cursorX, time, rainbowIndex++);
		}

		graphics.pose().popMatrix();
	}

	private int drawRainbowLetter(GuiGraphicsExtractor graphics, char c, int cursorX, long time, int rainbowIndex) {
		float hue = ((time / 4L + rainbowIndex * 60L) % 1000L) / 1000.0F;
		int letterColor = 0xFF000000 | Mth.hsvToRgb(hue, 1.0F, 1.0F);
		String letter = String.valueOf(c);
		graphics.text(this.font, letter, cursorX, 0, letterColor);
		return cursorX + this.font.width(letter);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(this.parent);
	}
}
