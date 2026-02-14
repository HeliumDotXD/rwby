package com.helium;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;

public final class AuraHudOverlay implements HudRenderCallback {
	private static final int BAR_WIDTH = 81;
	private static final int BAR_HEIGHT = 6;

	@Override
	public void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || !mc.level.getGameRules().getBoolean(RwbyMod.VISIBLE_AURA)) {
			return;
		}

		AuraClientState.AuraData selfAura = AuraClientState.self();
		if (selfAura == null) {
			return;
		}

		float maxAura = selfAura.max();
		if (maxAura <= 0.0F) {
			return;
		}
		float currentAura = Math.min(maxAura, selfAura.current());
		float ratio = currentAura / maxAura;

		int screenWidth = mc.getWindow().getGuiScaledWidth();
		int screenHeight = mc.getWindow().getGuiScaledHeight();
		int x = (screenWidth / 2) - 91;
		int y = screenHeight - 45;

		Vector3f color = AuraColorResolver.resolve(mc.player);
		int fillColor = ((int) (255 * 0.85F) << 24)
			| (((int) (color.x * 255.0F) & 0xFF) << 16)
			| (((int) (color.y * 255.0F) & 0xFF) << 8)
			| ((int) (color.z * 255.0F) & 0xFF);
		int bgColor = 0xAA1A1A2A;
		int borderColor = 0xDD9CCBFF;

		guiGraphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, borderColor);
		guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, bgColor);
		guiGraphics.fill(x, y, x + Math.max(1, (int) (BAR_WIDTH * ratio)), y + BAR_HEIGHT, fillColor);

		String text = (int) currentAura + "/" + (int) maxAura;
		if (selfAura.lockTicks() > 0) {
			text += " (Shattered)";
		}
		guiGraphics.drawString(mc.font, Component.literal(text), x + 2, y - 9, 0xE7F6FF, true);
	}
}
