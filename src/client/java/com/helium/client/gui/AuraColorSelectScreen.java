package com.helium.client.gui;

import com.helium.AuraClientState;
import com.helium.AuraColorSetPayload;
import com.helium.RwbyMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuraColorSelectScreen extends Screen {
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int COLUMNS = 4;
	private static final Component SUBTITLE = Component.literal("Pick one. You can change it later in code/commands.");

	public AuraColorSelectScreen() {
		super(Component.literal("Choose Your Aura Color"));
	}

	@Override
	protected void init() {
		super.init();
		List<Map.Entry<String, Integer>> colors = new ArrayList<>(RwbyMod.getAuraColorOptions().entrySet());
		int rows = (int) Math.ceil(colors.size() / (double) COLUMNS);
		int panelWidth = COLUMNS * (BUTTON_WIDTH + 6) + 28;
		int panelHeight = rows * (BUTTON_HEIGHT + 6) + 72;
		int panelX = (width - panelWidth) / 2;
		int panelY = (height - panelHeight) / 2;
		int startX = panelX + 14;
		int startY = panelY + 40;

		for (int i = 0; i < colors.size(); i++) {
			Map.Entry<String, Integer> entry = colors.get(i);
			int col = i % COLUMNS;
			int row = i / COLUMNS;
			int x = startX + col * (BUTTON_WIDTH + 6);
			int y = startY + row * (BUTTON_HEIGHT + 6);
			String colorId = entry.getKey();
			int rgb = entry.getValue();
			addRenderableWidget(Button.builder(
				Component.literal(formatName(colorId)),
				button -> {
					ClientPlayNetworking.send(new AuraColorSetPayload(colorId));
					AuraClientState.setLocalPlayerColor(rgb);
					onClose();
				}
			).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		int panelWidth = COLUMNS * (BUTTON_WIDTH + 6) + 28;
		int rows = (int) Math.ceil(RwbyMod.getAuraColorOptions().size() / (double) COLUMNS);
		int panelHeight = rows * (BUTTON_HEIGHT + 6) + 72;
		int panelX = (width - panelWidth) / 2;
		int panelY = (height - panelHeight) / 2;

		// Slightly more opaque than the active aura visuals, tinted with default aura blue.
		guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xCC73C7FF);
		guiGraphics.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + panelHeight - 2, 0xAA132033);
		guiGraphics.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + 34, 0xD0101A27);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		drawOutlinedCentered(guiGraphics, title, width / 2, panelY + 12, 0xF6FCFF, 0xFF091119);
		drawOutlinedCentered(guiGraphics, SUBTITLE, width / 2, panelY + 24, 0xDDEEFF, 0xFF091119);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static String formatName(String id) {
		StringBuilder out = new StringBuilder(id.length());
		boolean upper = true;
		for (int i = 0; i < id.length(); i++) {
			char c = id.charAt(i);
			if (c == '_' || c == '-') {
				out.append(' ');
				upper = true;
				continue;
			}
			out.append(upper ? Character.toUpperCase(c) : c);
			upper = false;
		}
		return out.toString();
	}

	private void drawOutlinedCentered(GuiGraphics guiGraphics, Component text, int x, int y, int color, int outline) {
		guiGraphics.drawCenteredString(font, text, x - 1, y, outline);
		guiGraphics.drawCenteredString(font, text, x + 1, y, outline);
		guiGraphics.drawCenteredString(font, text, x, y - 1, outline);
		guiGraphics.drawCenteredString(font, text, x, y + 1, outline);
		guiGraphics.drawCenteredString(font, text, x, y, color);
	}
}
