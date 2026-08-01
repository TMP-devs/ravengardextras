package com.ravengardextras.gearhighlighter;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the "N Crowns" line out of an item's lore. Plain string scan, no NBT writes, no network traffic. */
public final class CrownParser {
	private static final Pattern CROWNS_PATTERN =
			Pattern.compile("([\\d,]+(?:\\.\\d+)?)\\s*([kKmM]?)\\s*Crowns");

	private CrownParser() {
	}

	/** Returns the crown value found in the stack's lore, or -1 if none. */
	public static long findCrowns(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return -1;
		}
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) {
			return -1;
		}
		List<Component> lines = lore.lines();
		for (Component line : lines) {
			long value = parseLine(line.getString());
			if (value >= 0) {
				return value;
			}
		}
		return -1;
	}

	private static long parseLine(String text) {
		Matcher matcher = CROWNS_PATTERN.matcher(text);
		if (!matcher.find()) {
			return -1;
		}
		String numberPart = matcher.group(1).replace(",", "");
		String suffix = matcher.group(2);
		double value;
		try {
			value = Double.parseDouble(numberPart);
		} catch (NumberFormatException e) {
			return -1;
		}
		if (suffix != null) {
			if (suffix.equalsIgnoreCase("k")) {
				value *= 1_000;
			} else if (suffix.equalsIgnoreCase("m")) {
				value *= 1_000_000;
			}
		}
		return Math.round(value);
	}
}
