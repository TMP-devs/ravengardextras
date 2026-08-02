package com.ravengardextras.devtools;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Dev-only: tiny "D" button in the corner of any container screen. Copies all visible item data to clipboard. */
public class ItemDataDumpButton extends AbstractWidget {
	private static final int SIZE = 12;

	private final AbstractContainerScreen<?> screen;

	public ItemDataDumpButton(int x, int y, AbstractContainerScreen<?> screen) {
		super(x, y, SIZE, SIZE, Component.literal("D"));
		this.screen = screen;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		String dump = ItemDataDumper.dump(this.screen.getMenu());
		Minecraft client = Minecraft.getInstance();
		client.keyboardHandler.setClipboard(dump);
		if (client.player != null) {
			client.player.sendOverlayMessage(Component.literal("[RGE] Item data copied to clipboard."));
		}
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x0 = this.getX();
		int y0 = this.getY();
		int x1 = x0 + this.getWidth();
		int y1 = y0 + this.getHeight();
		graphics.fill(x0, y0, x1, y1, this.isHovered() ? 0xFFFFD700 : 0xFF7A5FE0);
		net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
		graphics.text(font, "D", x0 + 3, y0 + 2, 0xFF000000);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
	}
}
