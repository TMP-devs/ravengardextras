package com.ravengardextras;

import com.mojang.blaze3d.platform.InputConstants;
import com.ravengardextras.gearhighlighter.GearHighlighterConfig;
import com.ravengardextras.runtools.RunChatListener;
import com.ravengardextras.runtools.RunHudRenderer;
import com.ravengardextras.runtools.RunToolsConfig;
import com.ravengardextras.runtools.RunTracker;
import com.ravengardextras.runtools.StrongholdAlert;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class RavengardExtrasClient implements ClientModInitializer {
	public static GearHighlighterConfig CONFIG;
	public static RunToolsConfig RUN_TOOLS_CONFIG;
	public static RunTracker RUN_TRACKER;
	public static StrongholdAlert STRONGHOLD_ALERT;
	private static KeyMapping openMenuKey;
	private static volatile boolean menuOpenRequested = false;

	@Override
	public void onInitializeClient() {
		CONFIG = GearHighlighterConfig.load();
		RUN_TOOLS_CONFIG = RunToolsConfig.load();
		RUN_TRACKER = new RunTracker(RUN_TOOLS_CONFIG);
		STRONGHOLD_ALERT = new StrongholdAlert(RUN_TOOLS_CONFIG);
		RunChatListener.register(RUN_TRACKER, STRONGHOLD_ALERT, RUN_TOOLS_CONFIG);
		RunHudRenderer.register(RUN_TRACKER, RUN_TOOLS_CONFIG);

		openMenuKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.ravengardextras.open_menu", InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			RUN_TRACKER.tick(client);
			while (openMenuKey.consumeClick()) {
				menuOpenRequested = true;
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
}
