package com.ravengardextras;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

/** A styled feature-list row: icon, title, subtitle, and a live on/off status dot. */
public class FeatureButton extends AbstractWidget {
	private final ItemStack icon;
	private final String subtitle;
	private final BooleanSupplier statusSupplier;
	private final Runnable onClick;

	public FeatureButton(int x, int y, int width, int height, String title, String subtitle,
	                      ItemStack icon, BooleanSupplier statusSupplier, Runnable onClick) {
		super(x, y, width, height, Component.literal(title));
		this.subtitle = subtitle;
		this.icon = icon;
		this.statusSupplier = statusSupplier;
		this.onClick = onClick;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.onClick.run();
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x0 = this.getX();
		int y0 = this.getY();
		int x1 = x0 + this.getWidth();
		int y1 = y0 + this.getHeight();
		boolean hovered = this.isHovered();

		int bg = hovered ? 0xCC2A2A38 : 0xCC1A1A24;
		int border = hovered ? 0xFFFFD700 : 0xFF3A3A48;
		graphics.fill(x0, y0, x1, y1, border);
		graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, bg);
		graphics.fill(x0, y0, x0 + 3, y1, hovered ? 0xFFFFD700 : 0xFF7A5FE0);

		int iconX = x0 + 10;
		int iconY = y0 + (this.getHeight() - 16) / 2;
		graphics.item(this.icon, iconX, iconY);

		net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
		int textX = iconX + 22;
		graphics.text(font, this.getMessage(), textX, y0 + 6, 0xFFFFFFFF);
		graphics.text(font, this.subtitle, textX, y0 + 17, 0xFF9A9AAA);

		boolean on = this.statusSupplier.getAsBoolean();
		int dotColor = on ? 0xFF55FF55 : 0xFF8A8A8A;
		int dotX = x1 - 12;
		int dotY = y0 + this.getHeight() / 2 - 3;
		graphics.fill(dotX, dotY, dotX + 6, dotY + 6, dotColor);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
	}
}
