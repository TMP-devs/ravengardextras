package com.ravengardextras.debug;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Builds a compact, comparable one-line signature of an item's full state - id, count, damage, name
 * and every data component (including {@code custom_model_data}). Used to spot what changes on an
 * ability item as it cools down.
 */
public final class ItemDump {

	private ItemDump() {
	}

	public static String signature(ItemStack stack) {
		if (stack.isEmpty()) {
			return "(empty)";
		}
		return "id=" + BuiltInRegistries.ITEM.getKey(stack.getItem())
				+ "  count=" + stack.getCount()
				+ "  dmg=" + stack.getDamageValue() + "/" + stack.getMaxDamage()
				+ "  name=\"" + stack.getHoverName().getString() + "\""
				+ "  components=" + stack.getComponents();
	}
}
