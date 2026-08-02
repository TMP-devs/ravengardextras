package com.ravengardextras.dashboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/** One tab in the dashboard's top strip. Highlights when active or hovered. */
public class TabButtonWidget extends AbstractWidget {
	private final BooleanSupplier activeSupplier;
	private final Runnable onSelect;

	public TabButtonWidget(int x, int y, int width, int height, String label, BooleanSupplier activeSupplier, Runnable onSelect) {
		super(x, y, width, height, Component.literal(label));
		this.activeSupplier = activeSupplier;
		this.onSelect = onSelect;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.onSelect.run();
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x0 = this.getX();
		int y0 = this.getY();
		int x1 = x0 + this.getWidth();
		int y1 = y0 + this.getHeight();
		boolean active = this.activeSupplier.getAsBoolean();
		int fill = active ? DashboardColors.ACCENT : this.isHovered() ? DashboardColors.TAB_HOVER : DashboardColors.TAB_BG;
		int textColor = active ? 0xFF1A1522 : DashboardColors.TEXT_PRIMARY;

		graphics.fill(x0, y0, x1, y1, fill);
		graphics.fill(x0, y1 - 1, x1, y1, active ? 0xFFFFFFFF : DashboardColors.BORDER);

		Font font = Minecraft.getInstance().font;
		int tx = x0 + (this.getWidth() - font.width(this.getMessage())) / 2;
		int ty = y0 + (this.getHeight() - font.lineHeight) / 2;
		// The active tab's near-black text picks up a same-color drop shadow that smears
		// into it on the light accent background, so drop the shadow only in that state.
		graphics.text(font, this.getMessage(), tx, ty, textColor, !active);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
	}
}
