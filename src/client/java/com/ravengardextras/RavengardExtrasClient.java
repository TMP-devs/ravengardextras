package com.ravengardextras;

import com.mojang.blaze3d.platform.InputConstants;
import com.ravengardextras.gearhighlighter.GearHighlighterConfig;
import com.ravengardextras.ping.PingChatListener;
import com.ravengardextras.ping.PingConfig;
import com.ravengardextras.ping.PingKeyHandler;
import com.ravengardextras.ping.PingRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class RavengardExtrasClient implements ClientModInitializer {
	public static GearHighlighterConfig CONFIG;
	public static PingConfig PING_CONFIG;
	private static KeyMapping openMenuKey;
	private static KeyMapping pingKey;
	private static KeyMapping markKey;
	private static volatile boolean menuOpenRequested = false;

	@Override
	public void onInitializeClient() {
		CONFIG = GearHighlighterConfig.load();
		PING_CONFIG = PingConfig.load();

		openMenuKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.ravengardextras.open_menu", InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC));
		pingKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.ravengardextras.ping", InputConstants.KEY_Z, KeyMapping.Category.MISC));
		markKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.ravengardextras.mark", InputConstants.KEY_X, KeyMapping.Category.MISC));

		PingChatListener.register();
		PingRenderer.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenuKey.consumeClick()) {
				menuOpenRequested = true;
			}
			while (pingKey.consumeClick()) {
				PingKeyHandler.onPingKey(client);
			}
			while (markKey.consumeClick()) {
				PingKeyHandler.onMarkKey(client);
			}
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
}
