package com.helium;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class AuraColorResolver {
	private static final Vector3f DEFAULT_AURA_COLOR = new Vector3f(0.45F, 0.78F, 1.0F);
	private static final Map<ResourceLocation, Vector3f> CACHE = new HashMap<>();

	private AuraColorResolver() {
	}

	public static Vector3f resolve(Player player) {
		AuraClientState.AuraData synced = AuraClientState.get(player.getId());
		if (synced != null && synced.auraColorRgb() != RwbyMod.NO_AURA_COLOR) {
			return fromRgb(synced.auraColorRgb());
		}

		ResourceLocation texture = resolveSkinLocation(player);
		if (texture == null) {
			return DEFAULT_AURA_COLOR;
		}
		return resolveFromTexture(texture);
	}

	public static Vector3f resolve(LivingEntity entity) {
		AuraClientState.AuraData synced = AuraClientState.get(entity.getId());
		if (synced != null && synced.auraColorRgb() != RwbyMod.NO_AURA_COLOR) {
			return fromRgb(synced.auraColorRgb());
		}

		if (entity instanceof Player player) {
			return resolve(player);
		}

		ResourceLocation texture = resolveEntityTexture(entity);
		if (texture == null) {
			return DEFAULT_AURA_COLOR;
		}
		return resolveFromTexture(texture);
	}

	private static Vector3f resolveFromTexture(ResourceLocation texture) {
		Vector3f cached = CACHE.get(texture);
		if (cached != null) {
			return cached;
		}

		NativeImage image = resolveSkinImage(texture);
		if (image == null) {
			CACHE.put(texture, DEFAULT_AURA_COLOR);
			return DEFAULT_AURA_COLOR;
		}

		Map<Integer, Integer> counts = new HashMap<>();
		int bestColor = -1;
		int bestCount = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getPixelRGBA(x, y);
				int alpha = (argb >>> 24) & 0xFF;
				if (alpha < 48) {
					continue;
				}
				int rgb = argb & 0x00FFFFFF;
				int count = counts.getOrDefault(rgb, 0) + 1;
				counts.put(rgb, count);
					if (count > bestCount) {
						bestCount = count;
						bestColor = rgb;
					}
				}
			}

			if (bestColor == -1) {
				CACHE.put(texture, DEFAULT_AURA_COLOR);
				return DEFAULT_AURA_COLOR;
			}

		float r = ((bestColor >> 16) & 0xFF) / 255.0F;
		float g = ((bestColor >> 8) & 0xFF) / 255.0F;
		float b = (bestColor & 0xFF) / 255.0F;
		Vector3f resolved = new Vector3f(Mth.clamp(r, 0.0F, 1.0F), Mth.clamp(g, 0.0F, 1.0F), Mth.clamp(b, 0.0F, 1.0F));
		CACHE.put(texture, resolved);
		return resolved;
	}

	private static ResourceLocation resolveSkinLocation(Player player) {
		try {
			Method getSkin = player.getClass().getMethod("getSkin");
			Object skin = getSkin.invoke(player);
			if (skin != null) {
				Method texture = skin.getClass().getMethod("texture");
				Object result = texture.invoke(skin);
				if (result instanceof ResourceLocation rl) {
					return rl;
				}
			}
		} catch (ReflectiveOperationException ignored) {
		}

		try {
			Method legacy = player.getClass().getMethod("getSkinTextureLocation");
			Object result = legacy.invoke(player);
			if (result instanceof ResourceLocation rl) {
				return rl;
			}
		} catch (ReflectiveOperationException ignored) {
		}

		return null;
	}

	private static NativeImage resolveSkinImage(ResourceLocation skin) {
		AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(skin);
		if (texture == null) {
			return null;
		}

		try {
			Method getPixels = texture.getClass().getMethod("getPixels");
			Object result = getPixels.invoke(texture);
			if (result instanceof NativeImage image) {
				return image;
			}
		} catch (ReflectiveOperationException ignored) {
		}

		Class<?> c = texture.getClass();
		while (c != null) {
			for (Field field : c.getDeclaredFields()) {
				if (!NativeImage.class.isAssignableFrom(field.getType())) {
					continue;
				}
				try {
					field.setAccessible(true);
					Object result = field.get(texture);
					if (result instanceof NativeImage image) {
						return image;
					}
				} catch (ReflectiveOperationException ignored) {
				}
			}
			c = c.getSuperclass();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static ResourceLocation resolveEntityTexture(LivingEntity entity) {
		try {
			EntityRenderer<? super Entity> renderer = (EntityRenderer<? super Entity>) Minecraft.getInstance()
				.getEntityRenderDispatcher()
				.getRenderer(entity);
			return renderer.getTextureLocation(entity);
		} catch (RuntimeException ignored) {
		}

		return null;
	}

	private static Vector3f fromRgb(int rgb) {
		float r = ((rgb >> 16) & 0xFF) / 255.0F;
		float g = ((rgb >> 8) & 0xFF) / 255.0F;
		float b = (rgb & 0xFF) / 255.0F;
		return new Vector3f(Mth.clamp(r, 0.0F, 1.0F), Mth.clamp(g, 0.0F, 1.0F), Mth.clamp(b, 0.0F, 1.0F));
	}
}
