package com.helium.mixin;

import com.helium.RwbyMod;
import com.helium.aura.AuraComponents;
import com.helium.aura.AuraHolder;
import com.helium.aura.AuraManager;
import com.helium.aura.PlayerAuraComponent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAuraMixin implements AuraHolder {
	@Unique
	private static final double RWBY_AURA_REGEN_PER_TICK = 5.0D / 1200.0D;
	@Unique
	private static final int RWBY_AURA_SHATTER_LOCK_TICKS = 10 * 60 * 20;
	@Unique
	private double rwby$auraCurrent;
	@Unique
	private double rwby$auraMax;
	@Unique
	private int rwby$auraLockTicks;
	@Unique
	private boolean rwby$auraInitialized;
	@Unique
	private boolean rwby$processingAuraDamage;
	@Unique
	private int rwby$cancelKnockbackTicks;
	@Unique
	private static final float RWBY_CRESCENT_HP_DAMAGE_AFTER_AURA = 10.0F;

	@Inject(method = "tick", at = @At("TAIL"))
	private void rwby$tickAura(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.level().isClientSide) {
			return;
		}

			double baseMaxAura = Math.max(1.0D, self.getAttributeBaseValue(Attributes.MAX_HEALTH) * 50.0D);
			if (!rwby$isInitialized(self)) {
				rwby$setMax(self, baseMaxAura);
				rwby$setCurrent(self, baseMaxAura);
				rwby$setInitialized(self, true);
			} else if (Math.abs(rwby$getMax(self) - baseMaxAura) > 0.0001D) {
				double ratio = rwby$getMax(self) > 0.0D ? rwby$getCurrent(self) / rwby$getMax(self) : 1.0D;
				rwby$setMax(self, baseMaxAura);
				rwby$setCurrent(self, Math.max(0.0D, Math.min(rwby$getMax(self), rwby$getMax(self) * ratio)));
			}

			if (rwby$getLockTicks(self) > 0) {
				rwby$setLockTicks(self, rwby$getLockTicks(self) - 1);
			} else if (rwby$getCurrent(self) < rwby$getMax(self)) {
				rwby$setCurrent(self, Math.min(rwby$getMax(self), rwby$getCurrent(self) + RWBY_AURA_REGEN_PER_TICK));
			}

		if (rwby$cancelKnockbackTicks > 0) {
			rwby$cancelKnockbackTicks--;
		}
	}

	@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
	private void rwby$applyAuraShield(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.level().isClientSide || rwby$processingAuraDamage || amount <= 0.0F) {
			return;
		}

		boolean isRwbyShot = source.is(RwbyMod.CRESCENT_ROSE_SHOT_DAMAGE_TYPE);
		boolean isRwbyVAttack = source.is(RwbyMod.CRESCENT_ROSE_V_DAMAGE_TYPE);
		boolean isCrescentMelee = rwby$isCrescentMelee(source) && !isRwbyShot && !isRwbyVAttack;
		boolean isCrescentAttack = isRwbyShot || isRwbyVAttack || isCrescentMelee;
			boolean hasAura = rwby$isInitialized(self) && rwby$getCurrent(self) > 0.0D;

		if (isCrescentAttack && !hasAura && amount != RWBY_CRESCENT_HP_DAMAGE_AFTER_AURA) {
			rwby$processingAuraDamage = true;
			boolean result = self.hurt(source, RWBY_CRESCENT_HP_DAMAGE_AFTER_AURA);
			rwby$processingAuraDamage = false;
			cir.setReturnValue(result);
			return;
		}
		if (!hasAura) {
			return;
		}

		double auraDamage = amount / 2.0D;
		if (isRwbyShot) {
			auraDamage = amount * 0.5D;
		} else if (isRwbyVAttack || isCrescentMelee) {
			auraDamage = amount;
		}
			if (rwby$getCurrent(self) >= auraDamage) {
				rwby$setCurrent(self, rwby$getCurrent(self) - auraDamage);
				rwby$cancelKnockbackTicks = 2;
				rwby$registerAuraAggro(self, source);
				rwby$playAuraHitFlash(self, source);
				cir.setReturnValue(true);
				return;
			}

			double previousAura = rwby$getCurrent(self);
			rwby$setCurrent(self, 0.0D);
			if (previousAura > 0.0D) {
				rwby$setLockTicks(self, RWBY_AURA_SHATTER_LOCK_TICKS);
				rwby$cancelKnockbackTicks = 2;
				rwby$registerAuraAggro(self, source);
				rwby$playAuraHitFlash(self, source);
				AuraManager.onAuraShatter(self);
			}
		// Aura shatters first; remaining damage does not overflow into health on this hit.
		cir.setReturnValue(true);
	}

	@Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
	private void rwby$cancelAuraKnockback(double strength, double x, double z, CallbackInfo ci) {
		if (rwby$cancelKnockbackTicks > 0) {
			ci.cancel();
		}
	}

	@Unique
	private void rwby$playAuraHitFlash(LivingEntity self, DamageSource source) {
		Entity attacker = source.getEntity();
		if (attacker != null) {
			double dx = attacker.getX() - self.getX();
			double dz = attacker.getZ() - self.getZ();
			self.indicateDamage(dx, dz);
		} else {
			self.animateHurt(0.0F);
		}
	}

	@Unique
	private void rwby$registerAuraAggro(LivingEntity self, DamageSource source) {
		Entity attackerEntity = source.getEntity();
		if (!(attackerEntity instanceof LivingEntity attacker)) {
			return;
		}

		self.setLastHurtByMob(attacker);
		if (attacker instanceof Player playerAttacker) {
			self.setLastHurtByPlayer(playerAttacker);
		}

		if (self instanceof Mob mob) {
			mob.setTarget(attacker);
		}

		if (self instanceof NeutralMob neutral) {
			neutral.setLastHurtByMob(attacker);
			if (attacker instanceof Player playerAttacker) {
				neutral.setLastHurtByPlayer(playerAttacker);
			}
			neutral.setPersistentAngerTarget(attacker.getUUID());
			neutral.startPersistentAngerTimer();
		}
	}

	@Unique
	private boolean rwby$isCrescentMelee(DamageSource source) {
		Entity attacker = source.getEntity();
		if (!(attacker instanceof Player player)) {
			return false;
		}
		return player.getMainHandItem().is(RwbyMod.CRESCENT_ROSE) || player.getOffhandItem().is(RwbyMod.CRESCENT_ROSE);
	}

	@Override
	public double rwby$getAuraCurrent() {
		LivingEntity self = (LivingEntity) (Object) this;
		return rwby$getCurrent(self);
	}

	@Override
	public double rwby$getAuraMax() {
		LivingEntity self = (LivingEntity) (Object) this;
		return rwby$getMax(self);
	}

	@Override
	public int rwby$getAuraLockTicks() {
		LivingEntity self = (LivingEntity) (Object) this;
		return rwby$getLockTicks(self);
	}

	@Override
	public void rwby$setAuraCurrent(double value) {
		LivingEntity self = (LivingEntity) (Object) this;
		rwby$setCurrent(self, value);
	}

	@Override
	public void rwby$setAuraMax(double value) {
		LivingEntity self = (LivingEntity) (Object) this;
		rwby$setMax(self, value);
	}

	@Override
	public void rwby$setAuraLockTicks(int ticks) {
		LivingEntity self = (LivingEntity) (Object) this;
		rwby$setLockTicks(self, ticks);
	}

	@Override
	public void rwby$markAuraInitialized() {
		LivingEntity self = (LivingEntity) (Object) this;
		rwby$setInitialized(self, true);
	}

	@Override
	public boolean rwby$isAuraInitialized() {
		LivingEntity self = (LivingEntity) (Object) this;
		return rwby$isInitialized(self);
	}

	@Unique
	private PlayerAuraComponent rwby$getPlayerAuraComponent(LivingEntity self) {
		if (self instanceof Player player) {
			return AuraComponents.getPlayerAura(player);
		}
		return null;
	}

	@Unique
	private double rwby$getCurrent(LivingEntity self) {
		PlayerAuraComponent component = rwby$getPlayerAuraComponent(self);
		return component != null ? component.getAuraCurrent() : rwby$auraCurrent;
	}

	@Unique
	private void rwby$setCurrent(LivingEntity self, double value) {
		PlayerAuraComponent component = rwby$getPlayerAuraComponent(self);
		if (component != null) {
			component.setAuraCurrent(value);
		} else {
			rwby$auraCurrent = value;
		}
	}

	@Unique
	private double rwby$getMax(LivingEntity self) {
		PlayerAuraComponent component = rwby$getPlayerAuraComponent(self);
		return component != null ? component.getAuraMax() : rwby$auraMax;
	}

	@Unique
	private void rwby$setMax(LivingEntity self, double value) {
		PlayerAuraComponent component = rwby$getPlayerAuraComponent(self);
		if (component != null) {
			component.setAuraMax(value);
		} else {
			rwby$auraMax = value;
		}
	}

	@Unique
	private int rwby$getLockTicks(LivingEntity self) {
		PlayerAuraComponent component = rwby$getPlayerAuraComponent(self);
		return component != null ? component.getAuraLockTicks() : rwby$auraLockTicks;
	}

	@Unique
	private void rwby$setLockTicks(LivingEntity self, int ticks) {
		PlayerAuraComponent component = rwby$getPlayerAuraComponent(self);
		if (component != null) {
			component.setAuraLockTicks(ticks);
		} else {
			rwby$auraLockTicks = ticks;
		}
	}

	@Unique
	private boolean rwby$isInitialized(LivingEntity self) {
		PlayerAuraComponent component = rwby$getPlayerAuraComponent(self);
		return component != null ? component.isAuraInitialized() : rwby$auraInitialized;
	}

	@Unique
	private void rwby$setInitialized(LivingEntity self, boolean initialized) {
		PlayerAuraComponent component = rwby$getPlayerAuraComponent(self);
		if (component != null) {
			component.setAuraInitialized(initialized);
		} else {
			rwby$auraInitialized = initialized;
		}
	}
}
