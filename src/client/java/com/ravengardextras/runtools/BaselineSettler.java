package com.ravengardextras.runtools;

/**
 * Decides when the entry inventory snapshot can be trusted.
 *
 * <p>The sidebar scoreboard flips to the dungeon objective as soon as the server says so,
 * which is not necessarily after the inventory has finished syncing across the dimension
 * change. Snapshotting on that first tick can read a partial or empty inventory, capturing a
 * baseline far too low — and since the HUD shows {@code current − baseline}, the player's
 * entire kit then counts as profit, with nothing on screen indicating a fault.
 *
 * <p>So: wait out a short grace period, then require the total to hold steady for several
 * consecutive ticks before accepting it. The grace period matters on its own — without it,
 * an inventory that reads 0 for the first few ticks would "hold steady" at 0 and settle wrong.
 *
 * <p>Pure logic over a sequence of totals, so it is testable without a running game.
 */
final class BaselineSettler {
	/** Grace period (1s at 20 tps) before any value is eligible, covering post-teleport sync. */
	static final int MIN_DELAY_TICKS = 20;
	/** How many consecutive identical totals count as "settled". */
	static final int STABLE_TICKS = 5;

	private int ticks = 0;
	private int stableRun = 0;
	private long lastTotal = Long.MIN_VALUE;
	private boolean settled = false;
	private long settledValue = 0;

	/**
	 * Feed one tick's inventory total.
	 *
	 * @return true on the single tick where the baseline settles
	 */
	boolean accept(long total) {
		if (settled) {
			return false;
		}
		ticks++;

		if (total == lastTotal) {
			stableRun++;
		} else {
			stableRun = 1;
			lastTotal = total;
		}

		if (ticks >= MIN_DELAY_TICKS && stableRun >= STABLE_TICKS) {
			settled = true;
			settledValue = total;
			return true;
		}
		return false;
	}

	boolean isSettled() {
		return settled;
	}

	long settledValue() {
		return settledValue;
	}
}
