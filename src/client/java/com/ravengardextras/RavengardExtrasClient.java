package com.ravengardextras;

import com.mojang.blaze3d.platform.InputConstants;
import com.ravengardextras.devtools.ItemDataDumpButton;
import com.ravengardextras.gearhighlighter.GearHighlighterConfig;
import com.ravengardextras.ping.PingChatListener;
import com.ravengardextras.ping.PingConfig;
import com.ravengardextras.ping.PingKeyHandler;
import com.ravengardextras.ping.PingRenderer;
import com.ravengardextras.slotlocker.SlotLockerConfig;
import com.ravengardextras.mixin.AbstractContainerScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class RavengardExtrasClient implements ClientModInitializer {
	public static GearHighlighterConfig CONFIG;
	public static PingConfig PING_CONFIG;
	public static SlotLockerConfig SLOT_LOCKER_CONFIG;
	/** Own section in the vanilla Controls screen; label comes from key.category.ravengardextras.main. */
	private static final KeyMapping.Category KEY_CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ravengardextras", "main"));
	private static KeyMapping openMenuKey;
	private static KeyMapping pingKey;
	private static KeyMapping markKey;
	private static KeyMapping lockSlotKey;
	private static volatile boolean menuOpenRequested = false;

	@Override
	public void onInitializeClient() {
		CONFIG = GearHighlighterConfig.load();
		PING_CONFIG = PingConfig.load();
		SLOT_LOCKER_CONFIG = SlotLockerConfig.load();

		openMenuKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.ravengardextras.open_menu", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY));
		pingKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.ravengardextras.ping", InputConstants.KEY_Z, KEY_CATEGORY));
		markKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.ravengardextras.mark", InputConstants.KEY_X, KEY_CATEGORY));
		lockSlotKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.ravengardextras.lock_slot", InputConstants.KEY_L, KEY_CATEGORY));

		PingChatListener.register();
		PingRenderer.register();

		// Mark the hovered item while a chest/inventory GUI is open (keybinds don't
		// fire in screens, so this listens to the screen's own key events).
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof AbstractContainerScreen<?> containerScreen) {
				// DEV ONLY: dumps every visible item's id + data components to clipboard.
				net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(screen)
						.add(new ItemDataDumpButton(2, 2, containerScreen));
				ScreenKeyboardEvents.afterKeyPress(screen).register((scr, keyEvent) -> {
					if (markKey.matches(keyEvent)) {
						PingKeyHandler.markHoveredItem(client, containerScreen);
					}
					if (lockSlotKey.matches(keyEvent) && client.player != null) {
						var hovered = ((AbstractContainerScreenAccessor) containerScreen).ravengardextras$getHoveredSlot();
						if (SLOT_LOCKER_CONFIG.toggleLock(hovered, client.player)) {
							playLockSound(client);
						}
					}
				});
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenuKey.consumeClick()) {
				menuOpenRequested = true;
			}
			while (pingKey.consumeClick()) {
				PingKeyHandler.onPingKey(client);
			}
			while (markKey.consumeClick()) {
				// drained; the mark key is driven by held-state below (tap vs hold)
			}
			PingKeyHandler.tickMarkKey(client, markKey.isDown());
			// Deferred to end-of-tick so a chat screen's own close (which happens
			// right after a command is sent) can never race with and undo this.
			if (menuOpenRequested) {
				menuOpenRequested = false;
				client.gui.setScreen(new RavengardExtrasMenuScreen(null));
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			com.mojang.brigadier.Command<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> openMenu = ctx -> {
				menuOpenRequested = true;
				return 1;
			};
			dispatcher.register(literal("ravengardextras").executes(openMenu));
			dispatcher.register(literal("rge").executes(openMenu));
		});
	}

	/** Current display name of the ping key (tracks rebinds), e.g. "Z". */
	public static String pingKeyName() {
		return pingKey != null ? pingKey.getTranslatedKeyMessage().getString() : "?";
	}

	/** Current display name of the mark key (tracks rebinds), e.g. "X". */
	public static String markKeyName() {
		return markKey != null ? markKey.getTranslatedKeyMessage().getString() : "?";
	}

	/** Current display name of the slot-lock key (tracks rebinds), e.g. "L". */
	public static String lockSlotKeyName() {
		return lockSlotKey != null ? lockSlotKey.getTranslatedKeyMessage().getString() : "?";
	}

	private static void playLockSound(Minecraft client) {
		client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.2F, 0.55F));
	}
}
