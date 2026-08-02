package com.ravengardextras.gearrules;

import com.ravengardextras.gearhighlighter.CrownParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parsed rule-relevant stats for a single item, pulled from its lore text and hidden class glyphs. */
public class ItemStats {
	private static final Pattern PERCENT_STAT = Pattern.compile(
			"\\+?(\\d+(?:\\.\\d+)?)%\\s*(Damage|Defen[cs]e|Stamina Regeneration|Ability Cooldown Reduction)",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern FLAT_DAMAGE = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*Damage\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern FLAT_DEFENSE = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*Defen[cs]e\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern ATTACK_SPEED = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*Attack Speed\\b", Pattern.CASE_INSENSITIVE);

	public Double crownValue;
	public Double defense;
	public Double damage;
	public Double attackSpeed;
	public Double staminaRegenPercent;
	public Double abilityCdrPercent;
	public Double percentDamage;
	public Double percentDefense;
	public final Set<GearClass> classes = EnumSet.noneOf(GearClass.class);

	public static ItemStats of(ItemStack stack) {
		ItemStats stats = new ItemStats();
		if (stack.isEmpty()) {
			return stats;
		}

		long crowns = CrownParser.findCrowns(stack);
		if (crowns >= 0) {
			stats.crownValue = (double) crowns;
		}

		List<String> text = new ArrayList<>();
		text.add(stack.getHoverName().getString());
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore != null) {
			for (Component line : lore.lines()) {
				text.add(line.getString());
			}
		}

		for (GearClass gearClass : GearClass.values()) {
			for (String line : text) {
				if (line.indexOf(gearClass.glyph) >= 0) {
					stats.classes.add(gearClass);
					break;
				}
			}
		}

		for (String line : text) {
			Matcher percentMatcher = PERCENT_STAT.matcher(line);
			while (percentMatcher.find()) {
				double value = Double.parseDouble(percentMatcher.group(1));
				String stat = percentMatcher.group(2).toLowerCase();
				if (stat.startsWith("damage")) {
					stats.percentDamage = value;
				} else if (stat.startsWith("defen")) {
					stats.percentDefense = value;
				} else if (stat.startsWith("stamina")) {
					stats.staminaRegenPercent = value;
				} else if (stat.startsWith("ability")) {
					stats.abilityCdrPercent = value;
				}
			}

			if (stats.damage == null) {
				Matcher m = FLAT_DAMAGE.matcher(line);
				if (m.find()) {
					stats.damage = Double.parseDouble(m.group(1));
				}
			}
			if (stats.defense == null) {
				Matcher m = FLAT_DEFENSE.matcher(line);
				if (m.find()) {
					stats.defense = Double.parseDouble(m.group(1));
				}
			}
			if (stats.attackSpeed == null) {
				Matcher m = ATTACK_SPEED.matcher(line);
				if (m.find()) {
					stats.attackSpeed = Double.parseDouble(m.group(1));
				}
			}
		}

		return stats;
	}

	public Double value(GearParam param) {
		return switch (param) {
			case CROWN_VALUE -> this.crownValue;
			case DEFENSE -> this.defense;
			case DAMAGE -> this.damage;
			case ATTACK_SPEED -> this.attackSpeed;
			case STAMINA_REGEN -> this.staminaRegenPercent;
			case ABILITY_CDR -> this.abilityCdrPercent;
			case PERCENT_DAMAGE -> this.percentDamage;
			case PERCENT_DEFENSE -> this.percentDefense;
			case CLASS -> null;
		};
	}
}
