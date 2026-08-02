package com.ravengardextras.mixin;

import com.ravengardextras.RavengardExtrasClient;
import com.ravengardextras.gearhighlighter.CrownParser;
import com.ravengardextras.gearhighlighter.GearHighlighterConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Purely visual: draws a border over slots whose item lore crosses a Crowns threshold. No item/state mutation. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {

	protected AbstractContainerScreenMixin(net.minecraft.network.chat.Component title) {
		super(title);
	}

	@Inject(method = "extractSlot", at = @At("TAIL"))
	private void ravengardextras$extractSlot(GuiGraphicsExtractor guiGraphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		if (!RavengardExtrasClient.CONFIG.enabled) {
			return;
		}
		ItemStack stack = slot.getItem();
		if (stack.isEmpty()) {
			return;
		}
		if (RavengardExtrasClient.CONFIG.isHealItem(stack.getHoverName().getString())) {
			guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, RavengardExtrasClient.CONFIG.healColor);
			return;
		}
		long crowns = CrownParser.findCrowns(stack);
		if (crowns < 0) {
			return;
		}
		int color = RavengardExtrasClient.CONFIG.colorFor(crowns);
		if (color == 0) {
			return;
		}
		boolean rainbow = color == GearHighlighterConfig.RAINBOW;
		if (rainbow) {
			float hue = (System.currentTimeMillis() % 2000L) / 2000.0F;
			color = 0xFF000000 | Mth.hsvToRgb(hue, 1.0F, 1.0F);
		}
		int x = slot.x;
		int y = slot.y;
		int x2 = x + 16;
		int y2 = y + 16;
		if (rainbow) {
			// A rotating hue can land on a color close to the item's own icon and
			// disappear at a glance, so give it a fixed-contrast black ring just
			// outside the colored one — always visible no matter the hue.
			int ox = x - 1;
			int oy = y - 1;
			int ox2 = x2 + 1;
			int oy2 = y2 + 1;
			guiGraphics.fill(ox, oy, ox2, oy + 1, 0xFF000000);
			guiGraphics.fill(ox, oy2 - 1, ox2, oy2, 0xFF000000);
			guiGraphics.fill(ox, oy, ox + 1, oy2, 0xFF000000);
			guiGraphics.fill(ox2 - 1, oy, ox2, oy2, 0xFF000000);
		}
		guiGraphics.fill(x, y, x2, y + 1, color);
		guiGraphics.fill(x, y2 - 1, x2, y2, color);
		guiGraphics.fill(x, y, x + 1, y2, color);
		guiGraphics.fill(x2 - 1, y, x2, y2, color);
	}
}
