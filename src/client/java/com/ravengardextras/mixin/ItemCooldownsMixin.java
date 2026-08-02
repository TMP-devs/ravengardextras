package com.ravengardextras.mixin;

import com.ravengardextras.cooldown.RavengardCooldownAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Records the absolute end tick of each cooldown group as it starts, so we can report the exact
 * seconds remaining. Vanilla {@link ItemCooldowns} only keeps this in a private record and exposes
 * a clamped percentage, which isn't enough to render a countdown.
 */
@Mixin(ItemCooldowns.class)
public abstract class ItemCooldownsMixin implements RavengardCooldownAccess {

	@Shadow
	private int tickCount;

	@Unique
	private final Map<Identifier, Integer> ravengardextras$endTicks = new HashMap<>();

	@Inject(method = "onCooldownStarted", at = @At("HEAD"))
	private void ravengardextras$onStart(Identifier cooldownGroup, int duration, CallbackInfo ci) {
		ravengardextras$endTicks.put(cooldownGroup, tickCount + duration);
	}

	@Inject(method = "onCooldownEnded", at = @At("HEAD"))
	private void ravengardextras$onEnd(Identifier cooldownGroup, CallbackInfo ci) {
		ravengardextras$endTicks.remove(cooldownGroup);
	}

	@Override
	public int ravengardextras$remainingTicks(Identifier cooldownGroup) {
		Integer end = ravengardextras$endTicks.get(cooldownGroup);
		return end == null ? 0 : Math.max(0, end - tickCount);
	}
}
