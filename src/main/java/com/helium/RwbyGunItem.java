package com.helium;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RwbyGunItem extends Item {
	private static final float BULLET_DAMAGE = 400.0F;
	private static final double SHOT_RANGE = 64.0D;
	private static final int COOLDOWN_TICKS = 10;
	private static final float MIN_RECOIL_PITCH = 75.0F;
	private static final float MAX_RECOIL_PITCH = 90.0F;
	private static final double GROUND_RECOIL_HORIZONTAL = 1.15D;
	private static final double GROUND_RECOIL_VERTICAL = 1.05D;
	private static final double AIR_RECOIL_HORIZONTAL_FORCE = 1.05D;
	private static final double AIR_RECOIL_VERTICAL_FORCE = 0.7D;
	private static final double AIR_LIFT_VERTICAL_BONUS_PER_LEVEL = 0.3D;

	public RwbyGunItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack gunStack = player.getItemInHand(hand);

		if (!level.isClientSide()) {
			fireHitscan(level, player);
			applyRecoilLaunch(player, gunStack);
		}

		level.playSound(
			null,
			player.getX(),
			player.getY(),
			player.getZ(),
			RwbyMod.CRESCENT_ROSE_SHOT,
			SoundSource.PLAYERS,
			1.0F,
			1.0F
		);

		player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
		player.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResultHolder.sidedSuccess(gunStack, level.isClientSide());
	}

	private void fireHitscan(Level level, Player player) {
		Vec3 start = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		Vec3 end = start.add(look.scale(SHOT_RANGE));

		BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 impact = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
			level,
			player,
			start,
			impact,
			player.getBoundingBox().expandTowards(look.scale(SHOT_RANGE)).inflate(1.0D),
			this::canHit
		);

			if (entityHit != null) {
				Entity hitEntity = entityHit.getEntity();
				impact = hitEntity.getBoundingBox().getCenter().add(0.0D, hitEntity.getBbHeight() * 0.15D, 0.0D);
				hitEntity.hurt(RwbyDamageUtil.source(level, RwbyMod.CRESCENT_ROSE_SHOT_DAMAGE_TYPE, player), BULLET_DAMAGE);
			}

		sendShotBeam(level, player, start.add(look.scale(0.6D)), impact);
	}

	private boolean canHit(Entity entity) {
		return entity.isPickable() && entity instanceof LivingEntity;
	}

	private void sendShotBeam(Level level, Player shooter, Vec3 beamStart, Vec3 beamEnd) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		for (ServerPlayer target : PlayerLookup.tracking(shooter)) {
			sendBeamPacket(target, shooter.getId(), beamStart, beamEnd);
		}
		if (shooter instanceof ServerPlayer shooterPlayer) {
			sendBeamPacket(shooterPlayer, shooter.getId(), beamStart, beamEnd);
		}
	}

	private void sendBeamPacket(ServerPlayer target, int shooterEntityId, Vec3 beamStart, Vec3 beamEnd) {
		ServerPlayNetworking.send(target, new ShotBeamPayload(shooterEntityId, beamStart, beamEnd));
	}

	private void applyRecoilLaunch(Player player, ItemStack gunStack) {
		boolean grounded = player.onGround();
		if (grounded) {
			float pitch = player.getXRot();
			if (pitch < MIN_RECOIL_PITCH || pitch > MAX_RECOIL_PITCH) {
				return;
			}
		}

		Vec3 look = player.getLookAngle().normalize();
		Vec3 launch;
		if (grounded) {
			Vec3 backward = new Vec3(-look.x, 0.0D, -look.z);
			if (backward.lengthSqr() < 0.000001D) {
				backward = Vec3.ZERO;
			}
			launch = backward.scale(GROUND_RECOIL_HORIZONTAL).add(0.0D, GROUND_RECOIL_VERTICAL, 0.0D);
		} else {
			int airLiftLevel = getAirLiftLevel(player, gunStack);
			double verticalForce = AIR_RECOIL_VERTICAL_FORCE + (airLiftLevel * AIR_LIFT_VERTICAL_BONUS_PER_LEVEL);
			launch = new Vec3(
				-look.x * AIR_RECOIL_HORIZONTAL_FORCE,
				-look.y * verticalForce,
				-look.z * AIR_RECOIL_HORIZONTAL_FORCE
			);
		}

		player.setDeltaMovement(player.getDeltaMovement().add(launch));
		player.fallDistance = 0.0F;
		player.hasImpulse = true;
		player.hurtMarked = true;
	}

	private int getAirLiftLevel(Player player, ItemStack gunStack) {
		Registry<Enchantment> enchantmentRegistry = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> airLift = enchantmentRegistry.getHolderOrThrow(RwbyMod.AIR_LIFT_ENCHANTMENT);
		return EnchantmentHelper.getItemEnchantmentLevel(airLift, gunStack);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BLOCK;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72000;
	}
}
