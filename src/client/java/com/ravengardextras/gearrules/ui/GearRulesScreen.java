package com.ravengardextras.gearrules.ui;

import com.ravengardextras.RavengardExtrasClient;
import com.ravengardextras.dashboard.CheckboxRowWidget;
import com.ravengardextras.dashboard.DashboardColors;
import com.ravengardextras.dashboard.PanelButtonWidget;
import com.ravengardextras.dashboard.ScrollState;
import com.ravengardextras.dashboard.TabButtonWidget;
import com.ravengardextras.gearrules.GearCard;
import com.ravengardextras.gearrules.GearClass;
import com.ravengardextras.gearrules.GearCondition;
import com.ravengardextras.gearrules.GearParam;
import com.ravengardextras.gearrules.GearPreset;
import com.ravengardextras.gearrules.GearRulesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Up to 4 preset tabs, each holding its own reorderable highlight-rule cards. The card list
 * is scrollable: it's built at natural (0-based) y, then shifted into the viewport and clipped
 * to whole rows in {@link #layoutScroll} - same approach as the main dashboard screen.
 */
public class GearRulesScreen extends Screen {
	private static final int SWATCH_SIZE = 20;

	private final Screen parent;
	private final GearRulesConfig config = RavengardExtrasClient.GEAR_RULES;

	private final Set<String> expandedCards = new HashSet<>();
	private final Map<String, String> minText = new HashMap<>();
	private final Map<String, String> maxText = new HashMap<>();
	/** {card, x, y} in natural (pre-scroll) coordinates - shifted to screen space at draw time. */
	private final List<Object[]> headerSwatches = new ArrayList<>();
	private final List<Object[]> labelDraws = new ArrayList<>();
	/** {x, y, width} in natural coordinates, underlined between conditions inside an expanded card body. */
	private final List<int[]> conditionDividers = new ArrayList<>();

	/** uiId of the condition whose param dropdown is currently open, or null. */
	private String openDropdownFor;
	/** Bottom-left of the param button that opened the dropdown, so the list appears right under it. */
	private int dropdownAnchorX, dropdownAnchorY;
	private int[] dropdownBox;

	// --- Scrollable card list ---
	private final ScrollState scroll = new ScrollState();
	private final List<AbstractWidget> scrollableWidgets = new ArrayList<>();
	private int scrollViewTop, scrollViewBottom;

	private int guiX, guiY, guiWidth, guiHeight;
	private int contentX, contentY, contentWidth;

	public GearRulesScreen(Screen parent) {
		super(Component.literal("Highlight Rules"));
		this.parent = parent;
	}

	private GearPreset active() {
		return this.config.active();
	}

	@Override
	protected void init() {
		this.guiWidth = Math.min(this.width - 20, Math.max(520, this.width - 80));
		this.guiHeight = Math.min(this.height - 20, Math.max(380, this.height - 60));
		this.guiX = (this.width - this.guiWidth) / 2;
		this.guiY = (this.height - this.guiHeight) / 2;
		this.contentX = this.guiX + 18;
		this.contentY = this.guiY + 66;
		this.contentWidth = this.guiWidth - 36;
		this.scrollViewTop = this.contentY + 26;
		this.scrollViewBottom = this.guiY + this.guiHeight - 40;
		rebuild();
	}

	private void rebuild() {
		this.clearWidgets();
		this.headerSwatches.clear();
		this.labelDraws.clear();
		this.conditionDividers.clear();
		this.scrollableWidgets.clear();

		if (this.openDropdownFor != null) {
			buildDropdownOverlay();
			return;
		}

		buildPresetTabs();

		GearPreset preset = active();
		this.addRenderableWidget(new CheckboxRowWidget(this.contentX, this.contentY, this.contentWidth, 16,
				"Enable in-game (the open tab above is what applies)",
				() -> this.config.enabled, () -> { this.config.enabled = !this.config.enabled; this.config.save(); rebuild(); }));

		int y = 0;
		for (int i = 0; i < preset.cards.size(); i++) {
			GearCard card = preset.cards.get(i);
			y = buildCardHeader(preset, card, i, y);
			if (this.expandedCards.contains(card.id)) {
				y += 6;
				y = buildCardBody(card, y);
				y += 8;
			}
			y += 10;
		}

		addScrollable(new PanelButtonWidget(this.contentX, y, 100, 20, "+ New Card", () -> {
			GearCard card = new GearCard();
			preset.cards.add(card);
			this.config.save();
			this.expandedCards.add(card.id);
			rebuild();
		}));
		y += 24;

		layoutScroll(y);

		this.addRenderableWidget(new PanelButtonWidget(this.guiX + this.guiWidth / 2 - 40, this.guiY + this.guiHeight - 30, 80, 20,
				"Close", this::onClose));
	}

	/** Adds a widget built at natural (0-based) y to the scrollable content set instead of directly to the screen. */
	private <T extends AbstractWidget> T addScrollable(T widget) {
		this.scrollableWidgets.add(widget);
		this.addRenderableWidget(widget);
		return widget;
	}

	/** Shifts every scrollable widget (and the manually-drawn decorations) from natural y into the viewport. */
	private void layoutScroll(int contentHeight) {
		int viewportHeight = this.scrollViewBottom - this.scrollViewTop;
		this.scroll.clamp(contentHeight, viewportHeight);
		int shift = this.scrollViewTop - this.scroll.offset();
		for (AbstractWidget widget : this.scrollableWidgets) {
			int newY = widget.getY() + shift;
			widget.setY(newY);
			widget.visible = newY >= this.scrollViewTop && newY + widget.getHeight() <= this.scrollViewBottom;
		}
	}

	private int scrollShift() {
		return this.scrollViewTop - this.scroll.offset();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		this.scroll.nudge(scrollY);
		rebuild();
		return true;
	}

	// ============================== Param dropdown overlay ==============================

	private GearCondition findConditionByUiId(String uiId) {
		for (GearCard card : active().cards) {
			for (GearCondition condition : card.conditions) {
				if (condition.uiId.equals(uiId)) {
					return condition;
				}
			}
		}
		return null;
	}

	private void buildDropdownOverlay() {
		GearCondition condition = findConditionByUiId(this.openDropdownFor);
		if (condition == null) {
			this.openDropdownFor = null;
			rebuild();
			return;
		}

		int boxWidth = 220;
		int rowHeight = 22;
		GearParam[] params = GearParam.values();
		int boxHeight = 34 + params.length * (rowHeight + 4);
		// Opens right under the button that was clicked, clamped so it never spills outside the panel.
		int bx = Math.min(this.dropdownAnchorX, this.guiX + this.guiWidth - boxWidth - 10);
		int by = Math.min(this.dropdownAnchorY, this.guiY + this.guiHeight - boxHeight - 10);
		bx = Math.max(bx, this.guiX + 10);
		by = Math.max(by, this.guiY + 10);
		this.dropdownBox = new int[]{bx, by, boxWidth, boxHeight};

		int y = by + 30;
		for (GearParam param : params) {
			this.addRenderableWidget(new PanelButtonWidget(bx + 10, y, boxWidth - 20, rowHeight, param.label, () -> {
				if (condition.param != param) {
					condition.min = 0;
					condition.max = null;
					condition.classes.clear();
				}
				condition.param = param;
				this.openDropdownFor = null;
				this.config.save();
				rebuild();
			}));
			y += rowHeight + 4;
		}

		this.addRenderableWidget(new PanelButtonWidget(bx + boxWidth / 2 - 30, y + 2, 60, 18, "Cancel", () -> {
			this.openDropdownFor = null;
			rebuild();
		}));
	}

	// ============================== Preset tabs ==============================

	private void buildPresetTabs() {
		int tabsY = this.guiY + 30;
		int tabsWidth = this.contentWidth;
		int gap = 8;
		int tabWidth = (tabsWidth - gap * (GearRulesConfig.PRESET_COUNT - 1)) / GearRulesConfig.PRESET_COUNT;
		for (int i = 0; i < GearRulesConfig.PRESET_COUNT; i++) {
			int index = i;
			GearPreset preset = this.config.presets.get(i);
			this.addRenderableWidget(new TabButtonWidget(this.contentX + i * (tabWidth + gap), tabsY, tabWidth, 20,
					preset.name, () -> this.config.activePresetIndex == index, () -> {
				this.config.activePresetIndex = index;
				this.config.save();
				this.scroll.reset();
				rebuild();
			}));
		}
	}

	// ============================== Card list ==============================

	private int buildCardHeader(GearPreset preset, GearCard card, int index, int y) {
		int rowHeight = SWATCH_SIZE;
		int x = this.contentX;

		this.headerSwatches.add(new Object[]{card, x, y});

		boolean expanded = this.expandedCards.contains(card.id);
		String label = card.name + "  (" + card.conditions.size() + " condition" + (card.conditions.size() == 1 ? "" : "s") + ")"
				+ (expanded ? "  ▲" : "  ▼");
		int labelX = x + SWATCH_SIZE + 8;
		addScrollable(new PanelButtonWidget(labelX, y, this.contentWidth - (labelX - x) - 96, rowHeight, label, () -> {
			if (!this.expandedCards.remove(card.id)) {
				this.expandedCards.add(card.id);
			}
			rebuild();
		}));

		int bx = x + this.contentWidth - 90;
		addScrollable(new ArrowButtonWidget(bx, y, rowHeight, true, () -> moveCard(preset, index, -1)));
		addScrollable(new ArrowButtonWidget(bx + rowHeight + 4, y, rowHeight, false, () -> moveCard(preset, index, 1)));
		addScrollable(new RemoveButtonWidget(bx + (rowHeight + 4) * 2, y, rowHeight, () -> {
			preset.cards.remove(card);
			this.expandedCards.remove(card.id);
			this.config.save();
			rebuild();
		}));

		return y + rowHeight;
	}

	private void moveCard(GearPreset preset, int index, int delta) {
		int target = index + delta;
		if (target < 0 || target >= preset.cards.size()) {
			return;
		}
		GearCard card = preset.cards.remove(index);
		preset.cards.add(target, card);
		this.config.save();
		rebuild();
	}

	private int buildCardBody(GearCard card, int y) {
		int leftX = this.contentX + 14;
		int leftWidth = (int) (this.contentWidth * 0.56) - 20;
		int rightX = this.contentX + (int) (this.contentWidth * 0.56);

		EditBox nameBox = new EditBox(this.font, leftX, y, leftWidth, 16, Component.literal("Card name"));
		nameBox.setValue(card.name);
		nameBox.setResponder(text -> { card.name = text; this.config.save(); });
		addScrollable(nameBox);
		int condY = y + 24;

		List<GearCondition> conditions = new ArrayList<>(card.conditions);
		for (int i = 0; i < conditions.size(); i++) {
			condY = buildConditionRow(card, conditions.get(i), leftX, condY, leftWidth);
			if (i < conditions.size() - 1) {
				condY += 7;
				this.conditionDividers.add(new int[]{leftX, condY, leftWidth});
				condY += 7;
			}
		}

		condY += conditions.isEmpty() ? 0 : 6;
		addScrollable(new PanelButtonWidget(leftX, condY, leftWidth, 18, "+ Add Parameter", () -> {
			card.conditions.add(new GearCondition());
			this.config.save();
			rebuild();
		}));
		condY += 22;

		int rightY = y;
		ColorWheelWidget wheel = new ColorWheelWidget(rightX, rightY, card.color, argb -> {
			card.color = argb;
			card.rainbow = false;
		});
		addScrollable(wheel);
		rightY += 64 + 10;

		addScrollable(new TabButtonWidget(rightX, rightY, 110, 18, "RGB (Rainbow)",
				() -> card.rainbow, () -> { card.rainbow = !card.rainbow; this.config.save(); rebuild(); }));
		rightY += 26;

		return Math.max(condY, rightY);
	}

	private int buildConditionRow(GearCard card, GearCondition condition, int x, int y, int width) {
		int rowHeight = 20;
		int paramWidth = 150;

		addScrollable(new PanelButtonWidget(x, y, paramWidth, rowHeight, condition.param.label + "  ▾", () -> {
			this.openDropdownFor = condition.uiId;
			this.dropdownAnchorX = x;
			this.dropdownAnchorY = y + scrollShift() + rowHeight + 4;
			rebuild();
		}));

		int removeX = x + width - rowHeight;

		if (condition.param == GearParam.CLASS) {
			addScrollable(new RemoveButtonWidget(removeX, y, rowHeight, () -> {
				card.conditions.remove(condition);
				this.config.save();
				rebuild();
			}));

			int chipY = y + rowHeight + 6;
			int chipGap = 6;
			int chipWidth = (width - chipGap * (GearClass.values().length - 1)) / GearClass.values().length;
			int chipX = x;
			for (GearClass gearClass : GearClass.values()) {
				addScrollable(new TabButtonWidget(chipX, chipY, chipWidth, rowHeight, gearClass.label,
						() -> condition.classes.contains(gearClass), () -> {
					if (!condition.classes.remove(gearClass)) {
						condition.classes.add(gearClass);
					}
					this.config.save();
					rebuild();
				}));
				chipX += chipWidth + chipGap;
			}
			return chipY + rowHeight;
		}

		int afterParamX = x + paramWidth + 20;
		this.labelDraws.add(new Object[]{"≥", afterParamX - 12, y + 5, DashboardColors.TEXT_MUTED});

		EditBox minBox = new EditBox(this.font, afterParamX, y, 50, rowHeight, Component.literal("min"));
		minBox.setValue(this.minText.computeIfAbsent(condition.uiId, k -> trimNumber(condition.min)));
		minBox.setResponder(text -> {
			this.minText.put(condition.uiId, text);
			try {
				condition.min = Double.parseDouble(text.trim());
				this.config.save();
			} catch (NumberFormatException ignored) {
				// keep typing, don't clobber the last valid value
			}
		});
		addScrollable(minBox);

		int afterMinX = afterParamX + 50 + 20;
		if (condition.param.isRange()) {
			this.labelDraws.add(new Object[]{"≤", afterMinX - 12, y + 5, DashboardColors.TEXT_MUTED});
			EditBox maxBox = new EditBox(this.font, afterMinX, y, 50, rowHeight, Component.literal("max"));
			maxBox.setValue(this.maxText.computeIfAbsent(condition.uiId, k -> condition.max == null ? "" : trimNumber(condition.max)));
			maxBox.setHint(Component.literal("∞"));
			maxBox.setResponder(text -> {
				this.maxText.put(condition.uiId, text);
				if (text.trim().isEmpty()) {
					condition.max = null;
					this.config.save();
					return;
				}
				try {
					condition.max = Double.parseDouble(text.trim());
					this.config.save();
				} catch (NumberFormatException ignored) {
					// keep typing
				}
			});
			addScrollable(maxBox);
		}

		addScrollable(new RemoveButtonWidget(removeX, y, rowHeight, () -> {
			card.conditions.remove(condition);
			this.config.save();
			rebuild();
		}));

		return y + rowHeight;
	}

	private static String trimNumber(double value) {
		if (value == Math.floor(value) && !Double.isInfinite(value)) {
			return Long.toString((long) value);
		}
		return Double.toString(value);
	}

	// ============================== Focus handling ==============================

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		boolean handled = super.mouseClicked(event, doubleClick);
		if (!handled) {
			// Clicking empty space should let go of whatever text field you were editing,
			// same as clicking any other widget already does.
			this.setFocused((GuiEventListener) null);
		}
		return handled;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.openDropdownFor != null && event.key() == 256) { // Escape closes just the dropdown
			this.openDropdownFor = null;
			rebuild();
			return true;
		}
		if (getFocused() instanceof EditBox && (event.key() == 257 || event.key() == 335)) { // Enter / numpad Enter
			this.setFocused((GuiEventListener) null);
			return true;
		}
		return super.keyPressed(event);
	}

	// ============================== Render ==============================

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x1 = this.guiX + this.guiWidth;
		int y1 = this.guiY + this.guiHeight;

		graphics.fill(0, 0, this.width, this.height, DashboardColors.BG);
		graphics.fill(this.guiX - 2, this.guiY - 2, x1 + 2, y1 + 2, DashboardColors.ACCENT_SOFT);
		graphics.fill(this.guiX, this.guiY, x1, y1, DashboardColors.PANEL);

		if (this.openDropdownFor != null && this.dropdownBox != null) {
			int bx = this.dropdownBox[0];
			int by = this.dropdownBox[1];
			int bw = this.dropdownBox[2];
			int bh = this.dropdownBox[3];
			graphics.fill(bx - 2, by - 2, bx + bw + 2, by + bh + 2, DashboardColors.ACCENT_SOFT);
			graphics.fill(bx, by, bx + bw, by + bh, DashboardColors.PANEL_SOFT);
			String title = "Select a Parameter";
			graphics.text(this.font, title, bx + bw / 2 - this.font.width(title) / 2, by + 10, DashboardColors.TEXT_PRIMARY);
		}

		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		int shift = scrollShift();
		for (int[] divider : this.conditionDividers) {
			int dy = divider[1] + shift;
			if (dy >= this.scrollViewTop && dy < this.scrollViewBottom) {
				graphics.fill(divider[0], dy, divider[0] + divider[2], dy + 1, DashboardColors.BORDER);
			}
		}

		for (Object[] entry : this.headerSwatches) {
			GearCard card = (GearCard) entry[0];
			int sx = (int) entry[1];
			int sy = (int) entry[2] + shift;
			if (sy < this.scrollViewTop || sy + SWATCH_SIZE > this.scrollViewBottom) {
				continue;
			}
			int color = card.rainbow ? rainbowPreviewColor() : card.color;
			graphics.fill(sx, sy, sx + SWATCH_SIZE, sy + SWATCH_SIZE, 0xFFFFFFFF);
			graphics.fill(sx + 1, sy + 1, sx + SWATCH_SIZE - 1, sy + SWATCH_SIZE - 1, color);
		}
		for (Object[] draw : this.labelDraws) {
			int dy = (int) draw[2] + shift;
			if (dy >= this.scrollViewTop - 10 && dy <= this.scrollViewBottom) {
				graphics.text(this.font, (String) draw[0], (int) draw[1], dy, (int) draw[3]);
			}
		}

		String title = "Gear Highlighter";
		int titleWidth = this.font.width(title);
		graphics.text(this.font, title, this.guiX + this.guiWidth / 2 - titleWidth / 2, this.guiY + 12, DashboardColors.TEXT_PRIMARY);
	}

	private static int rainbowPreviewColor() {
		float hue = (System.currentTimeMillis() % 2000L) / 2000.0F;
		return 0xFF000000 | net.minecraft.util.Mth.hsvToRgb(hue, 1.0F, 1.0F);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(this.parent);
	}
}
