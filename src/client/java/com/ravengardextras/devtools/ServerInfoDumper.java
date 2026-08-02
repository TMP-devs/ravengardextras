package com.ravengardextras.devtools;

import com.ravengardextras.mixin.BossHealthOverlayAccessor;
import com.ravengardextras.mixin.ClientPacketListenerAccessor;
import com.ravengardextras.mixin.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.BossEvent;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Dev-only: dumps every client-visible signal that could identify "we're on the Ravengard
 * server/minigame" - server list entry, plugin-channel brand, scoreboard objectives/teams,
 * boss bars, tab-list header/footer. All read-only, nothing sent to the server.
 */
public final class ServerInfoDumper {
	private ServerInfoDumper() {
	}

	public static String dump() {
		Minecraft client = Minecraft.getInstance();
		StringBuilder sb = new StringBuilder();

		ServerData server = client.getCurrentServer();
		sb.append("=== Server ===\n");
		if (server != null) {
			sb.append("name = ").append(server.name).append('\n');
			sb.append("ip = ").append(server.ip).append('\n');
			sb.append("motd = ").append(server.motd.getString()).append('\n');
		} else {
			sb.append("(no ServerData - direct connect or singleplayer)\n");
		}
		sb.append("isLocalServer = ").append(client.isLocalServer()).append('\n');
		sb.append("hasSingleplayerServer = ").append(client.hasSingleplayerServer()).append('\n');

		ClientPacketListener connection = client.getConnection();
		if (connection != null) {
			String brand = ((ClientPacketListenerAccessor) connection).ravengardextras$getServerBrand();
			sb.append("serverBrand = ").append(brand).append('\n');
			if (connection.getConnection() != null) {
				sb.append("remoteAddress = ").append(connection.getConnection().getRemoteAddress()).append('\n');
			}
		}

		if (client.player != null && client.level != null) {
			sb.append("dimension = ").append(client.level.dimension().identifier()).append('\n');
		}

		sb.append("\n=== Scoreboard ===\n");
		if (connection != null) {
			Scoreboard scoreboard = connection.scoreboard();
			for (Objective objective : scoreboard.getObjectives()) {
				sb.append("objective \"").append(objective.getName()).append("\" | display=\"")
						.append(objective.getDisplayName().getString())
						.append("\" | criteria=").append(objective.getCriteria())
						.append(" | renderType=").append(objective.getRenderType()).append('\n');
			}
			for (DisplaySlot slot : DisplaySlot.values()) {
				Objective shown = scoreboard.getDisplayObjective(slot);
				if (shown != null) {
					sb.append("displaySlot ").append(slot).append(" = \"").append(shown.getName()).append("\"\n");
				}
			}
			for (PlayerTeam team : scoreboard.getPlayerTeams()) {
				sb.append("team \"").append(team.getName()).append("\" | display=\"").append(team.getDisplayName().getString())
						.append("\" | prefix=\"").append(team.getPlayerPrefix().getString())
						.append("\" | suffix=\"").append(team.getPlayerSuffix().getString())
						.append("\" | color=").append(team.getColor())
						.append(" | members=").append(team.getPlayers()).append('\n');
			}
		}

		sb.append("\n=== Boss bars ===\n");
		BossHealthOverlay bossOverlay = client.gui.hud.getBossOverlay();
		for (LerpingBossEvent event : ((BossHealthOverlayAccessor) bossOverlay).ravengardextras$getEvents().values()) {
			BossEvent bossEvent = event;
			sb.append("\"").append(bossEvent.getName().getString()).append("\" | progress=").append(bossEvent.getProgress()).append('\n');
		}

		sb.append("\n=== Tab list ===\n");
		PlayerTabOverlay tabList = client.gui.hud.getTabList();
		var header = ((PlayerTabOverlayAccessor) tabList).ravengardextras$getHeader();
		var footer = ((PlayerTabOverlayAccessor) tabList).ravengardextras$getFooter();
		sb.append("header = ").append(header != null ? header.getString() : "(none)").append('\n');
		sb.append("footer = ").append(footer != null ? footer.getString() : "(none)").append('\n');

		return sb.toString();
	}
}
