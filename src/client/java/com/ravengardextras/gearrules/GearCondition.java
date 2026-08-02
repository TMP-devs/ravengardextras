package com.ravengardextras.gearrules;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** One rule condition inside a card. Numeric params use [min, max]; a null max means unbounded. */
public class GearCondition {
	public transient String uiId = java.util.UUID.randomUUID().toString();
	public GearParam param = GearParam.CROWN_VALUE;
	public double min = 0;
	public Double max = null;
	public Set<GearClass> classes = EnumSet.noneOf(GearClass.class);

	public boolean matches(ItemStats stats) {
		if (this.param == GearParam.CLASS) {
			// An empty selection means "not configured yet", not "any class" - it deliberately
			// blocks the whole card from matching until at least one class chip is picked.
			if (this.classes.isEmpty()) {
				return false;
			}
			for (GearClass c : this.classes) {
				if (stats.classes.contains(c)) {
					return true;
				}
			}
			return false;
		}
		Double value = stats.value(this.param);
		if (value == null) {
			return false;
		}
		if (value < this.min) {
			return false;
		}
		return this.max == null || value <= this.max;
	}

	/** Plain-English fragment for this one condition, e.g. "Defense ≥ 5" or "Class: Assassin or Knight". */
	public String describe() {
		if (this.param == GearParam.CLASS) {
			if (this.classes.isEmpty()) {
				return "Class: (none selected)";
			}
			return "Class: " + this.classes.stream().map(c -> c.label).collect(Collectors.joining(" or "));
		}
		String minText = trimNumber(this.min) + (this.param.percent ? "%" : "");
		if (this.param.isRange() && this.max != null) {
			String maxText = trimNumber(this.max) + (this.param.percent ? "%" : "");
			return this.param.label + " " + minText + "–" + maxText;
		}
		return this.param.label + " ≥ " + minText;
	}

	public static String trimNumber(double value) {
		if (value == Math.floor(value) && !Double.isInfinite(value)) {
			return Long.toString((long) value);
		}
		return Double.toString(value);
	}
}
