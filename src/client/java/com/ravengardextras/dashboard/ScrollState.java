package com.ravengardextras.dashboard;

/** Tracks a vertical scroll offset for a content region taller than its viewport, clamped to a valid range. */
public class ScrollState {
	private int offset = 0;

	public int offset() {
		return this.offset;
	}

	public void reset() {
		this.offset = 0;
	}

	/** Nudges the raw offset from a wheel event; call clamp() afterward once the new content height is known. */
	public void nudge(double wheelDelta) {
		this.offset -= (int) (wheelDelta * 20);
		this.offset = Math.max(0, this.offset);
	}

	public void clamp(int contentHeight, int viewportHeight) {
		int max = Math.max(0, contentHeight - viewportHeight);
		this.offset = Math.max(0, Math.min(this.offset, max));
	}
}
