package com.ravengardextras.runtools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrongholdAlertMatcherTest {
	@Test
	void hasOpened() {
		assertTrue(StrongholdAlertMatcher.isStrongholdOpened("The Stronghold has opened!"));
	}

	@Test
	void unlocked() {
		assertTrue(StrongholdAlertMatcher.isStrongholdOpened("Stronghold unlocked"));
	}

	@Test
	void gateOpens() {
		assertTrue(StrongholdAlertMatcher.isStrongholdOpened("The stronghold gate opens"));
	}

	@Test
	void caseInsensitiveAndColorCodesAlreadyStripped() {
		assertTrue(StrongholdAlertMatcher.isStrongholdOpened("STRONGHOLD IS NOW OPEN"));
	}

	@Test
	void sealedIsNotOpen() {
		assertFalse(StrongholdAlertMatcher.isStrongholdOpened("The stronghold is sealed"));
	}

	@Test
	void closedIsNotOpen() {
		assertFalse(StrongholdAlertMatcher.isStrongholdOpened("The stronghold has closed"));
	}

	@Test
	void merelyEnteringIsNotOpen() {
		assertFalse(StrongholdAlertMatcher.isStrongholdOpened("You are entering the stronghold"));
	}

	@Test
	void openWithoutStrongholdIsIgnored() {
		assertFalse(StrongholdAlertMatcher.isStrongholdOpened("The chest has opened"));
	}

	@Test
	void emptyOrNull() {
		assertFalse(StrongholdAlertMatcher.isStrongholdOpened(""));
		assertFalse(StrongholdAlertMatcher.isStrongholdOpened(null));
	}
}
