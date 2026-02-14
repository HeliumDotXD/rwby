package com.helium;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

public class AuraScrollItem extends Item {
	public AuraScrollItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player instanceof ServerPlayer serverPlayer) {
			ServerPlayNetworking.send(serverPlayer, AuraOpenColorScreenPayload.INSTANCE);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
		super.inventoryTick(stack, level, entity, slotId, selected);
		boolean inHand = selected;
		if (!inHand && entity instanceof Player player) {
			inHand = player.getOffhandItem() == stack;
		}

		CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
		int current = modelData != null ? modelData.value() : 0;
		int desired = inHand ? 1 : 0;
		if (current == desired) {
			return;
		}
		if (desired == 0) {
			stack.remove(DataComponents.CUSTOM_MODEL_DATA);
		} else {
			stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(desired));
		}
	}
}
