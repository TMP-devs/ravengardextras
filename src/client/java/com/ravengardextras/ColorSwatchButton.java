package com.ravengardextras;

import com.ravengardextras.gearhighlighter.GearHighlighterConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

/** Small clickable color square used by preset color pickers. Animates if it's the rainbow entry. */
public class ColorSwatchButton extends AbstractWidget {
	private final int argb;
	private final Consumer<Integer> onPick;
	private boolean selected;

	public ColorSwatchButton(int x, int y, int size, int argb, Consumer<Integer> onPick) {
		super(x, y, size, size, Component.literal(
				argb == GearHighlighterConfig.RAINBOW ? "Rainbow" : String.format("#%06X", argb & 0xFFFFFF)));
		this.argb = argb;
		this.onPick = onPick;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.onPick.accept(this.argb);
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x0 = this.getX();
		int y0 = this.getY();
		int x1 = x0 + this.getWidth();
		int y1 = y0 + this.getHeight();
		int borderColor = this.selected ? 0xFFFFFFFF : (this.isHovered() ? 0xFFAAAAAA : 0xFF000000);
		graphics.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, borderColor);
		int displayColor = this.argb;
		if (displayColor == GearHighlighterConfig.RAINBOW) {
			float hue = (System.currentTimeMillis() % 3000L) / 3000.0F;
			displayColor = 0xFF000000 | Mth.hsvToRgb(hue, 1.0F, 1.0F);
		}
		graphics.fill(x0, y0, x1, y1, displayColor);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
	}
}
