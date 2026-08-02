package com.ravengardextras.dashboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Collapsed header row for a dashboard feature card: icon, title, subtitle, an
 * on/off pill toggle, and (if expandable) a chevron. Clicking the toggle flips
 * the feature; clicking anywhere else in the row expands/collapses the card.
 * Expansion itself is owned by the screen (it has to reflow sibling widgets),
 * this widget only reports where the click landed.
 */
public class FeatureCardWidget extends AbstractWidget {
	private static final int TOGGLE_W = 42;
	private static final int TOGGLE_H = 16;

	private final ItemStack icon;
	private final String description;
	private final String badge;
	private final BooleanSupplier enabledSupplier;
	private final Consumer<Boolean> toggleAction;
	private final boolean expandable;
	private final BooleanSupplier expandedSupplier;
	private final Runnable onHeaderClick;

	public FeatureCardWidget(int x, int y, int width, int height, String label, String description,
	                          ItemStack icon, BooleanSupplier enabledSupplier, Consumer<Boolean> toggleAction,
	                          boolean expandable, BooleanSupplier expandedSupplier, Runnable onHeaderClick) {
		this(x, y, width, height, label, description, null, icon, enabledSupplier, toggleAction,
				expandable, expandedSupplier, onHeaderClick);
	}

	/** Same as above but with a small colored badge (e.g. "experimental") drawn after the label. */
	public FeatureCardWidget(int x, int y, int width, int height, String label, String description, String badge,
	                          ItemStack icon, BooleanSupplier enabledSupplier, Consumer<Boolean> toggleAction,
	                          boolean expandable, BooleanSupplier expandedSupplier, Runnable onHeaderClick) {
		super(x, y, width, height, Component.literal(label));
		this.description = description;
		this.badge = badge;
		this.icon = icon;
		this.enabledSupplier = enabledSupplier;
		this.toggleAction = toggleAction;
		this.expandable = expandable;
		this.expandedSupplier = expandedSupplier;
		this.onHeaderClick = onHeaderClick;
	}

	private int toggleX() {
		return this.getX() + this.getWidth() - TOGGLE_W - 10;
	}

	private int toggleY() {
		return this.getY() + (this.getHeight() - TOGGLE_H) / 2;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		int tx = toggleX();
		int ty = toggleY();
		boolean onToggle = event.x() >= tx && event.x() < tx + TOGGLE_W && event.y() >= ty && event.y() < ty + TOGGLE_H;
		if (onToggle || !this.expandable) {
			this.toggleAction.accept(!this.enabledSupplier.getAsBoolean());
			return;
		}
		this.onHeaderClick.run();
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x0 = this.getX();
		int y0 = this.getY();
		int x1 = x0 + this.getWidth();
		int y1 = y0 + this.getHeight();
		boolean hovered = this.isHovered();
		boolean enabled = this.enabledSupplier.getAsBoolean();

		graphics.fill(x0, y0, x1, y1, hovered ? DashboardColors.PANEL_HOVER : DashboardColors.PANEL_SOFT);
		graphics.fill(x0, y0, x1, y0 + 1, DashboardColors.BORDER);
		graphics.fill(x0, y1 - 1, x1, y1, DashboardColors.BORDER);

		int iconX = x0 + 10;
		int iconY = y0 + (this.getHeight() - 16) / 2;
		graphics.item(this.icon, iconX, iconY);

		Font font = Minecraft.getInstance().font;
		int textX = iconX + 24;
		graphics.text(font, this.getMessage(), textX, y0 + 8, DashboardColors.TEXT_PRIMARY);
		graphics.text(font, this.description, textX, y0 + 19, DashboardColors.TEXT_MUTED);
		if (this.badge != null) {
			int badgeX = textX + font.width(this.getMessage()) + 6;
			graphics.pose().pushMatrix();
			graphics.pose().translate(badgeX, y0 + 9);
			graphics.pose().scale(0.75F, 0.75F);
			graphics.text(font, this.badge, 0, 0, 0xFFFF5555);
			graphics.pose().popMatrix();
		}

		if (this.expandable) {
			drawChevron(graphics, x0 + 6, y0 + this.getHeight() / 2, this.expandedSupplier.getAsBoolean());
		}

		drawToggle(graphics, toggleX(), toggleY(), enabled);
	}

	private static void drawToggle(GuiGraphicsExtractor graphics, int x, int y, boolean enabled) {
		graphics.fill(x, y, x + TOGGLE_W, y + TOGGLE_H, enabled ? DashboardColors.POSITIVE : 0xFF443A5C);
		int knobSize = TOGGLE_H - 4;
		int knobX = enabled ? x + TOGGLE_W - knobSize - 2 : x + 2;
		graphics.fill(knobX, y + 2, knobX + knobSize, y + 2 + knobSize, 0xFFFFFFFF);
	}

	private static void drawChevron(GuiGraphicsExtractor graphics, int cx, int cy, boolean down) {
		int color = DashboardColors.TEXT_MUTED;
		if (down) {
			graphics.fill(cx - 3, cy - 1, cx + 4, cy, color);
			graphics.fill(cx - 2, cy, cx + 3, cy + 1, color);
			graphics.fill(cx - 1, cy + 1, cx + 2, cy + 2, color);
			graphics.fill(cx, cy + 2, cx + 1, cy + 3, color);
		} else {
			graphics.fill(cx - 1, cy - 3, cx, cy + 4, color);
			graphics.fill(cx, cy - 2, cx + 1, cy + 3, color);
			graphics.fill(cx + 1, cy - 1, cx + 2, cy + 2, color);
			graphics.fill(cx + 2, cy, cx + 3, cy + 1, color);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
	}
}
