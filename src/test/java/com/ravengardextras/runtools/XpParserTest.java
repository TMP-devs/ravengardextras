package com.ravengardextras.runtools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XpParserTest {
	@Test
	void plusPrefixedGain() {
		assertEquals(15, XpParser.parseXp("+15 XP"));
	}

	@Test
	void noPrefixGain() {
		assertEquals(15, XpParser.parseXp("15 XP"));
	}

	@Test
	void sentenceForm() {
		assertEquals(15, XpParser.parseXp("You gained 15 XP!"));
	}

	@Test
	void commaThousands() {
		assertEquals(1240, XpParser.parseXp("+1,240 XP"));
	}

	@Test
	void expKeyword() {
		assertEquals(15, XpParser.parseXp("+15 EXP"));
	}

	@Test
	void experienceKeyword() {
		assertEquals(15, XpParser.parseXp("+15 Experience"));
	}

	@Test
	void caseInsensitive() {
		assertEquals(15, XpParser.parseXp("+15 xp"));
	}

	@Test
	void kSuffix() {
		assertEquals(2500, XpParser.parseXp("+2.5k XP"));
	}

	@Test
	void embeddedInActionBar() {
		assertEquals(15, XpParser.parseXp("Combo x3   +15 XP   Kills: 7"));
	}

	@Test
	void noXpKeyword() {
		assertEquals(-1, XpParser.parseXp("You have 15 apples"));
	}

	@Test
	void crownLineIsNotXp() {
		assertEquals(-1, XpParser.parseXp("300 Crowns"));
	}

	@Test
	void numberAfterKeywordIsIgnored() {
		// XP bar totals put the number after the keyword; those are not per-event gains.
		assertEquals(-1, XpParser.parseXp("XP: 1,240 / 2,000"));
	}

	@Test
	void progressBarRatioIsNotAGain() {
		// A persistent "current / max XP" bar must never be counted as a per-event gain,
		// otherwise the run total would balloon every tick it is on screen.
		assertEquals(-1, XpParser.parseXp("1,240 / 5,000 XP"));
		assertEquals(-1, XpParser.parseXp("Level 4  1240/5000 XP"));
	}

	@Test
	void emptyOrNull() {
		assertEquals(-1, XpParser.parseXp(""));
		assertEquals(-1, XpParser.parseXp(null));
	}
}
