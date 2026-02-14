package com.helium.aura;

import com.helium.RwbyMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;

public final class AuraComponents {
	public static final ComponentKey<PlayerAuraComponent> PLAYER_AURA = ComponentRegistry.getOrCreate(
		ResourceLocation.fromNamespaceAndPath(RwbyMod.MOD_ID, "player_aura"),
		PlayerAuraComponent.class
	);

	private AuraComponents() {
	}

	public static PlayerAuraComponent getPlayerAura(Player player) {
		return PLAYER_AURA.getNullable(player);
	}
}
