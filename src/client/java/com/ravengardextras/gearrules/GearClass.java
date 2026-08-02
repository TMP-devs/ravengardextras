package com.ravengardextras.gearrules;

/** Class-requirement tag hidden in an item's name/lore as an invisible font glyph. */
public enum GearClass {
	ASSASSIN("Assassin", ''),
	KNIGHT("Knight", ''),
	WARRIOR("Warrior", ''),
	HUNTER("Hunter", '');

	public final String label;
	public final char glyph;

	GearClass(String label, char glyph) {
		this.label = label;
		this.glyph = glyph;
	}
}
