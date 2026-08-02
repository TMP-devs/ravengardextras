package com.ravengardextras;

import com.ravengardextras.dashboard.CheckboxRowWidget;
import com.ravengardextras.dashboard.DashboardColors;
import com.ravengardextras.dashboard.DashboardTab;
import com.ravengardextras.dashboard.FeatureCardWidget;
import com.ravengardextras.dashboard.PanelButtonWidget;
import com.ravengardextras.dashboard.TabButtonWidget;
import com.ravengardextras.gearhighlighter.GearHighlighterConfig;
import com.ravengardextras.ping.PingColors;
import com.ravengardextras.ping.PingConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * RavengardExtras dashboard: a tabbed, expandable-card control panel in the style
 * of Isles+ (dark panel, tab strip, per-feature cards) but in our own black/purple
 * palette. Cards expand in place - toggling one rebuilds the active tab's widget
 * list, which is why every field a body needs to survive a rebuild (typed-but-
 * unsaved text, picked-but-unsaved colors) is a persisted instance field rather
 * than local state, and every EditBox uses setResponder to keep that field live.
 */
public class RavengardExtrasMenuScreen extends Screen {
	private static final int[] PRESET_COLORS = {
			0xFFFFFFFF, 0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55,
			0xFF55FF55, 0xFF55FFFF, 0xFF5555FF, 0xFFFF55FF,
			0xFFAA00AA, 0xFF000000, GearHighlighterConfig.RAINBOW,
	};

	private final Screen parent;
	private final GearHighlighterConfig config = RavengardExtrasClient.CONFIG;
	private final PingConfig pingConfig = RavengardExtrasClient.PING_CONFIG;

	private DashboardTab activeTab = DashboardTab.HIGHLIGHTERS;
	private final Set<String> expandedCards = new HashSet<>();
	private String statusError = "";

	// --- Gear Highlighter pending state (persists across incidental rebuilds) ---
	private final String[] gearThresholdText = new String[3];
	private final int[] gearTierColors = new int[3];
	private final EditBox[] gearThresholdBoxes = new EditBox[3];

	// --- Heal Highlighter pending state ---
	private int healWashColor;
	private final Set<String> healUncheckedPending;

	// --- Party Ping read-only preview position ---
	private int pingColorPreviewX;
	private int pingColorPreviewY;

	private int guiX, guiY, guiWidth, guiHeight;
	private int tabsX, tabsY, tabsWidth, tabsHeight;
	private int contentX, contentY, contentWidth;

	public RavengardExtrasMenuScreen(Screen parent) {
		super(Component.literal("RavengardExtras"));
		this.parent = parent;

		this.gearThresholdText[0] = Long.toString(this.config.tier1Threshold);
		this.gearThresholdText[1] = Long.toString(this.config.tier2Threshold);
		this.gearThresholdText[2] = Long.toString(this.config.tier3Threshold);
		this.gearTierColors[0] = this.config.tier1Color;
		this.gearTierColors[1] = this.config.tier2Color;
		this.gearTierColors[2] = this.config.tier3Color;

		this.healWashColor = this.config.healColor | 0xFF000000;
		this.healUncheckedPending = new HashSet<>(this.config.healUncheckedItems);
	}

	@Override
	protected void init() {
		int desiredWidth = Math.max(400, this.width - 80);
		int desiredHeight = Math.max(340, this.height - 60);
		this.guiWidth = Math.min(this.width - 20, desiredWidth);
		this.guiHeight = Math.min(this.height - 20, desiredHeight);
		this.guiX = (this.width - this.guiWidth) / 2;
		this.guiY = (this.height - this.guiHeight) / 2;

		this.tabsX = this.guiX + 18;
		this.tabsY = this.guiY + 40;
		this.tabsWidth = this.guiWidth - 36;
		this.tabsHeight = 22;

		this.contentX = this.guiX + 18;
		this.contentY = this.tabsY + this.tabsHeight + 12;
		this.contentWidth = this.guiWidth - 36;

		rebuild();
	}

	private void rebuild() {
		this.clearWidgets();

		DashboardTab[] tabs = DashboardTab.values();
		int gap = 8;
		int tabWidth = (this.tabsWidth - gap * (tabs.length - 1)) / tabs.length;
		for (int i = 0; i < tabs.length; i++) {
			DashboardTab tab = tabs[i];
			this.addRenderableWidget(new TabButtonWidget(this.tabsX + i * (tabWidth + gap), this.tabsY, tabWidth, this.tabsHeight,
					tab.label, () -> this.activeTab == tab, () -> {
				this.activeTab = tab;
				rebuild();
			}));
		}

		int y = this.contentY;
		for (CardDef card : cardsFor(this.activeTab)) {
			FeatureCardWidget header = new FeatureCardWidget(this.contentX, y, this.contentWidth, 44,
					card.label, card.description, card.icon, card.enabledSupplier, card.toggleAction,
					card.bodyBuilder != null, () -> this.expandedCards.contains(card.id), () -> toggleExpand(card.id));
			this.addRenderableWidget(header);
			y += 44;
			if (card.bodyBuilder != null && this.expandedCards.contains(card.id)) {
				y += 6;
				int bodyHeight = card.bodyBuilder.apply(this.contentX + 14, y);
				y += bodyHeight;
			}
			y += gap;
		}

		this.addRenderableWidget(new PanelButtonWidget(this.guiX + this.guiWidth / 2 - 40, this.guiY + this.guiHeight - 30, 80, 20,
				"Close", this::onClose));
	}

	private void toggleExpand(String id) {
		if (!this.expandedCards.remove(id)) {
			this.expandedCards.add(id);
		}
		rebuild();
	}

	/** A feature row: header info plus an optional body builder (x, y) -> height for expandable cards. */
	private record CardDef(String id, String label, String description, ItemStack icon,
	                        java.util.function.BooleanSupplier enabledSupplier,
	                        java.util.function.Consumer<Boolean> toggleAction,
	                        BiFunction<Integer, Integer, Integer> bodyBuilder) {
	}

	private List<CardDef> cardsFor(DashboardTab tab) {
		List<CardDef> cards = new ArrayList<>();
		switch (tab) {
			case HIGHLIGHTERS -> {
				cards.add(new CardDef("gear", "Gear Highlighter", "Outline gear by Crown value",
						new ItemStack(Items.GOLDEN_HELMET),
						() -> this.config.enabled, v -> { this.config.enabled = v; this.config.save(); },
						this::buildGearBody));
				cards.add(new CardDef("heal", "Heal Highlighter", "Tint healing items green",
						new ItemStack(Items.GOLDEN_APPLE),
						() -> this.config.healEnabled, v -> { this.config.healEnabled = v; this.config.save(); },
						this::buildHealBody));
			}
			case PARTY -> cards.add(new CardDef("ping", "Party Ping",
					"Ping (" + RavengardExtrasClient.pingKeyName() + ") / mark loot (" + RavengardExtrasClient.markKeyName() + ")",
					new ItemStack(Items.ENDER_EYE),
					() -> this.pingConfig.enabled, v -> { this.pingConfig.enabled = v; this.pingConfig.save(); },
					this::buildPingBody));
			case INVENTORY -> cards.add(new CardDef("slotlock", "Slot Locking",
					"Lock a slot with " + RavengardExtrasClient.lockSlotKeyName() + " to stop moving it",
					new ItemStack(Items.SHIELD),
					() -> RavengardExtrasClient.SLOT_LOCKER_CONFIG.enabled,
					v -> { RavengardExtrasClient.SLOT_LOCKER_CONFIG.enabled = v; RavengardExtrasClient.SLOT_LOCKER_CONFIG.save(); },
					null));
		}
		return cards;
	}

	// ============================== Gear Highlighter body ==============================

	private int buildGearBody(int x, int y) {
		int width = this.contentWidth - 28;
		int centerX = x + width / 2;
		int rowY = y;
		for (int tier = 0; tier < 3; tier++) {
			int tierIndex = tier;
			EditBox box = new EditBox(this.font, x, rowY, 110, 20, Component.literal("Tier " + (tier + 1) + " starts at"));
			box.setValue(this.gearThresholdText[tier]);
			box.setResponder(text -> this.gearThresholdText[tierIndex] = text);
			this.gearThresholdBoxes[tier] = box;
			this.addRenderableWidget(box);
			rowY += 24;

			addSwatchRow(centerX, rowY, this.gearTierColors[tier], color -> {
				this.gearTierColors[tierIndex] = color;
				rebuild();
			});
			rowY += 30;
		}
		rowY += 4;
		this.addRenderableWidget(new PanelButtonWidget(x, rowY, 72, 20, "Save", this::saveGear));
		rowY += 24;
		return rowY - y;
	}

	private long[] gearClampedThresholds() {
		long[] raw = new long[3];
		long[] fallback = {this.config.tier1Threshold, this.config.tier2Threshold, this.config.tier3Threshold};
		for (int tier = 0; tier < 3; tier++) {
			try {
				raw[tier] = Long.parseLong(this.gearThresholdText[tier].trim());
			} catch (NumberFormatException e) {
				raw[tier] = fallback[tier];
			}
		}
		for (int tier = 1; tier < 3; tier++) {
			if (raw[tier] < raw[tier - 1]) {
				raw[tier] = raw[tier - 1];
			}
		}
		return raw;
	}

	private void saveGear() {
		for (String text : this.gearThresholdText) {
			try {
				Long.parseLong(text.trim());
			} catch (NumberFormatException e) {
				this.statusError = "Threshold must be a whole number";
				return;
			}
		}
		long[] clamped = gearClampedThresholds();
		this.config.tier1Threshold = clamped[0];
		this.config.tier2Threshold = clamped[1];
		this.config.tier3Threshold = clamped[2];
		this.config.tier1Color = this.gearTierColors[0];
		this.config.tier2Color = this.gearTierColors[1];
		this.config.tier3Color = this.gearTierColors[2];
		this.config.save();
		this.statusError = "";
	}

	// ============================== Heal Highlighter body ==============================

	private int buildHealBody(int x, int y) {
		int width = this.contentWidth - 28;
		int centerX = x + width / 2;
		int rowY = y;

		addSwatchRow(centerX, rowY, this.healWashColor, color -> {
			this.healWashColor = color;
			rebuild();
		});
		rowY += 30;

		for (String name : this.config.healItemNames) {
			this.addRenderableWidget(new CheckboxRowWidget(x, rowY, width, 16, name,
					() -> !this.healUncheckedPending.contains(name),
					() -> {
						if (!this.healUncheckedPending.remove(name)) {
							this.healUncheckedPending.add(name);
						}
						rebuild();
					}));
			rowY += 18;
		}
		rowY += 4;

		this.addRenderableWidget(new PanelButtonWidget(x, rowY, 72, 20, "Save", this::saveHeal));
		rowY += 24;
		return rowY - y;
	}

	private void saveHeal() {
		this.config.healColor = 0x50000000 | (this.healWashColor & 0x00FFFFFF);
		this.config.healUncheckedItems = new HashSet<>(this.healUncheckedPending);
		this.config.save();
	}

	// ============================== Party Ping body ==============================

	/** Read-only: usage hints (keybinds shown dynamically) plus the player's ping color. Nothing here is editable. */
	private int buildPingBody(int x, int y) {
		this.pingColorPreviewX = x;
		this.pingColorPreviewY = y + pingInstructionLines().length * 10 + 6;
		return (pingInstructionLines().length * 10) + 6 + 16;
	}

	private String[] pingInstructionLines() {
		return new String[]{
				"Press " + RavengardExtrasClient.pingKeyName() + " to ping a block (fades after "
						+ this.pingConfig.tempPingSeconds + "s)",
				"Press " + RavengardExtrasClient.markKeyName() + " to mark loot (stays until cleared)",
				"Hold " + RavengardExtrasClient.markKeyName() + " for " + this.pingConfig.clearAllHoldSeconds
						+ "s to clear all your marks",
		};
	}

	// ============================== Shared swatch row ==============================

	private void addSwatchRow(int centerX, int y, int currentColor, java.util.function.Consumer<Integer> onPick) {
		int size = 16;
		int spacing = 18;
		int totalWidth = PRESET_COLORS.length * spacing - (spacing - size);
		int startX = centerX - totalWidth / 2;
		for (int color : PRESET_COLORS) {
			ColorSwatchButton swatch = new ColorSwatchButton(startX, y, size, color, onPick);
			swatch.setSelected(color == currentColor);
			this.addRenderableWidget(swatch);
			startX += spacing;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x1 = this.guiX + this.guiWidth;
		int y1 = this.guiY + this.guiHeight;

		graphics.fill(0, 0, this.width, this.height, DashboardColors.BG);
		graphics.fill(this.guiX - 2, this.guiY - 2, x1 + 2, y1 + 2, DashboardColors.ACCENT_SOFT);
		graphics.fill(this.guiX, this.guiY, x1, y1, DashboardColors.PANEL);

		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		String title = "RavengardExtras";
		int titleWidth = this.font.width(title);
		int titleX = this.guiX + this.guiWidth / 2 - titleWidth / 2;
		int titleY = this.guiY + 12;
		long time = System.currentTimeMillis();
		int cursorX = titleX;
		for (int i = 0; i < title.length(); i++) {
			float hue = ((time / 6L + i * 40L) % 1000L) / 1000.0F;
			int color = 0xFF000000 | Mth.hsvToRgb(hue, 0.5F, 1.0F);
			String letter = String.valueOf(title.charAt(i));
			graphics.text(this.font, letter, cursorX, titleY, color);
			cursorX += this.font.width(letter);
		}

		if (this.activeTab == DashboardTab.HIGHLIGHTERS && this.expandedCards.contains("gear")) {
			long[] clamped = gearClampedThresholds();
			for (int tier = 0; tier < 3; tier++) {
				String range = tier == 2 ? clamped[tier] + "+" : clamped[tier] + " – " + (clamped[tier + 1] - 1);
				EditBox box = this.gearThresholdBoxes[tier];
				graphics.text(this.font, range, box.getX() + 120, box.getY() + 6, DashboardColors.TEXT_MUTED);
			}
		}

		if (this.activeTab == DashboardTab.PARTY && this.expandedCards.contains("ping")) {
			String[] lines = pingInstructionLines();
			for (int i = 0; i < lines.length; i++) {
				graphics.text(this.font, lines[i], this.pingColorPreviewX, this.pingColorPreviewY - (lines.length - i) * 10 - 6,
						DashboardColors.TEXT_MUTED);
			}

			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player != null) {
				String name = minecraft.player.getName().getString();
				int color = PingColors.preview(name);
				String label = "Your ping color:";
				int labelX = this.pingColorPreviewX;
				int labelY = this.pingColorPreviewY + 4;
				graphics.text(this.font, label, labelX, labelY, DashboardColors.TEXT_MUTED);
				int swatchX = labelX + this.font.width(label) + 6;
				graphics.fill(swatchX, labelY - 1, swatchX + 10, labelY + 9, 0xFFFFFFFF);
				graphics.fill(swatchX + 1, labelY, swatchX + 9, labelY + 8, color);
			}
		}

		if (!this.statusError.isEmpty()) {
			int ex = this.guiX + this.guiWidth / 2 - this.font.width(this.statusError) / 2;
			graphics.text(this.font, this.statusError, ex, this.guiY + this.guiHeight - 54, DashboardColors.TEXT_WARNING);
		}

		drawCredit(graphics, this.guiX, this.guiY, x1, y1);
	}

	private void drawCredit(GuiGraphicsExtractor graphics, int px, int py, int x1, int y1) {
		String prefix = "a mod by ";
		String name1 = "chrrisk";
		String separator = " & ";
		String name2 = "Scrolls";
		float scale = 0.65F;
		int fullWidth = this.font.width(prefix) + this.font.width(name1) + this.font.width(separator) + this.font.width(name2);
		int scaledWidth = (int) (fullWidth * scale);
		int drawX = x1 - scaledWidth - 8;
		int drawY = py + 6;

		graphics.pose().pushMatrix();
		graphics.pose().translate(drawX, drawY);
		graphics.pose().scale(scale, scale);

		graphics.text(this.font, prefix, 0, 0, 0x77FFFFFF);
		int cursorX = this.font.width(prefix);
		long time = System.currentTimeMillis();
		int rainbowIndex = 0;
		for (int i = 0; i < name1.length(); i++) {
			cursorX = drawRainbowLetter(graphics, name1.charAt(i), cursorX, time, rainbowIndex++);
		}

		graphics.text(this.font, separator, cursorX, 0, 0x77FFFFFF);
		cursorX += this.font.width(separator);

		for (int i = 0; i < name2.length(); i++) {
			cursorX = drawRainbowLetter(graphics, name2.charAt(i), cursorX, time, rainbowIndex++);
		}

		graphics.pose().popMatrix();
	}

	private int drawRainbowLetter(GuiGraphicsExtractor graphics, char c, int cursorX, long time, int rainbowIndex) {
		float hue = ((time / 4L + rainbowIndex * 60L) % 1000L) / 1000.0F;
		int letterColor = 0xFF000000 | Mth.hsvToRgb(hue, 1.0F, 1.0F);
		String letter = String.valueOf(c);
		graphics.text(this.font, letter, cursorX, 0, letterColor);
		return cursorX + this.font.width(letter);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(this.parent);
	}
}
