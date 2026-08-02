package com.ravengardextras.dashboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** A dashboard-styled push button (used for "Close", "Save", etc) instead of the vanilla gray button. */
public class PanelButtonWidget extends AbstractWidget {
	private final Runnable onPress;

	public PanelButtonWidget(int x, int y, int width, int height, String label, Runnable onPress) {
		super(x, y, width, height, Component.literal(label));
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

		graphics.fill(x0, y0, x1, y1, hovered ? DashboardColors.TAB_HOVER : DashboardColors.PANEL_SOFT);
		graphics.fill(x0, y0, x1, y0 + 1, hovered ? DashboardColors.ACCENT : DashboardColors.BORDER);
		graphics.fill(x0, y1 - 1, x1, y1, hovered ? DashboardColors.ACCENT : DashboardColors.BORDER);
		graphics.fill(x0, y0, x0 + 1, y1, hovered ? DashboardColors.ACCENT : DashboardColors.BORDER);
		graphics.fill(x1 - 1, y0, x1, y1, hovered ? DashboardColors.ACCENT : DashboardColors.BORDER);

		Font font = Minecraft.getInstance().font;
		int tx = x0 + (this.getWidth() - font.width(this.getMessage())) / 2;
		int ty = y0 + (this.getHeight() - font.lineHeight) / 2;
		graphics.text(font, this.getMessage(), tx, ty, DashboardColors.TEXT_PRIMARY);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
	}
}
