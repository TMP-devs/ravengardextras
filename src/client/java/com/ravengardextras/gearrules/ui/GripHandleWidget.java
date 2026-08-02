package com.ravengardextras.gearrules.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;

/** A drag handle drawn as a dot grid. Reports absolute pointer position through the drag, and on release. */
public class GripHandleWidget extends AbstractWidget {
	private final Runnable onStart;
	private final BiConsumer<Integer, Integer> onMove;
	private final BiConsumer<Integer, Integer> onEnd;

	public GripHandleWidget(int x, int y, int size, Runnable onStart, BiConsumer<Integer, Integer> onMove, BiConsumer<Integer, Integer> onEnd) {
		super(x, y, size, size, Component.literal("Drag to rearrange"));
		this.onStart = onStart;
		this.onMove = onMove;
		this.onEnd = onEnd;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.onStart.run();
		this.onMove.accept((int) event.x(), (int) event.y());
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
		this.onMove.accept((int) event.x(), (int) event.y());
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		this.onEnd.accept((int) event.x(), (int) event.y());
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x0 = this.getX();
		int y0 = this.getY();
		int size = this.getWidth();
		boolean hovered = this.isHovered();
		int dot = hovered ? 0xFFEDE8F5 : 0xFF8A7FA8;

		int cell = Math.max(2, size / 4);
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 2; col++) {
				int dx = x0 + size / 2 - cell - 2 + col * (cell + 4);
				int dy = y0 + size / 2 - cell * 3 / 2 - 2 + row * (cell + 3);
				graphics.fill(dx, dy, dx + cell, dy + cell, dot);
			}
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
	}
}
