package com.ravengardextras.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.jspecify.annotations.Nullable;

/** Read-only access to the plugin-channel server brand string (e.g. "Paper", "Velocity"), which has no public getter. */
@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {
	@Accessor("serverBrand")
	@Nullable String ravengardextras$getServerBrand();
}
