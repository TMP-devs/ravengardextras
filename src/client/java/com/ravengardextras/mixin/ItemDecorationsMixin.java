package com.ravengardextras.mixin;

import com.ravengardextras.RavengardExtrasClient;
import com.ravengardextras.gearhighlighter.HealParser;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The 4-arg itemDecorations overload just forwards to this 5-arg one with a null count
 * override, and every call site - hotbar, inventory, chests, any container screen - goes
 * through this single method, so hooking it here covers all of them at once. Only swaps
 * in the heal amount when the caller didn't already ask for a specific override string
 * (e.g. the yellow quick-craft count), so that still takes priority over the heal amount.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class ItemDecorationsMixin {

	@Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
			at = @At("HEAD"), cancellable = true)
	private void ravengardextras$healAmount(Font font, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
		if (countOverride != null || !RavengardExtrasClient.CONFIG.healAmountEnabled) {
			return;
		}
		long heal = HealParser.findHeal(stack);
		if (heal < 0) {
			return;
		}
		((GuiGraphicsExtractor) (Object) this).itemDecorations(font, stack, x, y, Long.toString(heal));
		ci.cancel();
	}
}
