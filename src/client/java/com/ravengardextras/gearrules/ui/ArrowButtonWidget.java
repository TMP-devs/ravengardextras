package com.ravengardextras.gearrules.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** A square up/down reorder button, hand-drawn as a triangle instead of relying on the font's ▲▼ glyphs. */
public class ArrowButtonWidget extends AbstractWidget {
	private final boolean pointingUp;
	private final Runnable onPress;

	public ArrowButtonWidget(int x, int y, int size, boolean pointingUp, Runnable onPress) {
		super(x, y, size, size, Component.literal(pointingUp ? "Up" : "Down"));
		this.pointingUp = pointingUp;
		this.onPress = onPress;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.onPress.run();
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x0 = this.getX();
		int y0 = this.getY();
		int x1 = x0 + this.getWidth();
		int y1 = y0 + this.getHeight();
		boolean hovered = this.isHovered();

		int bg = hovered ? 0xFF3A3155 : 0xFF1B1629;
		int border = hovered ? 0xFF9B7FE8 : 0xFF453A63;
		graphics.fill(x0, y0, x1, y1, bg);
		graphics.fill(x0, y0, x1, y0 + 1, border);
		graphics.fill(x0, y1 - 1, x1, y1, border);
		graphics.fill(x0, y0, x0 + 1, y1, border);
		graphics.fill(x1 - 1, y0, x1, y1, border);

		int cx = x0 + this.getWidth() / 2;
		int cy = y0 + this.getHeight() / 2;
		int color = 0xFFEDE8F5;
		if (this.pointingUp) {
			graphics.fill(cx, cy - 3, cx + 1, cy - 2, color);
			graphics.fill(cx - 1, cy - 2, cx + 2, cy - 1, color);
			graphics.fill(cx - 2, cy - 1, cx + 3, cy, color);
			graphics.fill(cx - 3, cy, cx + 4, cy + 1, color);
			graphics.fill(cx - 1, cy + 1, cx + 2, cy + 3, color);
		} else {
			graphics.fill(cx - 1, cy - 3, cx + 2, cy - 1, color);
			graphics.fill(cx - 3, cy - 1, cx + 4, cy, color);
			graphics.fill(cx - 2, cy, cx + 3, cy + 1, color);
			graphics.fill(cx - 1, cy + 1, cx + 2, cy + 2, color);
			graphics.fill(cx, cy + 2, cx + 1, cy + 3, color);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
	}
}
