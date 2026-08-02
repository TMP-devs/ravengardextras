package com.ravengardextras.gearhighlighter;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the "Heals +N HP" line out of an item's lore. Plain string scan, same shape as CrownParser. */
public final class HealParser {
	private static final Pattern HEAL_PATTERN = Pattern.compile("Heals\\s*\\+?(\\d+(?:\\.\\d+)?)\\s*HP", Pattern.CASE_INSENSITIVE);

	private HealParser() {
	}

	/** Returns the HP value found in the stack's lore, or -1 if none. */
	public static long findHeal(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return -1;
		}
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) {
			return -1;
		}
		List<Component> lines = lore.lines();
		for (Component line : lines) {
			Matcher matcher = HEAL_PATTERN.matcher(line.getString());
			if (matcher.find()) {
				try {
					return Math.round(Double.parseDouble(matcher.group(1)));
				} catch (NumberFormatException ignored) {
					return -1;
				}
			}
		}
		return -1;
	}
}
