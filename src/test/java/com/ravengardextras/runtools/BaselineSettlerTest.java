package com.ravengardextras.runtools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaselineSettlerTest {

	/** Feeds a constant total for n ticks, returning the tick it settled on, or -1. */
	private static int feed(BaselineSettler settler, long total, int ticks) {
		for (int i = 0; i < ticks; i++) {
			if (settler.accept(total)) {
				return i;
			}
		}
		return -1;
	}

	@Test
	void doesNotSettleDuringTheGracePeriod() {
		BaselineSettler settler = new BaselineSettler();
		assertEquals(-1, feed(settler, 40, BaselineSettler.MIN_DELAY_TICKS - 1));
		assertFalse(settler.isSettled());
	}

	@Test
	void settlesOnAStableInventory() {
		BaselineSettler settler = new BaselineSettler();
		assertTrue(feed(settler, 40, BaselineSettler.MIN_DELAY_TICKS + 5) >= 0);
		assertTrue(settler.isSettled());
		assertEquals(40, settler.settledValue());
	}

	/**
	 * The bug this class exists for: inventory reads 0 for a while after the teleport, then
	 * syncs to its real value. Settling must land on 40, not on the transient 0.
	 */
	@Test
	void ignoresAnEmptyInventoryThatSyncsLate() {
		BaselineSettler settler = new BaselineSettler();
		for (int i = 0; i < 15; i++) {
			assertFalse(settler.accept(0), "must not settle on the pre-sync value");
		}
		int settledAt = feed(settler, 40, 30);
		assertTrue(settledAt >= 0);
		assertEquals(40, settler.settledValue());
	}

	@Test
	void aChangingTotalResetsTheStableRun() {
		BaselineSettler settler = new BaselineSettler();
		for (int i = 0; i < BaselineSettler.MIN_DELAY_TICKS + 10; i++) {
			assertFalse(settler.accept(i), "a total that changes every tick can never settle");
		}
	}

	@Test
	void settlesOnZeroWhenTheInventoryGenuinelyIsEmpty() {
		BaselineSettler settler = new BaselineSettler();
		assertTrue(feed(settler, 0, BaselineSettler.MIN_DELAY_TICKS + 5) >= 0);
		assertEquals(0, settler.settledValue());
	}

	@Test
	void settlesOnlyOnce() {
		BaselineSettler settler = new BaselineSettler();
		assertTrue(feed(settler, 40, BaselineSettler.MIN_DELAY_TICKS + 5) >= 0);
		assertFalse(settler.accept(999), "later inventory changes must not move the baseline");
		assertEquals(40, settler.settledValue());
	}
}
