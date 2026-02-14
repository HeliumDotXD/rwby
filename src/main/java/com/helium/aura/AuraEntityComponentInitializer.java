package com.helium.aura;

import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public final class AuraEntityComponentInitializer implements EntityComponentInitializer {
	@Override
	public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
		registry.registerForPlayers(AuraComponents.PLAYER_AURA, player -> new PlayerAuraComponent(), RespawnCopyStrategy.ALWAYS_COPY);
	}
}
