package com.ravengardextras;

import com.mojang.blaze3d.platform.InputConstants;
import com.ravengardextras.cooldown.CooldownDebugOverlay;
import com.ravengardextras.debug.CooldownScan;
import com.ravengardextras.debug.DebugFile;
import com.ravengardextras.debug.ScoreboardDump;
import com.ravengardextras.gearhighlighter.GearHighlighterConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class RavengardExtrasClient implements ClientModInitializer {
	public static GearHighlighterConfig CONFIG;
	private static KeyMapping openMenuKey;
	private static volatile boolean menuOpenRequested = false;

	@Override
	public void onInitializeClient() {
		CONFIG = GearHighlighterConfig.load();

		openMenuKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.ravengardextras.open_menu", InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			com.ravengardextras.debug.CooldownWatcher.tick();
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

		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath("ravengardextras", "cooldown_debug"),
				new CooldownDebugOverlay());

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			com.mojang.brigadier.Command<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> openMenu = ctx -> {
				menuOpenRequested = true;
				return 1;
			};
			com.mojang.brigadier.Command<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> toggleDebug = ctx -> {
				CooldownDebugOverlay.enabled = !CooldownDebugOverlay.enabled;
				ctx.getSource().sendFeedback(Component.literal(
						"[RGE] cooldown debug: " + (CooldownDebugOverlay.enabled ? "ON" : "OFF")));
				return 1;
			};
			// Silent, file-only dumps to <game dir>/ravengardextras/debug.txt for Cmd+F reading.
			com.mojang.brigadier.Command<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> dumpScoreboard = ctx -> {
				DebugFile.section("SCOREBOARD", ScoreboardDump.sidebarLines());
				return 1;
			};
			com.mojang.brigadier.Command<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> dumpCooldowns = ctx -> {
				DebugFile.section("COOLDOWNS", CooldownScan.lines());
				return 1;
			};
			com.mojang.brigadier.Command<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> dumpText = ctx -> {
				DebugFile.section("TEXT SNAPSHOT", com.ravengardextras.debug.HudText.snapshot());
				return 1;
			};
			dispatcher.register(literal("ravengardextras").executes(openMenu)
					.then(literal("debug").executes(toggleDebug)
							.then(literal("scoreboard").executes(dumpScoreboard))
							.then(literal("cooldowns").executes(dumpCooldowns))
							.then(literal("text").executes(dumpText))));
			dispatcher.register(literal("rge").executes(openMenu)
					.then(literal("debug").executes(toggleDebug)
							.then(literal("scoreboard").executes(dumpScoreboard))
							.then(literal("cooldowns").executes(dumpCooldowns))
							.then(literal("text").executes(dumpText))));
		});
	}
}
