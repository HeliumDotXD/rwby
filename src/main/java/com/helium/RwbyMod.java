package com.helium;

import com.helium.aura.AuraManager;
import com.helium.aura.AuraComponents;
import com.helium.aura.PlayerAuraComponent;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.core.Registry;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.GameRules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RwbyMod implements ModInitializer {
	public static final String MOD_ID = "rwby";
	public static final ResourceLocation SHOT_BEAM_PACKET_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "shot_beam");
	public static final ResourceLocation CRESCENT_BURST_PACKET_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "crescent_burst");
	public static final ResourceLocation CRESCENT_HOOK_PACKET_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "crescent_hook");
	public static final ResourceLocation AURA_SYNC_PACKET_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "aura_sync");
	public static final ResourceLocation AURA_SHATTER_PACKET_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "aura_shatter");
	public static final ResourceLocation AURA_COLOR_SET_PACKET_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "aura_color_set");
	public static final ResourceLocation AURA_OPEN_COLOR_SCREEN_PACKET_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "aura_open_color_screen");
	public static final int NO_AURA_COLOR = -1;
	private static final Map<String, Integer> AURA_COLOR_OPTIONS = createAuraColorOptions();
	public static final ResourceKey<DamageType> CRESCENT_ROSE_SHOT_DAMAGE_TYPE = ResourceKey.create(
		Registries.DAMAGE_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "crescent_rose_shot")
	);
	public static final ResourceKey<DamageType> CRESCENT_ROSE_V_DAMAGE_TYPE = ResourceKey.create(
		Registries.DAMAGE_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "crescent_rose_v")
	);
	public static final GameRules.Key<GameRules.BooleanValue> VISIBLE_AURA = GameRuleRegistry.register(
		"visibleaura",
		GameRules.Category.PLAYER,
		GameRuleFactory.createBooleanRule(true)
	);
	public static final ResourceKey<Enchantment> AIR_LIFT_ENCHANTMENT = ResourceKey.create(
		Registries.ENCHANTMENT,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "air_lift")
	);
	public static final Item CRESCENT_ROSE = registerItem(
		"crescent_rose",
		new RwbyGunItem(
			new Item.Properties()
				.stacksTo(1)
				.durability(1200)
				.attributes(
					ItemAttributeModifiers.builder()
						.add(
							Attributes.ATTACK_DAMAGE,
							new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 599.0D, AttributeModifier.Operation.ADD_VALUE),
							EquipmentSlotGroup.MAINHAND
						)
						.add(
							Attributes.ATTACK_SPEED,
							new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.0D, AttributeModifier.Operation.ADD_VALUE),
							EquipmentSlotGroup.MAINHAND
						)
						.build()
				)
		)
	);
	public static final Item COFFEE = registerItem(
		"coffee",
		new AuraCoffeeItem(
			new Item.Properties()
				.stacksTo(16)
				.food(new FoodProperties.Builder().nutrition(0).saturationModifier(0.0F).alwaysEdible().build()),
			100.0D,
			0.0D,
			0
		)
	);
	public static final Item COFFEE_CREAM_FIVE_SUGARS = registerItem(
		"coffee_cream_five_sugars",
		new AuraCoffeeItem(
			new Item.Properties()
				.stacksTo(16)
				.food(new FoodProperties.Builder().nutrition(0).saturationModifier(0.0F).alwaysEdible().build()),
			200.0D,
			10.0D / 20.0D,
			20 * 60
		)
	);
	public static final Item SCROLL = registerItem(
		"scroll",
		new AuraScrollItem(new Item.Properties().stacksTo(1))
	);
	public static final SoundEvent CRESCENT_ROSE_SHOT = registerSoundEvent("crescent_rose_shot");

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(ShotBeamPayload.TYPE, ShotBeamPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(AuraSyncPayload.TYPE, AuraSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(AuraShatterPayload.TYPE, AuraShatterPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(AuraOpenColorScreenPayload.TYPE, AuraOpenColorScreenPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(AuraColorSetPayload.TYPE, AuraColorSetPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CrescentBurstPayload.TYPE, CrescentBurstPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CrescentHookPayload.TYPE, CrescentHookPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(AuraColorSetPayload.TYPE, (payload, context) -> {
			String colorId = payload.colorId();
			if (!AURA_COLOR_OPTIONS.containsKey(colorId)) {
				return;
			}
			PlayerAuraComponent aura = AuraComponents.getPlayerAura(context.player());
			if (aura != null) {
				aura.rwby$setAuraColorId(colorId);
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(CrescentBurstPayload.TYPE, (payload, context) -> {
			RwbyAbilityHandler.requestBurst(context.player());
		});
			ServerPlayNetworking.registerGlobalReceiver(CrescentHookPayload.TYPE, (payload, context) -> {
				RwbyAbilityHandler.requestHook(context.player());
			});
			ServerTickEvents.END_SERVER_TICK.register(RwbyAbilityHandler::onServerTick);
			ServerTickEvents.END_SERVER_TICK.register(AuraManager::onServerTick);
			ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> entries.accept(CRESCENT_ROSE));
				ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
					entries.accept(COFFEE);
					entries.accept(COFFEE_CREAM_FIVE_SUGARS);
				});
				ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> entries.accept(SCROLL));

		LOGGER.info("RWBY mod initialized with weapon item registration.");
	}

	private static Item registerItem(String name, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item);
	}

	private static SoundEvent registerSoundEvent(String name) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static Map<String, Integer> getAuraColorOptions() {
		return AURA_COLOR_OPTIONS;
	}

	public static int resolveAuraColorRgb(String colorId) {
		if (colorId == null) {
			return NO_AURA_COLOR;
		}
		return AURA_COLOR_OPTIONS.getOrDefault(colorId, NO_AURA_COLOR);
	}

	private static Map<String, Integer> createAuraColorOptions() {
		Map<String, Integer> colors = new LinkedHashMap<>();
		colors.put("red", 0xFF4040);
		colors.put("orange", 0xFF8B3D);
		colors.put("yellow", 0xFFD53D);
		colors.put("lime", 0x97FF45);
		colors.put("green", 0x32D16B);
		colors.put("teal", 0x1ED2C8);
		colors.put("cyan", 0x3BDFFF);
		colors.put("blue", 0x73C7FF);
		colors.put("indigo", 0x7280FF);
		colors.put("violet", 0xB57BFF);
		colors.put("magenta", 0xF067FF);
		colors.put("pink", 0xFF82C4);
		colors.put("white", 0xF2F7FF);
		colors.put("silver", 0xB8C1CF);
		colors.put("gold", 0xFFD369);
		colors.put("black", 0x2C2F3A);
		return Collections.unmodifiableMap(colors);
	}
}
