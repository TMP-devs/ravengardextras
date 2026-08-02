package com.ravengardextras.mixin;

import com.ravengardextras.RavengardExtrasClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blocks moving/swapping/dropping the contents of a locked player-inventory slot, and draws a lock icon over it. */
@Mixin(AbstractContainerScreen.class)
public abstract class SlotLockerMixin extends Screen {

	protected SlotLockerMixin(net.minecraft.network.chat.Component title) {
		super(title);
	}

	@Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
	private void ravengardextras$blockLockedSlotClicks(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		if (slot != null && RavengardExtrasClient.SLOT_LOCKER_CONFIG.isLocked(slot, player)) {
			ci.cancel();
			return;
		}
		// Number-key swap into a locked hotbar slot: the clicked slot is the source
		// (often unlocked), so the target hotbar index only shows up as `button`.
		if (actionType == ContainerInput.SWAP && button >= 0 && button < 9
				&& RavengardExtrasClient.SLOT_LOCKER_CONFIG.isSlotIndexLocked(button)) {
			ci.cancel();
		}
	}

	@Inject(method = "extractSlot", at = @At("TAIL"))
	private void ravengardextras$drawLockIcon(GuiGraphicsExtractor guiGraphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		Player player = Minecraft.getInstance().player;
		if (player == null || !RavengardExtrasClient.SLOT_LOCKER_CONFIG.isLocked(slot, player)) {
			return;
		}
		int ox = slot.x;
		int oy = slot.y;
		final int shackle = 0x9AE8D48A;
		final int body = 0x9AE8D48A;
		final int keyhole = 0x9A000000;

		guiGraphics.fill(ox + 4, oy + 2, ox + 12, oy + 3, shackle);
		guiGraphics.fill(ox + 4, oy + 2, ox + 6, oy + 7, shackle);
		guiGraphics.fill(ox + 10, oy + 2, ox + 12, oy + 7, shackle);
		guiGraphics.fill(ox + 3, oy + 6, ox + 13, oy + 14, body);
		guiGraphics.fill(ox + 7, oy + 8, ox + 9, oy + 13, keyhole);
	}
}
