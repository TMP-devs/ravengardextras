package com.ravengardextras.debug;

import com.ravengardextras.mixin.BossHealthOverlayAccessor;
import com.ravengardextras.mixin.HudAccessor;
import com.ravengardextras.mixin.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

	/** Detailed line per active boss bar: id, color, progress and the name with hidden glyphs
	 *  shown as \\uXXXX codepoints. Empty list if none. */
	public static List<String> bossBars() {
		List<String> out = new ArrayList<>();
		for (Map.Entry<UUID, LerpingBossEvent> entry : events().entrySet()) {
			LerpingBossEvent event = entry.getValue();
			out.add(String.format("id=%s  color=%s  %5.1f%%  name=\"%s\"",
					entry.getKey().toString().substring(0, 8),
					event.getColor(),
					event.getProgress() * 100.0F,
					escape(event.getName().getString())));
		}
		return out;
	}

	/** Coarse signature (integer percents) used to detect meaningful boss-bar changes without
	 *  logging every lerp tick. */
	public static String bossKey() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<UUID, LerpingBossEvent> entry : events().entrySet()) {
			LerpingBossEvent event = entry.getValue();
			sb.append(entry.getKey().toString(), 0, 8)
					.append(event.getColor())
					.append(Math.round(event.getProgress() * 100.0F))
					.append(escape(event.getName().getString()))
					.append('|');
		}
		return sb.toString();
	}

	private static Map<UUID, LerpingBossEvent> events() {
		return ((BossHealthOverlayAccessor) Minecraft.getInstance().gui.hud.getBossOverlay()).ravengardextras$events();
	}

	/** Renders non-printable/custom-font glyphs as \\uXXXX so hidden boss-bar names are visible. */
	private static String escape(String s) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= 0x20 && c < 0x7F) {
				b.append(c);
			} else {
				b.append(String.format("\\u%04x", (int) c));
			}
		}
		return b.toString();
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
