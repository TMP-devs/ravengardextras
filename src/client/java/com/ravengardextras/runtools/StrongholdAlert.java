package com.ravengardextras.runtools;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Plays an ender dragon growl when chat announces the stronghold has opened.
 * A short cooldown stops a burst of near-identical server lines from stacking the sound.
 */
public final class StrongholdAlert {
	private static final long COOLDOWN_MILLIS = 5_000;

	private final RunToolsConfig config;
	private long lastPlayedAt = Long.MIN_VALUE;

	public StrongholdAlert(RunToolsConfig config) {
		this.config = config;
	}

	/** Checks one chat line and fires the growl if it announces the stronghold opening. */
	public void observe(String plainText, long nowMillis) {
		if (!config.strongholdAlertEnabled) {
			return;
		}
		if (!StrongholdAlertMatcher.isStrongholdOpened(plainText)) {
			return;
		}
		if (nowMillis - lastPlayedAt < COOLDOWN_MILLIS) {
			return;
		}
		lastPlayedAt = nowMillis;
		play(config.strongholdVolume);
	}

	/** Plays the growl once at the given volume (also used by the settings "Test" button). */
	public static void play(float volume) {
		Minecraft client = Minecraft.getInstance();
		if (client.getSoundManager() == null) {
			return;
		}
		client.getSoundManager().play(
				SimpleSoundInstance.forUI(SoundEvents.ENDER_DRAGON_GROWL, 1.0F, volume));
	}
}
