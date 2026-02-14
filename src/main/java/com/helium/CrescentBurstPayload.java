package com.helium;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CrescentBurstPayload() implements CustomPacketPayload {
	public static final CrescentBurstPayload INSTANCE = new CrescentBurstPayload();
	public static final Type<CrescentBurstPayload> TYPE = new Type<>(RwbyMod.CRESCENT_BURST_PACKET_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, CrescentBurstPayload> CODEC = StreamCodec.of(
		(buffer, payload) -> {
		},
		buffer -> INSTANCE
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
