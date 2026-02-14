package com.helium.aura;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.ladysnake.cca.api.v3.entity.RespawnableComponent;

public final class PlayerAuraComponent implements RespawnableComponent<PlayerAuraComponent>, AuraColorHolder {
	private static final String NBT_AURA_CURRENT = "auraCurrent";
	private static final String NBT_AURA_MAX = "auraMax";
	private static final String NBT_AURA_LOCK = "auraLockTicks";
	private static final String NBT_AURA_INITIALIZED = "auraInitialized";
	private static final String NBT_AURA_COLOR = "auraColor";

	private double auraCurrent;
	private double auraMax;
	private int auraLockTicks;
	private boolean auraInitialized;
	private String auraColorId;

	@Override
	public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		auraCurrent = Math.max(0.0D, tag.getDouble(NBT_AURA_CURRENT));
		auraMax = Math.max(1.0D, tag.getDouble(NBT_AURA_MAX));
		auraLockTicks = Math.max(0, tag.getInt(NBT_AURA_LOCK));
		auraInitialized = tag.getBoolean(NBT_AURA_INITIALIZED);
		if (tag.contains(NBT_AURA_COLOR)) {
			auraColorId = tag.getString(NBT_AURA_COLOR);
		} else {
			auraColorId = null;
		}
	}

	@Override
	public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		tag.putDouble(NBT_AURA_CURRENT, auraCurrent);
		tag.putDouble(NBT_AURA_MAX, auraMax);
		tag.putInt(NBT_AURA_LOCK, auraLockTicks);
		tag.putBoolean(NBT_AURA_INITIALIZED, auraInitialized);
		if (auraColorId != null && !auraColorId.isBlank()) {
			tag.putString(NBT_AURA_COLOR, auraColorId);
		}
	}

	@Override
	public void copyForRespawn(PlayerAuraComponent original, HolderLookup.Provider registryLookup, boolean lossless, boolean keepInventory, boolean sameCharacter) {
		this.auraCurrent = original.auraCurrent;
		this.auraMax = original.auraMax;
		this.auraLockTicks = original.auraLockTicks;
		this.auraInitialized = original.auraInitialized;
		this.auraColorId = original.auraColorId;
	}

	public double getAuraCurrent() {
		return auraCurrent;
	}

	public void setAuraCurrent(double auraCurrent) {
		this.auraCurrent = auraCurrent;
	}

	public double getAuraMax() {
		return auraMax;
	}

	public void setAuraMax(double auraMax) {
		this.auraMax = auraMax;
	}

	public int getAuraLockTicks() {
		return auraLockTicks;
	}

	public void setAuraLockTicks(int auraLockTicks) {
		this.auraLockTicks = auraLockTicks;
	}

	public boolean isAuraInitialized() {
		return auraInitialized;
	}

	public void setAuraInitialized(boolean auraInitialized) {
		this.auraInitialized = auraInitialized;
	}

	@Override
	public String rwby$getAuraColorId() {
		return auraColorId;
	}

	@Override
	public void rwby$setAuraColorId(String colorId) {
		this.auraColorId = colorId;
	}
}
