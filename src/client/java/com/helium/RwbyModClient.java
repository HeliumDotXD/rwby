package com.helium;

import com.helium.client.gui.AuraColorSelectScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class RwbyModClient implements ClientModInitializer {
	private static final KeyMapping CRESCENT_BURST_KEY = new KeyMapping(
		"key.rwby.crescent_burst",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_B,
		"key.categories.rwby"
	);
	private static final KeyMapping CRESCENT_HOOK_KEY = new KeyMapping(
		"key.rwby.crescent_hook",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_V,
		"key.categories.rwby"
	);
	private int ticksSinceJoin;
	private boolean colorScreenOpenedThisJoin;

	@Override
	public void onInitializeClient() {
		KeyBindingHelper.registerKeyBinding(CRESCENT_BURST_KEY);
		KeyBindingHelper.registerKeyBinding(CRESCENT_HOOK_KEY);

		ClientPlayNetworking.registerGlobalReceiver(ShotBeamPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> ShotBeamRenderer.addBeam(payload.shooterEntityId(), payload.start(), payload.end()));
		});
			ClientPlayNetworking.registerGlobalReceiver(AuraSyncPayload.TYPE, (payload, context) -> {
				context.client().execute(() -> AuraClientState.update(
					payload.entityId(),
					payload.current(),
					payload.max(),
					payload.lockTicks(),
					payload.auraColorRgb()
				));
			});
			ClientPlayNetworking.registerGlobalReceiver(AuraShatterPayload.TYPE, (payload, context) -> {
				context.client().execute(() -> AuraShatterEffectManager.start(payload.entityId()));
			});
			ClientPlayNetworking.registerGlobalReceiver(AuraOpenColorScreenPayload.TYPE, (payload, context) -> {
				context.client().execute(() -> context.client().setScreen(new AuraColorSelectScreen()));
			});

			ClientTickEvents.END_CLIENT_TICK.register(client -> {
				if (client.player == null) {
					ticksSinceJoin = 0;
					colorScreenOpenedThisJoin = false;
					return;
				}
				ticksSinceJoin++;

				while (CRESCENT_BURST_KEY.consumeClick()) {
					ClientPlayNetworking.send(CrescentBurstPayload.INSTANCE);
				}
				while (CRESCENT_HOOK_KEY.consumeClick()) {
					ClientPlayNetworking.send(CrescentHookPayload.INSTANCE);
				}

				if (!colorScreenOpenedThisJoin
					&& ticksSinceJoin > 40
					&& client.screen == null
					&& AuraClientState.shouldOpenColorPicker()) {
					client.setScreen(new AuraColorSelectScreen());
					colorScreenOpenedThisJoin = true;
				}

				AuraClientState.tick();
				AuraShatterEffectManager.tick();
			});

		HudRenderCallback.EVENT.register(new AuraHudOverlay());
		WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> ShotBeamRenderer.render(context));
		WorldRenderEvents.AFTER_TRANSLUCENT.register(AuraWorldBarRenderer::render);
	}
}
