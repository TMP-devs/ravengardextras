package com.ravengardextras.gearrules;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A highlight rule: matches if ANY group matches (OR); a group matches if ALL its conditions match (AND). */
public class GearCard {
	public String id = UUID.randomUUID().toString();
	public String name = "New Card";
	public boolean rainbow = false;
	public int color = 0xFF55FFFF;
	/** Lower evaluates first (GearRuleMatcher). Independent of grid position - dragging cards around never changes this. */
	public int priority = 0;
	public List<GearConditionGroup> groups = new ArrayList<>(List.of(new GearConditionGroup()));
	/** Legacy pre-OR-groups format, migrated into groups by GearRulesConfig on load. Not used directly otherwise. */
	public List<GearCondition> conditions = new ArrayList<>();

	public boolean matches(ItemStats stats) {
		for (GearConditionGroup group : this.groups) {
			if (group.matches(stats)) {
				return true;
			}
		}
		return false;
	}

	public int effectiveColor() {
		return this.rainbow ? com.ravengardextras.gearhighlighter.GearHighlighterConfig.RAINBOW : this.color;
	}
}
