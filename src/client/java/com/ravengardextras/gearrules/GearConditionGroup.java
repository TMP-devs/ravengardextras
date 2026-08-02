package com.ravengardextras.gearrules;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Conditions inside a group are AND'd together; a card matches if ANY of its groups matches (OR between groups). */
public class GearConditionGroup {
	public transient String uiId = UUID.randomUUID().toString();
	public List<GearCondition> conditions = new ArrayList<>();

	public boolean matches(ItemStats stats) {
		if (this.conditions.isEmpty()) {
			return false;
		}
		for (GearCondition condition : this.conditions) {
			if (!condition.matches(stats)) {
				return false;
			}
		}
		return true;
	}

	/** One plain-English line per condition in this group, for the collapsed grid tile's bullet list. */
	public List<String> describeLines() {
		if (this.conditions.isEmpty()) {
			return List.of("No rules set yet - won't match any gear.");
		}
		return this.conditions.stream().map(GearCondition::describe).collect(Collectors.toList());
	}
}
