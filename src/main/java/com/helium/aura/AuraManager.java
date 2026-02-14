package com.helium.aura;

import com.helium.AuraShatterPayload;
import com.helium.AuraSyncPayload;
import com.helium.RwbyMod;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class AuraManager {
	private static int syncTicker = 0;
	private static final double SYNC_RADIUS = 48.0D;
	private static final Map<UUID, AuraRegenBoost> AURA_REGEN_BOOSTS = new HashMap<>();

	private AuraManager() {
	}

	public static void onServerTick(MinecraftServer server) {
		tickAuraRegenBoosts(server);

		syncTicker++;
		if (syncTicker < 10) {
			return;
		}
		syncTicker = 0;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.level().getGameRules().getBoolean(RwbyMod.VISIBLE_AURA)) {
				continue;
			}

			for (LivingEntity living : player.serverLevel().getEntitiesOfClass(
				LivingEntity.class,
				player.getBoundingBox().inflate(SYNC_RADIUS),
				LivingEntity::isAlive
			)) {
				if (!(living instanceof AuraHolder aura)) {
					continue;
				}

				ServerPlayNetworking.send(
					player,
						new AuraSyncPayload(
							living.getId(),
							(float) aura.rwby$getAuraCurrent(),
							(float) aura.rwby$getAuraMax(),
							aura.rwby$getAuraLockTicks(),
							resolveAuraColor(living)
						)
					);
			}
		}
	}

	public static void restoreAura(LivingEntity entity, double amount) {
		if (amount <= 0.0D || !(entity instanceof AuraHolder aura) || !aura.rwby$isAuraInitialized()) {
			return;
		}
		if (aura.rwby$getAuraLockTicks() > 0) {
			return;
		}
		aura.rwby$setAuraCurrent(Math.min(aura.rwby$getAuraMax(), aura.rwby$getAuraCurrent() + amount));
	}

	public static void addAuraRegenBoost(LivingEntity entity, double auraPerTick, int durationTicks) {
		if (durationTicks <= 0 || auraPerTick <= 0.0D) {
			return;
		}
		AURA_REGEN_BOOSTS.put(entity.getUUID(), new AuraRegenBoost(auraPerTick, durationTicks));
	}

	public static void onAuraShatter(LivingEntity entity) {
		AuraShatterPayload payload = new AuraShatterPayload(entity.getId());
		for (ServerPlayer watcher : PlayerLookup.tracking(entity)) {
			ServerPlayNetworking.send(watcher, payload);
		}
		if (entity instanceof ServerPlayer player) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private static int resolveAuraColor(LivingEntity living) {
		if (living instanceof ServerPlayer player) {
			PlayerAuraComponent aura = AuraComponents.getPlayerAura(player);
			if (aura != null) {
				return RwbyMod.resolveAuraColorRgb(aura.rwby$getAuraColorId());
			}
		}
		return RwbyMod.NO_AURA_COLOR;
	}

	private static void tickAuraRegenBoosts(MinecraftServer server) {
		if (AURA_REGEN_BOOSTS.isEmpty()) {
			return;
		}

		Iterator<Map.Entry<UUID, AuraRegenBoost>> iterator = AURA_REGEN_BOOSTS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, AuraRegenBoost> entry = iterator.next();
			AuraRegenBoost boost = entry.getValue();
			if (boost.remainingTicks <= 0) {
				iterator.remove();
				continue;
			}

			Entity entity = server.getPlayerList().getPlayer(entry.getKey());
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				iterator.remove();
				continue;
			}

			restoreAura(living, boost.auraPerTick);
			boost.remainingTicks--;
			if (boost.remainingTicks <= 0) {
				iterator.remove();
			}
		}
	}

	private static final class AuraRegenBoost {
		private final double auraPerTick;
		private int remainingTicks;

		private AuraRegenBoost(double auraPerTick, int remainingTicks) {
			this.auraPerTick = auraPerTick;
			this.remainingTicks = remainingTicks;
		}
	}
}
