package com.helium;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AuraSyncPayload(int entityId, float current, float max, int lockTicks, int auraColorRgb) implements CustomPacketPayload {
	public static final Type<AuraSyncPayload> TYPE = new Type<>(RwbyMod.AURA_SYNC_PACKET_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, AuraSyncPayload> CODEC = StreamCodec.of(
		(buffer, payload) -> {
			buffer.writeInt(payload.entityId);
				buffer.writeFloat(payload.current);
				buffer.writeFloat(payload.max);
				buffer.writeInt(payload.lockTicks);
				buffer.writeInt(payload.auraColorRgb);
			},
			buffer -> new AuraSyncPayload(buffer.readInt(), buffer.readFloat(), buffer.readFloat(), buffer.readInt(), buffer.readInt())
		);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
