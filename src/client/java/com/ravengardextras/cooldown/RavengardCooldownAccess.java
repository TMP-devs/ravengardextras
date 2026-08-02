package com.ravengardextras.cooldown;

import net.minecraft.resources.Identifier;

/**
 * Implemented (via mixin) by {@link net.minecraft.world.item.ItemCooldowns} so we can read the
 * exact remaining tick count for a cooldown group, which the vanilla class only exposes as a
 * clamped 0..1 percentage.
 */
public interface RavengardCooldownAccess {
	/** Remaining cooldown in ticks for the given group, or 0 if not on cooldown. */
	int ravengardextras$remainingTicks(Identifier cooldownGroup);
}
