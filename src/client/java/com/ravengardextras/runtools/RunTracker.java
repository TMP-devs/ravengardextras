package com.ravengardextras.runtools;

import com.ravengardextras.gearhighlighter.CrownParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Tracks Crowns gained and XP earned within a single Ravengard run.
 *
 * <p><b>Crowns:</b> on entering a run the total Crown value of the whole inventory is
 * snapshotted as the baseline. Net gained is always {@code current − baseline}, so the
 * gear you walked in with never counts: enter with 300 Crowns of gear, loot a 75-Crown
 * sword and the HUD reads +75; drop it again and it drops back to 0; swap a good item for
 * a worse one and it goes negative. A Crown lore value is multiplied by the stack size so
 * a stack of five 10-Crown potions counts as 50.
 *
 * <p><b>XP:</b> accrued from chat/action-bar gains (see {@link XpParser}) only while a run
 * is active, and reset at the start of each run.
 *
 * <p>Run boundaries come from {@link RavengardRunDetector} (the dungeon sidebar scoreboard).
 */
public final class RunTracker {
	private final RunToolsConfig config;

	private boolean runActive = false;
	private boolean baselineCaptured = false;
	private long baselineCrowns = 0;
	private long currentCrowns = 0;
	private long runXp = 0;
	private boolean escapeReported = false;

	public RunTracker(RunToolsConfig config) {
		this.config = config;
	}

	/** Called every client tick. Drives run start/end and refreshes the current Crown total. */
	public void tick(Minecraft client) {
		boolean toolsOn = config.crownCalcEnabled || config.xpCalcEnabled;
		boolean nowInRun = toolsOn && RavengardRunDetector.inRavengard(client);

		if (nowInRun && !runActive) {
			beginRun();
		} else if (!nowInRun && runActive) {
			endRun(client);
		}

		if (runActive && client.player != null) {
			if (!baselineCaptured) {
				baselineCrowns = totalCrowns(client.player);
				currentCrowns = baselineCrowns;
				baselineCaptured = true;
			} else {
				currentCrowns = totalCrowns(client.player);
			}
		}
	}

	private void beginRun() {
		runActive = true;
		baselineCaptured = false;
		baselineCrowns = 0;
		currentCrowns = 0;
		runXp = 0;
		escapeReported = false;
	}

	private void endRun(Minecraft client) {
		runActive = false;
	}

	/**
	 * Called when the local player's "has escaped with…" line appears — the last line of
	 * the escape block, after all XP has landed. Prints our calculated run totals into chat
	 * right below the server's escape message. Fires at most once per run.
	 */
	public void reportEscape() {
		if (!runActive || escapeReported) {
			return;
		}
		escapeReported = true;
		Minecraft client = Minecraft.getInstance();
		if (client.gui == null || client.gui.hud == null || client.gui.hud.getChat() == null) {
			return;
		}
		var chat = client.gui.hud.getChat();
		if (config.crownCalcEnabled) {
			chat.addClientSystemMessage(Component.literal("Calculated Crowns Collected: " + String.format("%,d", netCrowns())));
		}
		if (config.xpCalcEnabled) {
			chat.addClientSystemMessage(Component.literal("Calculated EXP earned: " + String.format("%,d", runXp)));
		}
	}

	/** Feed an XP gain parsed from chat. Only counts while a run is active. */
	public void addXp(long amount) {
		if (runActive && amount > 0) {
			runXp += amount;
		}
	}

	public boolean isRunActive() {
		return runActive;
	}

	/** Net Crowns gained this run (current inventory value minus the entry snapshot). */
	public long netCrowns() {
		return baselineCaptured ? currentCrowns - baselineCrowns : 0;
	}

	public long runXp() {
		return runXp;
	}

	/** Sums the Crown lore value of every stack in the inventory, weighted by stack size. */
	private static long totalCrowns(LocalPlayer player) {
		Inventory inventory = player.getInventory();
		long total = 0;
		int size = inventory.getContainerSize();
		for (int i = 0; i < size; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			long crowns = CrownParser.findCrowns(stack);
			if (crowns > 0) {
				total += crowns * stack.getCount();
			}
		}
		return total;
	}

	/** "+75" / "-40" / "+0" — an explicit sign makes gains and losses read at a glance. */
	public static String formatSigned(long value) {
		return (value >= 0 ? "+" : "") + String.format("%,d", value);
	}
}
