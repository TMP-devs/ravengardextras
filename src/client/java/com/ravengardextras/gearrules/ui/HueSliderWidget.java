package com.ravengardextras.gearrules.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.function.IntConsumer;

/** A single horizontal hue bar (full saturation/value) - a compact stand-in for a full color wheel. */
public class HueSliderWidget extends AbstractWidget {
	private static final int HEIGHT = 14;

	private final IntConsumer onChange;
	private float hue;

	public HueSliderWidget(int x, int y, int width, int argb, IntConsumer onChange) {
		super(x, y, width, HEIGHT, Component.literal("Color"));
		this.onChange = onChange;
		float[] hsb = Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, null);
		this.hue = hsb[0];
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		handlePointer(event.x());
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
		handlePointer(event.x());
	}

	private void handlePointer(double mouseX) {
		this.hue = Math.max(0.0F, Math.min(1.0F, (float) ((mouseX - this.getX()) / this.getWidth())));
		int rgb = Color.HSBtoRGB(this.hue, 1.0F, 1.0F);
		this.onChange.accept(0xFF000000 | (rgb & 0x00FFFFFF));
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x0 = this.getX();
		int y0 = this.getY();
		int width = this.getWidth();
		int bands = Math.max(1, width / 3);
		int bandWidth = Math.max(1, width / bands);
		for (int i = 0; i < bands; i++) {
			float h = (float) i / bands;
			int rgb = Color.HSBtoRGB(h, 1.0F, 1.0F);
			graphics.fill(x0 + i * bandWidth, y0, x0 + (i + 1) * bandWidth, y0 + HEIGHT, 0xFF000000 | (rgb & 0x00FFFFFF));
		}
		int markerX = x0 + Math.round(this.hue * width);
		graphics.fill(markerX - 1, y0 - 1, markerX + 2, y0 + HEIGHT + 1, 0xFFFFFFFF);
		graphics.fill(markerX, y0, markerX + 1, y0 + HEIGHT, 0xFF000000);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
	}
}
