package com.ravengardextras.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.jspecify.annotations.Nullable;

/** Read-only access to the tab-list header/footer text, which only has setters. */
@Mixin(PlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {
	@Accessor("header")
	@Nullable Component ravengardextras$getHeader();

	@Accessor("footer")
	@Nullable Component ravengardextras$getFooter();
}
