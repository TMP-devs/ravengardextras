package com.ravengardextras.ping;

/**
 * Detects a double-tap: two taps at most a window apart. The tap that completes
 * a double-tap resets the detector, so a third quick tap starts a new sequence
 * instead of chaining.
 */
public final class DoubleTap {
	private final long windowMillis;
	private long lastTapMillis;
	private boolean hasTap;

	public DoubleTap(long windowMillis) {
		this.windowMillis = windowMillis;
	}

	/** Records a tap at nowMillis; true iff it completed a double-tap. */
	public boolean tap(long nowMillis) {
		boolean second = hasTap && nowMillis - lastTapMillis <= windowMillis;
		if (second) {
			hasTap = false;
		} else {
			lastTapMillis = nowMillis;
			hasTap = true;
		}
		return second;
	}
}
