package com.helium;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record ShotBeamPayload(int shooterEntityId, Vec3 start, Vec3 end) implements CustomPacketPayload {
	public static final Type<ShotBeamPayload> TYPE = new Type<>(RwbyMod.SHOT_BEAM_PACKET_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, ShotBeamPayload> CODEC = StreamCodec.of(
		(buffer, payload) -> payload.write(buffer),
		ShotBeamPayload::new
	);

	private ShotBeamPayload(RegistryFriendlyByteBuf buffer) {
		this(
			buffer.readInt(),
			new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat()),
			new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat())
		);
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeInt(shooterEntityId);
		buffer.writeFloat((float) start.x);
		buffer.writeFloat((float) start.y);
		buffer.writeFloat((float) start.z);
		buffer.writeFloat((float) end.x);
		buffer.writeFloat((float) end.y);
		buffer.writeFloat((float) end.z);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
