package com.ravengardextras.gearrules;

public enum GearParam {
	CROWN_VALUE("Crown Value", false),
	DEFENSE("Defense", false),
	DAMAGE("Damage", false),
	ATTACK_SPEED("Attack Speed", false),
	STAMINA_REGEN("Stamina Regeneration", true),
	ABILITY_CDR("Ability Cooldown Reduction", true),
	PERCENT_DAMAGE("% Damage", true),
	PERCENT_DEFENSE("% Defense", true),
	CLASS("Class", false);

	public final String label;
	public final boolean percent;

	GearParam(String label, boolean percent) {
		this.label = label;
		this.percent = percent;
	}

	public boolean isNumeric() {
		return this != CLASS;
	}

	/** Only Crown Value supports a bounded [min, max] range; every other numeric stat is a single "at least" threshold. */
	public boolean isRange() {
		return this == CROWN_VALUE;
	}
}
