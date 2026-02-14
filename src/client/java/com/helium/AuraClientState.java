package com.helium;

import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class AuraClientState {
	private static final Map<Integer, AuraData> AURA_BY_ENTITY_ID = new HashMap<>();
	private static final int STALE_TICKS = 200;

	private AuraClientState() {
	}

	public static void update(int entityId, float current, float max, int lock, int auraColorRgb) {
		AURA_BY_ENTITY_ID.put(entityId, new AuraData(Math.max(0.0F, current), Math.max(1.0F, max), Math.max(0, lock), auraColorRgb, 0));
	}

	public static AuraData get(int entityId) {
		return AURA_BY_ENTITY_ID.get(entityId);
	}

	public static AuraData self() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return null;
		}
		return AURA_BY_ENTITY_ID.get(mc.player.getId());
	}

	public static void tick() {
		Iterator<Map.Entry<Integer, AuraData>> iterator = AURA_BY_ENTITY_ID.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, AuraData> entry = iterator.next();
			AuraData data = entry.getValue();
			if (data.staleTicks > STALE_TICKS) {
				iterator.remove();
				continue;
			}
				entry.setValue(new AuraData(data.current(), data.max(), data.lockTicks(), data.auraColorRgb(), data.staleTicks() + 1));
			}
	}

	public static boolean shouldOpenColorPicker() {
		AuraData selfAura = self();
		return selfAura != null && selfAura.auraColorRgb() == RwbyMod.NO_AURA_COLOR;
	}

	public static void setLocalPlayerColor(int auraColorRgb) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		AuraData data = AURA_BY_ENTITY_ID.get(mc.player.getId());
		if (data == null) {
			return;
		}
		AURA_BY_ENTITY_ID.put(mc.player.getId(), new AuraData(data.current(), data.max(), data.lockTicks(), auraColorRgb, 0));
	}

	public record AuraData(float current, float max, int lockTicks, int auraColorRgb, int staleTicks) {
	}
}
