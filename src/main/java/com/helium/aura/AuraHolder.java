package com.helium.aura;

public interface AuraHolder {
	double rwby$getAuraCurrent();
	double rwby$getAuraMax();
	int rwby$getAuraLockTicks();
	void rwby$setAuraCurrent(double value);
	void rwby$setAuraMax(double value);
	void rwby$setAuraLockTicks(int ticks);
	void rwby$markAuraInitialized();
	boolean rwby$isAuraInitialized();
}
