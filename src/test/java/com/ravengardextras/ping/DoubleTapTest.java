package com.ravengardextras.ping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoubleTapTest {
	@Test
	void secondTapWithinWindowIsDoubleTap() {
		DoubleTap tap = new DoubleTap(400);
		assertFalse(tap.tap(1_000));
		assertTrue(tap.tap(1_300));
	}

	@Test
	void secondTapOutsideWindowIsNot() {
		DoubleTap tap = new DoubleTap(400);
		assertFalse(tap.tap(1_000));
		assertFalse(tap.tap(1_500));
	}

	@Test
	void tapExactlyAtWindowEdgeCounts() {
		DoubleTap tap = new DoubleTap(400);
		assertFalse(tap.tap(1_000));
		assertTrue(tap.tap(1_400));
	}

	@Test
	void completingADoubleTapResetsTheSequence() {
		DoubleTap tap = new DoubleTap(400);
		assertFalse(tap.tap(1_000));
		assertTrue(tap.tap(1_300));
		// A third quick tap starts a NEW sequence - it must not chain another double-tap.
		assertFalse(tap.tap(1_600));
		assertTrue(tap.tap(1_900));
	}

	@Test
	void firstTapAtTimeZeroWorksCorrectly() {
		DoubleTap tap = new DoubleTap(400);
		assertFalse(tap.tap(0));
		assertTrue(tap.tap(300));
	}
}
