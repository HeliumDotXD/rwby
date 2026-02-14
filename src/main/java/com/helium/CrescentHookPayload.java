package com.helium;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CrescentHookPayload() implements CustomPacketPayload {
	public static final CrescentHookPayload INSTANCE = new CrescentHookPayload();
	public static final Type<CrescentHookPayload> TYPE = new Type<>(RwbyMod.CRESCENT_HOOK_PACKET_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, CrescentHookPayload> CODEC = StreamCodec.of(
		(buffer, payload) -> {
		},
		buffer -> INSTANCE
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
