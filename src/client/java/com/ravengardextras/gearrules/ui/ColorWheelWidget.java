package com.ravengardextras.gearrules.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.function.IntConsumer;

/** A saturation/value square plus a hue strip. Reports picked colors as opaque ARGB ints. */
public class ColorWheelWidget extends AbstractWidget {
	private static final int SQUARE_SIZE = 64;
	private static final int STRIP_GAP = 6;
	private static final int STRIP_WIDTH = 12;
	private static final int CELL = 8;

	private final IntConsumer onChange;
	private float hue;
	private float sat;
	private float val;

	public ColorWheelWidget(int x, int y, int argb, IntConsumer onChange) {
		super(x, y, SQUARE_SIZE + STRIP_GAP + STRIP_WIDTH, SQUARE_SIZE, Component.literal("Color"));
		this.onChange = onChange;
		float[] hsb = Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, null);
		this.hue = hsb[0];
		this.sat = hsb[1];
		this.val = hsb[2];
	}

	private int stripX() {
		return this.getX() + SQUARE_SIZE + STRIP_GAP;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		handlePointer(event.x(), event.y());
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
		handlePointer(event.x(), event.y());
	}

	private void handlePointer(double mouseX, double mouseY) {
		int x0 = this.getX();
		int y0 = this.getY();
		if (mouseX >= x0 && mouseX < x0 + SQUARE_SIZE && mouseY >= y0 && mouseY < y0 + SQUARE_SIZE) {
			this.sat = clamp01((float) ((mouseX - x0) / SQUARE_SIZE));
			this.val = clamp01(1.0F - (float) ((mouseY - y0) / SQUARE_SIZE));
			fireChange();
		} else if (mouseX >= stripX() && mouseX < stripX() + STRIP_WIDTH && mouseY >= y0 && mouseY < y0 + SQUARE_SIZE) {
			this.hue = clamp01((float) ((mouseY - y0) / SQUARE_SIZE));
			fireChange();
		}
	}

	private static float clamp01(float v) {
		return Math.max(0.0F, Math.min(1.0F, v));
	}

	private void fireChange() {
		int rgb = Color.HSBtoRGB(this.hue, this.sat, this.val);
		this.onChange.accept(0xFF000000 | (rgb & 0x00FFFFFF));
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x0 = this.getX();
		int y0 = this.getY();

		for (int cy = 0; cy < SQUARE_SIZE; cy += CELL) {
			for (int cx = 0; cx < SQUARE_SIZE; cx += CELL) {
				float s = clamp01((cx + CELL / 2.0F) / SQUARE_SIZE);
				float v = clamp01(1.0F - (cy + CELL / 2.0F) / SQUARE_SIZE);
				int rgb = Color.HSBtoRGB(this.hue, s, v);
				graphics.fill(x0 + cx, y0 + cy, x0 + cx + CELL, y0 + cy + CELL, 0xFF000000 | (rgb & 0x00FFFFFF));
			}
		}

		int selX = x0 + Math.round(this.sat * SQUARE_SIZE);
		int selY = y0 + Math.round((1.0F - this.val) * SQUARE_SIZE);
		graphics.fill(selX - 2, selY - 2, selX + 2, selY + 2, 0xFFFFFFFF);
		graphics.fill(selX - 1, selY - 1, selX + 1, selY + 1, 0xFF000000);

		int sx = stripX();
		int bands = 18;
		int bandHeight = Math.max(1, SQUARE_SIZE / bands);
		for (int i = 0; i < bands; i++) {
			float h = (float) i / bands;
			int rgb = Color.HSBtoRGB(h, 1.0F, 1.0F);
			graphics.fill(sx, y0 + i * bandHeight, sx + STRIP_WIDTH, y0 + (i + 1) * bandHeight, 0xFF000000 | (rgb & 0x00FFFFFF));
		}
		int markerY = y0 + Math.round(this.hue * SQUARE_SIZE);
		graphics.fill(sx - 1, markerY - 1, sx + STRIP_WIDTH + 1, markerY + 1, 0xFFFFFFFF);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
	}
}
