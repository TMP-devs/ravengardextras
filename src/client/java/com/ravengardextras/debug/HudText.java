package com.ravengardextras.debug;

import com.ravengardextras.mixin.BossHealthOverlayAccessor;
import com.ravengardextras.mixin.HudAccessor;
import com.ravengardextras.mixin.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Reads the text HUD sources (action bar, boss bars, tab header/footer) where a server-drawn
 *  cooldown number is most likely to appear. */
public final class HudText {

	private HudText() {
	}

	/** Current action-bar text, or "(none)" if none is showing. */
	public static String actionBar() {
		Minecraft mc = Minecraft.getInstance();
		HudAccessor hud = (HudAccessor) mc.gui.hud;
		Component msg = hud.ravengardextras$overlayMessage();
		if (msg == null || hud.ravengardextras$overlayMessageTime() <= 0) {
			return "(none)";
		}
		return msg.getString();
	}

	/** One line per active boss bar: "NN%  text". Empty list if none. */
	public static List<String> bossBars() {
		Minecraft mc = Minecraft.getInstance();
		BossHealthOverlayAccessor overlay = (BossHealthOverlayAccessor) mc.gui.hud.getBossOverlay();
		List<String> out = new ArrayList<>();
		for (LerpingBossEvent event : overlay.ravengardextras$events().values()) {
			out.add(String.format("%3.0f%%  %s", event.getProgress() * 100.0F, event.getName().getString()));
		}
		return out;
	}

	public static String tabHeader() {
		Component header = ((PlayerTabOverlayAccessor) Minecraft.getInstance().gui.hud.getTabList()).ravengardextras$header();
		return header == null ? "(none)" : header.getString();
	}

	public static String tabFooter() {
		Component footer = ((PlayerTabOverlayAccessor) Minecraft.getInstance().gui.hud.getTabList()).ravengardextras$footer();
		return footer == null ? "(none)" : footer.getString();
	}

	/** Full one-shot snapshot of every text source, for a manual dump. */
	public static List<String> snapshot() {
		List<String> out = new ArrayList<>();
		out.add("[action bar] " + actionBar());
		List<String> bars = bossBars();
		out.add("[boss bars] " + (bars.isEmpty() ? "(none)" : ""));
		for (String bar : bars) {
			out.add("  " + bar);
		}
		out.add("[tab header] " + tabHeader());
		out.add("[tab footer] " + tabFooter());
		out.add("[scoreboard]");
		out.addAll(ScoreboardDump.sidebarLines());
		return out;
	}
}
