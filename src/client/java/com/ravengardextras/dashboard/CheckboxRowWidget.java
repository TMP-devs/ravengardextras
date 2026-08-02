package com.ravengardextras.dashboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/** One label + checkbox row, same visual language Isles+ uses for its expandable-card option lists. */
public class CheckboxRowWidget extends AbstractWidget {
	private static final int BOX_SIZE = 12;

	private final BooleanSupplier checkedSupplier;
	private final Runnable onToggle;

	public CheckboxRowWidget(int x, int y, int width, int height, String label, BooleanSupplier checkedSupplier, Runnable onToggle) {
		super(x, y, width, height, Component.literal(label));
		this.checkedSupplier = checkedSupplier;
		this.onToggle = onToggle;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.onToggle.run();
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		Font font = Minecraft.getInstance().font;
		int y0 = this.getY();
		int textY = y0 + (this.getHeight() - font.lineHeight) / 2;
		graphics.text(font, this.getMessage(), this.getX(), textY, DashboardColors.TEXT_PRIMARY);

		int cbX = this.getX() + this.getWidth() - BOX_SIZE;
		int cbY = y0 + (this.getHeight() - BOX_SIZE) / 2;
		graphics.fill(cbX, cbY, cbX + BOX_SIZE, cbY + BOX_SIZE, DashboardColors.BORDER);
		graphics.fill(cbX + 1, cbY + 1, cbX + BOX_SIZE - 1, cbY + BOX_SIZE - 1, DashboardColors.PANEL_SOFT);
		if (this.checkedSupplier.getAsBoolean()) {
			graphics.fill(cbX + 2, cbY + 2, cbX + BOX_SIZE - 2, cbY + BOX_SIZE - 2, DashboardColors.POSITIVE);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
	}
}
