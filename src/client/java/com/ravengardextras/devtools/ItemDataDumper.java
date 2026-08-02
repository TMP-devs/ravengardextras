package com.ravengardextras.devtools;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/** Dev-only: dumps every slot's item id + full data-component set as plain text. */
public final class ItemDataDumper {
	private ItemDataDumper() {
	}

	public static String dump(AbstractContainerMenu menu) {
		StringBuilder sb = new StringBuilder();
		for (Slot slot : menu.slots) {
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) {
				continue;
			}
			sb.append("slot ").append(slot.index)
					.append(" | ").append(BuiltInRegistries.ITEM.getKey(stack.getItem()))
					.append(" x").append(stack.getCount())
					.append(" | \"").append(stack.getHoverName().getString()).append("\"\n");
			sb.append("    [labels] ").append(describeLabels(stack)).append('\n');
			for (TypedDataComponent<?> component : stack.getComponents()) {
				sb.append("    ").append(component.type()).append(" = ").append(component.value()).append('\n');
			}
		}
		if (sb.isEmpty()) {
			sb.append("(no items found)");
		}
		return sb.toString();
	}

	/** Ravengard hides class/rarity/type tags as invisible PUA glyphs in the name/lore text. */
	private static String describeLabels(ItemStack stack) {
		List<String> text = new ArrayList<>();
		text.add(stack.getHoverName().getString());
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore != null) {
			for (Component line : lore.lines()) {
				text.add(line.getString());
			}
		}

		List<String> found = new ArrayList<>();
		for (RavengardLabels.Glyph glyph : RavengardLabels.findAll(text)) {
			found.add(glyph.kind + "=" + glyph);
		}
		return found.isEmpty() ? "(none detected)" : String.join(", ", found);
	}
}
