package com.ravengardextras.gearrules;

import java.util.EnumSet;
import java.util.Set;

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
}
