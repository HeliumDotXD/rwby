package com.helium;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AuraShatterPayload(int entityId) implements CustomPacketPayload {
	public static final Type<AuraShatterPayload> TYPE = new Type<>(RwbyMod.AURA_SHATTER_PACKET_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, AuraShatterPayload> CODEC = StreamCodec.of(
		(buffer, payload) -> buffer.writeInt(payload.entityId),
		buffer -> new AuraShatterPayload(buffer.readInt())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
