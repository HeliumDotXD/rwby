package com.helium;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class RwbyAbilityHandler {
	private static final int BURST_COOLDOWN_TICKS = 25;
	private static final double BURST_FORCE = 1.85D;
	private static final double BURST_TRAIL_LENGTH = 6.0D;
	private static final int HOOK_COOLDOWN_TICKS = 30;
	private static final int HOOK_HOLD_TICKS = 24;
	private static final double HOOK_RANGE = 1.5D;
	private static final double HOOK_RANGE_SQR = HOOK_RANGE * HOOK_RANGE;
	private static final double HOOK_PULL_TARGET_FORCE = 1.1D;
	private static final double HOOK_LUNGE_PLAYER_FORCE = 0.55D;
	private static final float HOOK_V_ATTACK_DAMAGE = 1200.0F;

	private static final Map<UUID, PendingHook> PENDING_HOOKS = new HashMap<>();

	private RwbyAbilityHandler() {
	}

	public static void requestBurst(ServerPlayer player) {
		InteractionHand hand = getCrescentRoseHand(player);
		if (hand == null) {
			return;
		}

		if (player.getCooldowns().isOnCooldown(RwbyMod.CRESCENT_ROSE)) {
			return;
		}

		applyBurst(player, hand);
	}

	public static void requestHook(ServerPlayer player) {
		InteractionHand hand = getCrescentRoseHand(player);
		if (hand == null || player.getCooldowns().isOnCooldown(RwbyMod.CRESCENT_ROSE)) {
			return;
		}

		LivingEntity target = findHookTarget(player);
		if (target == null) {
			return;
		}

		player.startUsingItem(hand);
		PENDING_HOOKS.put(player.getUUID(), new PendingHook(HOOK_HOLD_TICKS, hand, target.getId()));
	}

	public static void onServerTick(MinecraftServer server) {
		if (PENDING_HOOKS.isEmpty()) {
			return;
		}

		Iterator<Map.Entry<UUID, PendingHook>> iterator = PENDING_HOOKS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, PendingHook> entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null || !player.isAlive()) {
				iterator.remove();
				continue;
			}

			PendingHook pending = entry.getValue();
			if (pending.ticksLeft > 0) {
				pending.ticksLeft--;
				continue;
			}

			executeHookSlash(player, pending.hand, pending.targetEntityId);
			iterator.remove();
		}
	}

	private static void applyBurst(ServerPlayer player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(RwbyMod.CRESCENT_ROSE)) {
			return;
		}

		Vec3 direction = player.getLookAngle().normalize();
		Vec3 burst = direction.scale(BURST_FORCE);
		playBurstEffects(player, direction);

		player.setDeltaMovement(burst);
		player.fallDistance = 0.0F;
		player.hasImpulse = true;
		player.hurtMarked = true;
		player.getCooldowns().addCooldown(RwbyMod.CRESCENT_ROSE, BURST_COOLDOWN_TICKS);
	}

	private static LivingEntity findHookTarget(ServerPlayer player) {
		Vec3 start = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		Vec3 end = start.add(look.scale(HOOK_RANGE));
		BlockHitResult blockHit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 maxEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
			player.level(),
			player,
			start,
			maxEnd,
			player.getBoundingBox().expandTowards(look.scale(HOOK_RANGE)).inflate(1.0D),
			RwbyAbilityHandler::isValidHookTarget
		);

		if (entityHit == null) {
			return null;
		}

		Entity entity = entityHit.getEntity();
		return entity instanceof LivingEntity living ? living : null;
	}

	private static boolean isValidHookTarget(Entity entity) {
		return entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator();
	}

	private static void executeHookSlash(ServerPlayer player, InteractionHand hand, int targetEntityId) {
		ItemStack stack = player.getItemInHand(hand);
		player.stopUsingItem();
		if (!stack.is(RwbyMod.CRESCENT_ROSE)) {
			return;
		}

		Entity targetEntity = player.serverLevel().getEntity(targetEntityId);
		if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
			return;
		}
		if (player.distanceToSqr(target) > HOOK_RANGE_SQR) {
			return;
		}

		Vec3 toPlayer = player.position().subtract(target.position()).normalize();
		Vec3 toTarget = target.position().subtract(player.position()).normalize();

		target.setDeltaMovement(target.getDeltaMovement().add(toPlayer.scale(HOOK_PULL_TARGET_FORCE)).add(0.0D, 0.28D, 0.0D));
		target.hurtMarked = true;
		player.setDeltaMovement(player.getDeltaMovement().add(toTarget.scale(HOOK_LUNGE_PLAYER_FORCE)).add(0.0D, 0.08D, 0.0D));
		player.hurtMarked = true;
		player.fallDistance = 0.0F;

			target.hurt(RwbyDamageUtil.source(player.level(), RwbyMod.CRESCENT_ROSE_V_DAMAGE_TYPE, player), HOOK_V_ATTACK_DAMAGE);

		Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.5D));
		Vec3 end = target.getBoundingBox().getCenter().add(0.0D, target.getBbHeight() * 0.2D, 0.0D);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), RwbyMod.CRESCENT_ROSE_SHOT, SoundSource.PLAYERS, 1.0F, 1.0F);
		player.serverLevel().sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 8, 0.06D, 0.06D, 0.06D, 0.01D);

		for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
			ServerPlayNetworking.send(watcher, new ShotBeamPayload(player.getId(), start, end));
		}
		ServerPlayNetworking.send(player, new ShotBeamPayload(player.getId(), start, end));
		player.getCooldowns().addCooldown(RwbyMod.CRESCENT_ROSE, HOOK_COOLDOWN_TICKS);
	}

	private static void playBurstEffects(ServerPlayer player, Vec3 forward) {
		var level = player.serverLevel();
		Vec3 start = player.getEyePosition().add(forward.scale(-0.55D));
		Vec3 end = start.add(forward.scale(-BURST_TRAIL_LENGTH));

		level.playSound(null, player.getX(), player.getY(), player.getZ(), RwbyMod.CRESCENT_ROSE_SHOT, SoundSource.PLAYERS, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.FLASH, start.x, start.y, start.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

		for (int i = 1; i <= 6; i++) {
			Vec3 p = start.lerp(end, i / 6.0D);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}

		for (ServerPlayer target : PlayerLookup.tracking(player)) {
			ServerPlayNetworking.send(target, new ShotBeamPayload(player.getId(), start, end));
		}
		ServerPlayNetworking.send(player, new ShotBeamPayload(player.getId(), start, end));
	}

	private static InteractionHand getCrescentRoseHand(ServerPlayer player) {
		if (player.getMainHandItem().is(RwbyMod.CRESCENT_ROSE)) {
			return InteractionHand.MAIN_HAND;
		}
		if (player.getOffhandItem().is(RwbyMod.CRESCENT_ROSE)) {
			return InteractionHand.OFF_HAND;
		}
		return null;
	}

	private static final class PendingHook {
		private int ticksLeft;
		private final InteractionHand hand;
		private final int targetEntityId;

		private PendingHook(int ticksLeft, InteractionHand hand, int targetEntityId) {
			this.ticksLeft = ticksLeft;
			this.hand = hand;
			this.targetEntityId = targetEntityId;
		}
	}
}
