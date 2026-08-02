package com.ravengardextras.runtools;

/**
 * Colours shared by the run-tools HUD and the escape summary, so a Crown total reads the
 * same gold in both places and an XP total the same green.
 */
final class RunToolsColors {
	/** Crown totals. */
	static final int CROWN = 0xFFFFD24A;
	/** XP totals. */
	static final int XP = 0xFF66FF66;
	/** The "[RGE]" tag, marking a line as ours rather than the server's. */
	static final int TAG = 0xFF9B7FE8;
	/** Label text ahead of a value. */
	static final int LABEL = 0xFFA79BC4;

	private RunToolsColors() {
	}
}
