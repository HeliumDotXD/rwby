package com.helium;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AuraColorSetPayload(String colorId) implements CustomPacketPayload {
	public static final Type<AuraColorSetPayload> TYPE = new Type<>(RwbyMod.AURA_COLOR_SET_PACKET_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, AuraColorSetPayload> CODEC = StreamCodec.of(
		(buffer, payload) -> buffer.writeUtf(payload.colorId, 64),
		buffer -> new AuraColorSetPayload(buffer.readUtf(64))
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
