package com.ravengardextras.gearrules.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** A square red delete button, drawn as an X rather than relying on the font glyph (which renders stretched/off-center). */
public class RemoveButtonWidget extends AbstractWidget {
	private final Runnable onPress;

	public RemoveButtonWidget(int x, int y, int size, Runnable onPress) {
		super(x, y, size, size, Component.literal("Remove"));
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

		int bg = hovered ? 0xFFE85D5D : 0xFF7A2E2E;
		int border = hovered ? 0xFFFFFFFF : 0xFFB84C4C;
		graphics.fill(x0, y0, x1, y1, bg);
		graphics.fill(x0, y0, x1, y0 + 1, border);
		graphics.fill(x0, y1 - 1, x1, y1, border);
		graphics.fill(x0, y0, x0 + 1, y1, border);
		graphics.fill(x1 - 1, y0, x1, y1, border);

		int cx = x0 + this.getWidth() / 2;
		int cy = y0 + this.getHeight() / 2;
		int r = Math.max(2, this.getWidth() / 2 - 4);
		int c = 0xFFFFFFFF;
		for (int i = -r; i <= r; i++) {
			graphics.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, c);
			graphics.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, c);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
	}
}
