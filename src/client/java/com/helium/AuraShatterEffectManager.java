package com.helium;

import net.minecraft.client.Minecraft;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class AuraShatterEffectManager {
	private static final int EFFECT_TICKS = 22;
	private static final Map<Integer, Integer> ACTIVE = new HashMap<>();
	private static final Vector3f DEFAULT_COLOR = new Vector3f(0.45F, 0.78F, 1.0F);

	private AuraShatterEffectManager() {
	}

	public static void start(int entityId) {
		ACTIVE.put(entityId, EFFECT_TICKS);
	}

	public static float getFlashStrength(Entity entity, float partialTick) {
		Integer ticksLeft = ACTIVE.get(entity.getId());
		if (ticksLeft == null || ticksLeft <= 0) {
			return 0.0F;
		}

		float life = Math.max(0.0F, (ticksLeft - partialTick) / (float) EFFECT_TICKS);
		float pulse = 0.55F + 0.45F * (float) Math.sin((entity.tickCount + partialTick) * 1.6F);
		return life * pulse;
	}

	public static int getEmissiveColor(Entity entity, float intensity) {
		Vector3f color = resolveColor(entity);
		float r = Math.min(1.0F, color.x * (1.55F + intensity * 0.45F));
		float g = Math.min(1.0F, color.y * (1.55F + intensity * 0.45F));
		float b = Math.min(1.0F, color.z * (1.55F + intensity * 0.45F));
		float alpha = Math.min(1.0F, 0.5F + intensity * 0.5F);
		return FastColor.ARGB32.colorFromFloat(alpha, r, g, b);
	}

	public static int getGlintColor(float intensity) {
		float c = Math.min(1.0F, 0.82F + intensity * 0.18F);
		float alpha = Math.min(1.0F, 0.25F + intensity * 0.45F);
		return FastColor.ARGB32.colorFromFloat(alpha, c, c, c);
	}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || ACTIVE.isEmpty()) {
			return;
		}

		Iterator<Map.Entry<Integer, Integer>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, Integer> entry = iterator.next();
			Entity entity = mc.level.getEntity(entry.getKey());
			int ticksLeft = entry.getValue();
			if (entity == null || ticksLeft <= 0) {
				iterator.remove();
				continue;
			}

			entry.setValue(ticksLeft - 1);
		}
	}

	private static Vector3f resolveColor(Entity entity) {
		if (entity instanceof LivingEntity living) {
			return AuraColorResolver.resolve(living);
		}
		return DEFAULT_COLOR;
	}
}
