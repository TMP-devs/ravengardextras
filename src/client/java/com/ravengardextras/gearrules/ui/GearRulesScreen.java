package com.ravengardextras.gearrules.ui;

import com.ravengardextras.RavengardExtrasClient;
import com.ravengardextras.dashboard.DashboardColors;
import com.ravengardextras.dashboard.PanelButtonWidget;
import com.ravengardextras.dashboard.ScrollState;
import com.ravengardextras.dashboard.TabButtonWidget;
import com.ravengardextras.gearrules.GearCard;
import com.ravengardextras.gearrules.GearClass;
import com.ravengardextras.gearrules.GearCondition;
import com.ravengardextras.gearrules.GearConditionGroup;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Up to 4 preset tabs, each holding its own highlight-rule cards laid out as a 2-column grid.
 * Grid position is purely cosmetic (drag a card's grip onto another to swap places); match
 * precedence is the separate numeric {@code priority} field on each card, lowest evaluates first.
 * The card grid is scrollable: it's built at natural (0-based) y, then shifted into the viewport
 * and clipped to whole rows in {@link #layoutScroll} - same approach as the main dashboard screen.
 */
public class GearRulesScreen extends Screen {
	private static final int DESC_BAR_Y_OFFSET = 26;
	private static final int DESC_BAR_HEIGHT = 20;
	private static final int ADD_BUTTON_WIDTH = 110;
	private static final String DESCRIPTION = "Colors gear by the rules below - highest priority wins on overlap.";

	private final Screen parent;
	private final GearRulesConfig config = RavengardExtrasClient.GEAR_RULES;

	private final Map<String, String> minText = new HashMap<>();
	private final Map<String, String> maxText = new HashMap<>();
	private final Map<String, String> priorityText = new HashMap<>();
	private final List<Object[]> labelDraws = new ArrayList<>();
	/** {x, y, width} in natural coordinates, underlined between conditions inside a card body. */
	private final List<int[]> conditionDividers = new ArrayList<>();
	/** {x, y} in natural coordinates - a small square bullet point next to a describeLines() entry. */
	private final List<int[]> bulletDraws = new ArrayList<>();
	/** {card, x, y, width, height} in natural coordinates, used both to draw each tile's border and to hit-test drops. */
	private final List<Object[]> tileRects = new ArrayList<>();

	/** uiId of the condition whose param dropdown is currently open, or null. */
	private String openDropdownFor;
	private int[] dropdownBox;

	/** Card currently being dragged by its grip, or null. dragX/dragY are absolute screen coords. */
	private GearCard draggingCard;
	private int dragX, dragY;

	/** Card whose conditions/color are open in the edit modal, or null to show the summary grid. */
	private GearCard editingCard;
	/** {x, y, width, height} of the edit modal panel, in screen coords, for background-dim + click-outside-to-close hit-testing. */
	private int[] editModalBox;

	// --- Scrollable card grid ---
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
		this.guiWidth = Math.min(this.width - 20, Math.max(560, this.width - 80));
		this.guiHeight = Math.min(this.height - 20, Math.max(380, this.height - 60));
		this.guiX = (this.width - this.guiWidth) / 2;
		this.guiY = (this.height - this.guiHeight) / 2;
		this.contentX = this.guiX + 18;
		int tabsYOffset = DESC_BAR_Y_OFFSET + DESC_BAR_HEIGHT + 8;
		this.contentY = this.guiY + tabsYOffset + 20 + 12;
		this.contentWidth = this.guiWidth - 36;
		computeGridViewport();
		rebuild();
	}

	private void computeGridViewport() {
		this.scrollViewTop = this.contentY;
		this.scrollViewBottom = this.guiY + this.guiHeight - 40;
	}

	private void rebuild() {
		this.clearWidgets();
		this.labelDraws.clear();
		this.conditionDividers.clear();
		this.bulletDraws.clear();
		this.tileRects.clear();
		this.scrollableWidgets.clear();
		this.editModalBox = null;

		if (this.openDropdownFor != null) {
			buildDropdownOverlay();
			return;
		}

		GearPreset preset = active();
		if (this.editingCard != null && !preset.cards.contains(this.editingCard)) {
			this.editingCard = null;
		}

		if (this.editingCard != null) {
			// The modal owns the whole screen while open: no preset tabs, no add-card, no page Close -
			// only "Back to Cards" or clicking outside the modal exits it.
			buildCardEditModal(this.editingCard);
			return;
		}

		computeGridViewport();
		buildStickyHeader();
		buildCardGrid(preset);

		this.addRenderableWidget(new PanelButtonWidget(this.guiX + this.guiWidth / 2 - 40, this.guiY + this.guiHeight - 30, 80, 20,
				"Close", this::onClose));
	}

	private void buildCardGrid(GearPreset preset) {
		// A few px of headroom so the tile's own border (drawn slightly outside its bounds) isn't
		// sitting exactly on the scissor edge and getting sliced off under the sticky tabs.
		int y = 4;
		int gap = 12;
		int colWidth = (this.contentWidth - gap) / 2;
		for (int i = 0; i < preset.cards.size(); i += 2) {
			GearCard left = preset.cards.get(i);
			int leftTileIndex = this.tileRects.size();
			int leftHeight = buildCardTile(preset, left, this.contentX, y, colWidth);
			int rightTileIndex = this.tileRects.size();
			int rightHeight = 0;
			boolean hasRight = i + 1 < preset.cards.size();
			if (hasRight) {
				GearCard right = preset.cards.get(i + 1);
				rightHeight = buildCardTile(preset, right, this.contentX + colWidth + gap, y, colWidth);
			}
			// Both tiles in a row share the taller one's height so their boxes line up instead of one looking stubby.
			int rowHeight = Math.max(leftHeight, rightHeight);
			this.tileRects.get(leftTileIndex)[4] = rowHeight;
			if (hasRight) {
				this.tileRects.get(rightTileIndex)[4] = rowHeight;
			}
			y += rowHeight + gap;
		}

		layoutScroll(y);
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
			for (GearConditionGroup group : card.groups) {
				for (GearCondition condition : group.conditions) {
					if (condition.uiId.equals(uiId)) {
						return condition;
					}
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
		// Always centered on screen - anchoring it to the clicked button broke once that button could
		// live inside the scrollable grid or the edit modal, each with its own shifting coordinate space.
		int bx = this.width / 2 - boxWidth / 2;
		int by = this.height / 2 - boxHeight / 2;
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

	// ============================== Sticky header: title/add-card + preset tabs ==============================

	private void buildStickyHeader() {
		int descY = this.guiY + DESC_BAR_Y_OFFSET;
		int addX = this.guiX + this.guiWidth - 18 - ADD_BUTTON_WIDTH;
		this.addRenderableWidget(new PanelButtonWidget(addX, descY, ADD_BUTTON_WIDTH, DESC_BAR_HEIGHT, "+ Add Card", this::addNewCard));

		int sortWidth = 110;
		int sortX = addX - 8 - sortWidth;
		this.addRenderableWidget(new PanelButtonWidget(sortX, descY, sortWidth, DESC_BAR_HEIGHT, "Sort by Priority", this::sortByPriorityDescending));

		int tabsY = descY + DESC_BAR_HEIGHT + 8;
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

	/** Reorders the grid to match evaluation order (highest priority first) - purely visual, doesn't touch priority itself. */
	private void sortByPriorityDescending() {
		GearPreset preset = active();
		preset.cards.sort(Comparator.comparingInt((GearCard c) -> -c.priority).thenComparing(c -> c.id));
		this.config.save();
		this.scroll.reset();
		rebuild();
	}

	private void addNewCard() {
		GearPreset preset = active();
		GearCard card = new GearCard();
		card.color = pickDistinctColor(preset);
		// New cards default below everything else (highest priority wins first), so they don't jump the queue unasked.
		card.priority = preset.cards.stream().mapToInt(c -> c.priority).min().orElse(0) - 1;
		preset.cards.add(card);
		this.config.save();
		rebuild();
	}

	/**
	 * A random hue kept apart from every other non-rainbow card's color in this preset, so new cards don't
	 * visually blend in with existing ones. Can never "run out": if the wheel gets crowded enough that no
	 * random hue clears the minimum spacing after a bunch of tries, it falls back to the single widest gap
	 * left between existing hues (which always exists) and drops the new color right in the middle of it.
	 */
	private static int pickDistinctColor(GearPreset preset) {
		List<Float> existingHues = new ArrayList<>();
		for (GearCard c : preset.cards) {
			if (!c.rainbow) {
				existingHues.add(hueOf(c.color));
			}
		}
		if (existingHues.isEmpty()) {
			return 0xFF55FFFF;
		}

		float minSpacing = 0.08F; // ~29 degrees apart, enough that adjacent cards don't read as the same color
		java.util.Random random = new java.util.Random();
		for (int attempt = 0; attempt < 40; attempt++) {
			float candidate = random.nextFloat();
			boolean farEnoughFromAll = true;
			for (float hue : existingHues) {
				if (hueDistance(candidate, hue) < minSpacing) {
					farEnoughFromAll = false;
					break;
				}
			}
			if (farEnoughFromAll) {
				return colorFromHue(candidate);
			}
		}

		List<Float> sortedHues = new ArrayList<>(existingHues);
		Collections.sort(sortedHues);
		float widestGapMidpoint = 0F;
		float widestGap = -1F;
		for (int i = 0; i < sortedHues.size(); i++) {
			float low = sortedHues.get(i);
			float high = (i + 1 < sortedHues.size()) ? sortedHues.get(i + 1) : sortedHues.get(0) + 1.0F;
			float gap = high - low;
			if (gap > widestGap) {
				widestGap = gap;
				widestGapMidpoint = low + gap / 2F;
			}
		}
		return colorFromHue(widestGapMidpoint % 1.0F);
	}

	private static float hueOf(int argb) {
		float[] hsb = java.awt.Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, null);
		return hsb[0];
	}

	private static float hueDistance(float a, float b) {
		float d = Math.abs(a - b);
		return Math.min(d, 1.0F - d);
	}

	private static int colorFromHue(float hue) {
		int rgb = java.awt.Color.HSBtoRGB(hue, 1.0F, 1.0F);
		return 0xFF000000 | (rgb & 0x00FFFFFF);
	}

	// ============================== Card grid ==============================

	/** Builds one card tile at natural (x, y) with the given width; height is driven purely by its own content. */
	private int buildCardTile(GearPreset preset, GearCard card, int x, int y, int width) {
		int startY = y;
		int pad = 10;
		int leftX = x + pad;
		int innerWidth = width - pad * 2;
		y += pad;

		int cornerSize = 14;
		addScrollable(new GripHandleWidget(x + pad, y, cornerSize,
				() -> this.draggingCard = card,
				(mx, my) -> { this.dragX = mx; this.dragY = my; },
				(mx, my) -> { this.dragX = mx; this.dragY = my; handleDrop(preset, card); }));
		addScrollable(new RemoveButtonWidget(x + width - pad - cornerSize, y, cornerSize, () -> {
			preset.cards.remove(card);
			this.config.save();
			rebuild();
		}));
		y += cornerSize + 8;

		EditBox nameBox = new EditBox(this.font, leftX, y, innerWidth, 18, Component.literal("Card name"));
		nameBox.setValue(card.name);
		nameBox.setResponder(text -> { card.name = text; this.config.save(); });
		addScrollable(nameBox);
		y += 18 + 8;

		int priorityWidth = 32;
		int priorityHeight = 18;
		int priorityX = x + width / 2 - priorityWidth / 2;
		String priorityLabel = "Priority";
		this.labelDraws.add(new Object[]{priorityLabel, x + width / 2 - this.font.width(priorityLabel) / 2, y, DashboardColors.TEXT_MUTED});
		y += 10;

		EditBox priorityBox = new EditBox(this.font, priorityX, y, priorityWidth, priorityHeight, Component.literal("priority"));
		priorityBox.setValue(this.priorityText.computeIfAbsent(card.id, k -> Integer.toString(card.priority)));
		priorityBox.setResponder(text -> {
			this.priorityText.put(card.id, text);
			try {
				card.priority = Integer.parseInt(text.trim());
				this.config.save();
			} catch (NumberFormatException ignored) {
				// keep typing, don't clobber the last valid value
			}
		});
		addScrollable(priorityBox);
		y += priorityHeight + 10;

		this.conditionDividers.add(new int[]{leftX, y, innerWidth});
		y += 10;

		GearConditionGroup group = card.groups.get(0);
		if (!group.conditions.isEmpty()) {
			String allMustMatch = "(all must match)";
			this.labelDraws.add(new Object[]{allMustMatch, x + width / 2 - this.font.width(allMustMatch) / 2, y, DashboardColors.TEXT_MUTED});
			y += 11;
		}

		int bulletSize = 4;
		int textX = leftX + bulletSize + 6;
		int wrapWidth = innerWidth - bulletSize - 6;
		List<String> conditionLines = group.describeLines();
		for (int i = 0; i < conditionLines.size(); i++) {
			boolean first = true;
			for (String sub : wrapText(conditionLines.get(i), wrapWidth)) {
				if (first) {
					this.bulletDraws.add(new int[]{leftX, y + 2});
				}
				this.labelDraws.add(new Object[]{sub, textX, y, DashboardColors.TEXT_PRIMARY});
				y += 11;
				first = false;
			}
			// All conditions must match (AND) - spell that out instead of leaving an ambiguous gap.
			if (i < conditionLines.size() - 1) {
				this.labelDraws.add(new Object[]{"AND", leftX, y, DashboardColors.ACCENT});
				y += 11;
			}
		}
		y += 8;

		String editLabel = "Edit Rules (" + group.conditions.size() + ") ▸";
		addScrollable(new PanelButtonWidget(leftX, y, innerWidth, 20, editLabel, () -> {
			this.editingCard = card;
			this.scroll.reset();
			rebuild();
		}));
		y += 20;

		y += pad;
		int height = y - startY;
		this.tileRects.add(new Object[]{card, x, startY, width, height});
		return height;
	}

	/** Greedy word-wrap using the screen's font metrics, for the summary sentence on each tile. */
	private List<String> wrapText(String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String word : text.split(" ")) {
			String candidate = current.isEmpty() ? word : current + " " + word;
			if (this.font.width(candidate) > maxWidth && !current.isEmpty()) {
				lines.add(current.toString());
				current = new StringBuilder(word);
			} else {
				current = new StringBuilder(candidate);
			}
		}
		if (!current.isEmpty()) {
			lines.add(current.toString());
		}
		return lines;
	}

	// ============================== Card edit modal (conditions + color) ==============================

	private void buildCardEditModal(GearCard card) {
		int modalWidth = (int) (this.guiWidth * 0.8);
		int modalHeight = (int) (this.guiHeight * 0.82);
		int modalX = this.guiX + (this.guiWidth - modalWidth) / 2;
		int modalY = this.guiY + (this.guiHeight - modalHeight) / 2;
		this.editModalBox = new int[]{modalX, modalY, modalWidth, modalHeight};

		int pad = 16;
		int modalContentX = modalX + pad;
		int modalContentWidth = modalWidth - pad * 2;

		int headerY = modalY + 30;
		addRenderableWidget(new PanelButtonWidget(modalContentX, headerY, 130, 20, "◂ Back to Cards", () -> {
			this.editingCard = null;
			this.scroll.reset();
			rebuild();
		}));

		this.scrollViewTop = headerY + 20 + 12;
		this.scrollViewBottom = modalY + modalHeight - pad;

		int y = 0;
		GearConditionGroup group = card.groups.get(0);

		if (!group.conditions.isEmpty()) {
			String allMustMatch = "(all must match)";
			this.labelDraws.add(new Object[]{allMustMatch, modalContentX, y, DashboardColors.TEXT_MUTED});
			y += 14;
		}

		List<GearCondition> conditions = new ArrayList<>(group.conditions);
		for (int i = 0; i < conditions.size(); i++) {
			y = buildConditionRow(group, conditions.get(i), modalContentX, y, modalContentWidth);
			if (i < conditions.size() - 1) {
				y += 6;
				this.conditionDividers.add(new int[]{modalContentX, y, modalContentWidth});
				y += 6;
			}
		}
		y += conditions.isEmpty() ? 2 : 8;

		addScrollable(new PanelButtonWidget(modalContentX, y, modalContentWidth, 18, "+ Add Parameter", () -> {
			group.conditions.add(new GearCondition());
			this.config.save();
			rebuild();
		}));
		y += 26;

		HueSliderWidget slider = new HueSliderWidget(modalContentX, y, modalContentWidth, card.color, argb -> {
			card.color = argb;
			card.rainbow = false;
			this.config.save();
		});
		addScrollable(slider);
		y += 14 + 10;

		addScrollable(new TabButtonWidget(modalContentX, y, 130, 18, "RGB (Rainbow)",
				() -> card.rainbow, () -> { card.rainbow = !card.rainbow; this.config.save(); rebuild(); }));
		y += 26;

		layoutScroll(y);
	}

	private void handleDrop(GearPreset preset, GearCard source) {
		int shift = scrollShift();
		for (Object[] rect : this.tileRects) {
			GearCard target = (GearCard) rect[0];
			if (target == source) {
				continue;
			}
			int rx = (int) rect[1];
			int ry = (int) rect[2] + shift;
			int rw = (int) rect[3];
			int rh = (int) rect[4];
			if (this.dragX >= rx && this.dragX < rx + rw && this.dragY >= ry && this.dragY < ry + rh) {
				int sourceIndex = preset.cards.indexOf(source);
				int targetIndex = preset.cards.indexOf(target);
				Collections.swap(preset.cards, sourceIndex, targetIndex);
				this.config.save();
				break;
			}
		}
		this.draggingCard = null;
		rebuild();
	}

	private int buildConditionRow(GearConditionGroup group, GearCondition condition, int x, int y, int width) {
		int rowHeight = 20;
		int removeX = x + width - rowHeight;
		int paramWidth = width - rowHeight - 6;

		addScrollable(new PanelButtonWidget(x, y, paramWidth, rowHeight, condition.param.label + "  ▾", () -> {
			this.openDropdownFor = condition.uiId;
			rebuild();
		}));

		addScrollable(new RemoveButtonWidget(removeX, y, rowHeight, () -> {
			group.conditions.remove(condition);
			this.config.save();
			rebuild();
		}));

		y += rowHeight + 6;

		if (condition.param == GearParam.CLASS) {
			int chipGap = 6;
			int chipWidth = (width - chipGap * (GearClass.values().length - 1)) / GearClass.values().length;
			int chipX = x;
			for (GearClass gearClass : GearClass.values()) {
				addScrollable(new TabButtonWidget(chipX, y, chipWidth, rowHeight, gearClass.label,
						() -> condition.classes.contains(gearClass), () -> {
					if (!condition.classes.remove(gearClass)) {
						condition.classes.add(gearClass);
					}
					this.config.save();
					rebuild();
				}));
				chipX += chipWidth + chipGap;
			}
			return y + rowHeight;
		}

		this.labelDraws.add(new Object[]{"Min", x, y + 5, DashboardColors.TEXT_MUTED});
		EditBox minBox = new EditBox(this.font, x + 30, y, 60, rowHeight, Component.literal("min"));
		minBox.setValue(this.minText.computeIfAbsent(condition.uiId, k -> GearCondition.trimNumber(condition.min)));
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

		if (condition.param.isRange()) {
			int maxX = x + 30 + 60 + 16;
			this.labelDraws.add(new Object[]{"Max", maxX, y + 5, DashboardColors.TEXT_MUTED});
			EditBox maxBox = new EditBox(this.font, maxX + 30, y, 60, rowHeight, Component.literal("max"));
			maxBox.setValue(this.maxText.computeIfAbsent(condition.uiId, k -> condition.max == null ? "" : GearCondition.trimNumber(condition.max)));
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

		return y + rowHeight;
	}

	// ============================== Focus handling ==============================

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.editingCard != null && this.openDropdownFor == null && this.editModalBox != null) {
			int mx = (int) event.x();
			int my = (int) event.y();
			int bx = this.editModalBox[0];
			int by = this.editModalBox[1];
			int bw = this.editModalBox[2];
			int bh = this.editModalBox[3];
			boolean insideModal = mx >= bx && mx < bx + bw && my >= by && my < by + bh;
			if (!insideModal) {
				// Everything already auto-saves as you type, so clicking outside the modal is a safe "done".
				this.editingCard = null;
				this.scroll.reset();
				rebuild();
				return true;
			}
		}

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
		if (this.editingCard != null && event.key() == 256) { // Escape backs out of the edit modal
			this.editingCard = null;
			this.scroll.reset();
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

		if (this.editingCard == null) {
			int descY = this.guiY + DESC_BAR_Y_OFFSET;
			int descX0 = this.contentX;
			int descX1 = this.guiX + this.guiWidth - 18;
			graphics.fill(descX0 - 2, descY - 2, descX1 + 2, descY + DESC_BAR_HEIGHT + 2, DashboardColors.ACCENT_SOFT);
			graphics.fill(descX0, descY, descX1, descY + DESC_BAR_HEIGHT, DashboardColors.PANEL_SOFT);
			graphics.text(this.font, DESCRIPTION, descX0 + 8, descY + 6, DashboardColors.TEXT_MUTED);
		}

		if (this.editingCard != null && this.editModalBox != null) {
			// Gray out everything else so it reads as inert while the modal is open.
			graphics.fill(this.guiX, this.guiY, x1, y1, 0xA0000000);

			int bx = this.editModalBox[0];
			int by = this.editModalBox[1];
			int bw = this.editModalBox[2];
			int bh = this.editModalBox[3];
			graphics.fill(bx - 2, by - 2, bx + bw + 2, by + bh + 2, DashboardColors.ACCENT_SOFT);
			graphics.fill(bx, by, bx + bw, by + bh, DashboardColors.PANEL);
			String editTitle = "Editing: " + this.editingCard.name;
			graphics.text(this.font, editTitle, bx + bw / 2 - this.font.width(editTitle) / 2, by + 10, DashboardColors.TEXT_PRIMARY);
		}

		int shift = scrollShift();

		// Clip everything in the scroll region to the viewport so tiles can't paint over the sticky header/tabs while scrolling.
		graphics.enableScissor(this.guiX, this.scrollViewTop, this.guiX + this.guiWidth, this.scrollViewBottom);

		for (Object[] rect : this.tileRects) {
			GearCard card = (GearCard) rect[0];
			int tx = (int) rect[1];
			int ty = (int) rect[2] + shift;
			int tw = (int) rect[3];
			int th = (int) rect[4];
			if (ty + th < this.scrollViewTop || ty > this.scrollViewBottom) {
				continue;
			}
			int borderColor = card.rainbow ? rainbowPreviewColor() : card.color;
			int borderThickness = 2;
			graphics.fill(tx - borderThickness, ty - borderThickness, tx + tw + borderThickness, ty + th + borderThickness, borderColor);
			graphics.fill(tx, ty, tx + tw, ty + th, DashboardColors.PANEL_SOFT);
		}
		graphics.disableScissor();

		// Widgets already hide themselves entirely when out of the viewport (see layoutScroll), so the sticky
		// header/tabs/close button - which live outside [scrollViewTop, scrollViewBottom] - render unclipped here.
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		graphics.enableScissor(this.guiX, this.scrollViewTop, this.guiX + this.guiWidth, this.scrollViewBottom);
		for (int[] divider : this.conditionDividers) {
			int dy = divider[1] + shift;
			if (dy >= this.scrollViewTop && dy < this.scrollViewBottom) {
				graphics.fill(divider[0], dy, divider[0] + divider[2], dy + 1, DashboardColors.BORDER);
			}
		}
		for (Object[] draw : this.labelDraws) {
			int dy = (int) draw[2] + shift;
			if (dy >= this.scrollViewTop - 10 && dy <= this.scrollViewBottom) {
				graphics.text(this.font, (String) draw[0], (int) draw[1], dy, (int) draw[3]);
			}
		}
		for (int[] bullet : this.bulletDraws) {
			int by = bullet[1] + shift;
			if (by >= this.scrollViewTop - 4 && by <= this.scrollViewBottom) {
				graphics.fill(bullet[0], by, bullet[0] + 4, by + 4, DashboardColors.ACCENT);
			}
		}

		graphics.disableScissor();

		if (this.draggingCard != null) {
			int ghostWidth = Math.max(60, this.font.width(this.draggingCard.name) + 16);
			int ghostHeight = 18;
			int gx = this.dragX - ghostWidth / 2;
			int gy = this.dragY - ghostHeight / 2;
			graphics.fill(gx, gy, gx + ghostWidth, gy + ghostHeight, DashboardColors.ACCENT_SOFT);
			graphics.text(this.font, this.draggingCard.name, gx + 8, gy + 5, DashboardColors.TEXT_PRIMARY);
		}

		if (this.editingCard == null) {
			String title = "Gear Highlighter";
			int titleWidth = this.font.width(title);
			graphics.text(this.font, title, this.guiX + this.guiWidth / 2 - titleWidth / 2, this.guiY + 12, DashboardColors.TEXT_PRIMARY);
		}
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
