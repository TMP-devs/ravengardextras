package com.ravengardextras.gearhighlighter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrownParserTest {

	@Test
	void readsAPlainValue() {
		assertEquals(75, CrownParser.parseLine("75 Crowns"));
	}

	/**
	 * The off-by-one: an item worth exactly one crown reads "1 Crown", not "1 Crowns".
	 * Requiring the plural valued such items at zero, so a run totalling 205 on the
	 * server was reported as 204.
	 */
	@Test
	void readsTheSingularForm() {
		assertEquals(1, CrownParser.parseLine("1 Crown"));
	}

	@Test
	void isCaseInsensitive() {
		assertEquals(5, CrownParser.parseLine("5 crowns"));
		assertEquals(1, CrownParser.parseLine("1 crown"));
		assertEquals(9, CrownParser.parseLine("9 CROWNS"));
	}

	@Test
	void readsValuesEmbeddedInALoreLine() {
		assertEquals(1, CrownParser.parseLine("Value: 1 Crown"));
		assertEquals(40, CrownParser.parseLine("Worth 40 Crowns!"));
	}

	@Test
	void handlesThousandsSeparatorsAndSuffixes() {
		assertEquals(1234, CrownParser.parseLine("1,234 Crowns"));
		assertEquals(2500, CrownParser.parseLine("2.5k Crowns"));
		assertEquals(3_000_000, CrownParser.parseLine("3M Crowns"));
	}

	/** "Crown" must be a whole word, so unrelated lore cannot be mistaken for a value. */
	@Test
	void doesNotMatchWordsMerelyStartingWithCrown() {
		assertEquals(-1, CrownParser.parseLine("1 Crowning Achievement"));
		assertEquals(-1, CrownParser.parseLine("2 Crownsmiths"));
	}

	@Test
	void returnsMinusOneWhenThereIsNoValue() {
		assertEquals(-1, CrownParser.parseLine("A sturdy iron sword"));
		assertEquals(-1, CrownParser.parseLine(""));
	}
}
