package com.ravengardextras.devtools;

/**
 * Ravengard's resource pack tags items (class requirement, rarity, gear slot)
 * with invisible Private-Use-Area codepoints inside the name/lore text, mapped
 * to icons by a custom font. The codepoint is present in the raw string but
 * renders as nothing when copy-pasted elsewhere.
 */
public final class RavengardLabels {
	private RavengardLabels() {
	}

	public enum Kind {
		CLASS, RARITY, ARMOR_SLOT, ACCESSORY_SLOT, WEAPON
	}

	public enum Glyph {
		ASSASSIN(Kind.CLASS, ''),
		KNIGHT(Kind.CLASS, ''),
		WARRIOR(Kind.CLASS, ''),
		HUNTER(Kind.CLASS, ''),

		COMMON(Kind.RARITY, ''),
		UNCOMMON(Kind.RARITY, ''),
		RARE(Kind.RARITY, ''),
		EPIC(Kind.RARITY, ''),
		LEGENDARY(Kind.RARITY, ''),

		HELMET(Kind.ARMOR_SLOT, ''),
		CHESTPLATE(Kind.ARMOR_SLOT, ''),
		LEGGINGS(Kind.ARMOR_SLOT, ''),
		BOOTS(Kind.ARMOR_SLOT, ''),

		NECKLACE(Kind.ACCESSORY_SLOT, ''),
		EARRING(Kind.ACCESSORY_SLOT, ''),
		BELT(Kind.ACCESSORY_SLOT, ''),
		RING(Kind.ACCESSORY_SLOT, ''),

		BOW(Kind.WEAPON, ''),
		CROSSBOW(Kind.WEAPON, ''),
		DAGGER(Kind.WEAPON, ''),
		GREATAXE(Kind.WEAPON, ''),
		GREATSWORD(Kind.WEAPON, ''),
		HALBERD(Kind.WEAPON, ''),
		HAMMER(Kind.WEAPON, ''),
		MACE(Kind.WEAPON, ''),
		SHIELD(Kind.WEAPON, ''),
		SWORD(Kind.WEAPON, ''),
		KNIFE(Kind.WEAPON, '');

		public final Kind kind;
		public final char glyph;

		Glyph(Kind kind, char glyph) {
			this.kind = kind;
			this.glyph = glyph;
		}
	}

	/** Returns every glyph tag found anywhere in the given lines, in declaration order. */
	public static java.util.List<Glyph> findAll(Iterable<String> lines) {
		java.util.List<Glyph> found = new java.util.ArrayList<>();
		for (Glyph glyph : Glyph.values()) {
			for (String line : lines) {
				if (line.indexOf(glyph.glyph) >= 0) {
					found.add(glyph);
					break;
				}
			}
		}
		return found;
	}
}
