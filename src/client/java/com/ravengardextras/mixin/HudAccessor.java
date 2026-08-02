package com.ravengardextras.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the private action-bar (overlay message) state so it can be dumped for investigation. */
@Mixin(Hud.class)
public interface HudAccessor {
	@Accessor("overlayMessageString")
	Component ravengardextras$overlayMessage();

	@Accessor("overlayMessageTime")
	int ravengardextras$overlayMessageTime();
}
