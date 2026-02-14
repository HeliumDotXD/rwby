package com.helium;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AuraOpenColorScreenPayload() implements CustomPacketPayload {
	public static final AuraOpenColorScreenPayload INSTANCE = new AuraOpenColorScreenPayload();
	public static final Type<AuraOpenColorScreenPayload> TYPE = new Type<>(RwbyMod.AURA_OPEN_COLOR_SCREEN_PACKET_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, AuraOpenColorScreenPayload> CODEC = StreamCodec.of(
		(buffer, payload) -> {
		},
		buffer -> INSTANCE
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
