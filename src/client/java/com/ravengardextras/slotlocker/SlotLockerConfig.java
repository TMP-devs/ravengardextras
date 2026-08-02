package com.ravengardextras.slotlocker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Locks player-inventory slots so their contents can't be moved, swapped, or dropped. */
public class SlotLockerConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("ravengardextras-slotlocker.json");

	public boolean enabled = true;
	/** Player-inventory slot indices (0-8 hotbar, 9-35 main) currently locked. */
	public Set<Integer> lockedSlots = new HashSet<>();

	public static SlotLockerConfig load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				SlotLockerConfig cfg = GSON.fromJson(reader, SlotLockerConfig.class);
				if (cfg != null) {
					if (cfg.lockedSlots == null) {
						cfg.lockedSlots = new HashSet<>();
					}
					return cfg;
				}
			} catch (IOException | RuntimeException ignored) {
				// fall through to defaults
			}
		}
		SlotLockerConfig cfg = new SlotLockerConfig();
		cfg.save();
		return cfg;
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException ignored) {
			// non-fatal, config just won't persist this run
		}
	}

	/** True if this slot belongs to the given player's own inventory and is locked. */
	public boolean isLocked(Slot slot, Player player) {
		if (!enabled || slot == null || player == null) {
			return false;
		}
		return slot.container == player.getInventory() && lockedSlots.contains(slot.getContainerSlot());
	}

	/** True if the given player-inventory slot index (0-8 hotbar) is locked. */
	public boolean isSlotIndexLocked(int index) {
		return enabled && lockedSlots.contains(index);
	}

	/** Toggles the lock on the given slot. Returns false (no-op) if it's not a player-inventory slot. */
	public boolean toggleLock(Slot slot, Player player) {
		if (slot == null || player == null || slot.container != player.getInventory()) {
			return false;
		}
		int index = slot.getContainerSlot();
		if (!lockedSlots.remove(index)) {
			lockedSlots.add(index);
		}
		save();
		return true;
	}

	public void clearAll() {
		lockedSlots.clear();
		save();
	}
}
