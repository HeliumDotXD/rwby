package com.helium;

import com.helium.aura.AuraManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class AuraCoffeeItem extends Item {
	private final double instantAuraRestore;
	private final double regenPerTick;
	private final int regenTicks;

	public AuraCoffeeItem(Properties properties, double instantAuraRestore, double regenPerTick, int regenTicks) {
		super(properties);
		this.instantAuraRestore = instantAuraRestore;
		this.regenPerTick = regenPerTick;
		this.regenTicks = regenTicks;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
		ItemStack result = super.finishUsingItem(stack, level, livingEntity);
		if (level instanceof ServerLevel && livingEntity.isAlive()) {
			AuraManager.restoreAura(livingEntity, instantAuraRestore);
			if (regenTicks > 0 && regenPerTick > 0.0D) {
				AuraManager.addAuraRegenBoost(livingEntity, regenPerTick, regenTicks);
			}
		}
		return result;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.DRINK;
	}

	@Override
	public SoundEvent getDrinkingSound() {
		return SoundEvents.GENERIC_DRINK;
	}

	@Override
	public SoundEvent getEatingSound() {
		return SoundEvents.GENERIC_DRINK;
	}
}
