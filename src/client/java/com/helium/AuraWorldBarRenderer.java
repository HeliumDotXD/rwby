package com.helium;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class AuraWorldBarRenderer {
	private static final double RANGE = 40.0D;
	private static final float BAR_HALF_WIDTH = 0.55F;

	private AuraWorldBarRenderer() {
	}

	public static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || !mc.level.getGameRules().getBoolean(RwbyMod.VISIBLE_AURA)) {
			return;
		}

		PoseStack poseStack = context.matrixStack();
		if (poseStack == null) {
			return;
		}

		Vec3 cameraPos = context.camera().getPosition();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer vertices = buffers.getBuffer(RenderType.lines());
		Matrix4f pose = poseStack.last().pose();

		for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(RANGE), LivingEntity::isAlive)) {
			if (entity == mc.player) {
				continue;
			}

			AuraClientState.AuraData aura = AuraClientState.get(entity.getId());
			if (aura == null || aura.max() <= 0.0F) {
				continue;
			}

			float ratio = Math.max(0.0F, Math.min(1.0F, aura.current() / aura.max()));
			Vec3 anchor = entity.position().add(0.0D, entity.getBbHeight() + 0.35D, 0.0D);
			Vec3 toCamera = cameraPos.subtract(anchor).normalize();
			Vec3 right = toCamera.cross(new Vec3(0.0D, 1.0D, 0.0D));
			if (right.lengthSqr() < 1.0E-5) {
				continue;
			}
			right = right.normalize();

			Vec3 leftPoint = anchor.add(right.scale(BAR_HALF_WIDTH));
			Vec3 rightPoint = anchor.subtract(right.scale(BAR_HALF_WIDTH));
			Vec3 fillEnd = leftPoint.add(rightPoint.subtract(leftPoint).scale(ratio));

			Vector3f color = AuraColorResolver.resolve(entity);
				drawBar(vertices, pose, cameraPos, leftPoint, rightPoint, 0.08F, 0.08F, 0.12F, 0.85F);
				drawBar(vertices, pose, cameraPos, leftPoint, fillEnd, color.x, color.y, color.z, 0.95F);
		}

		buffers.endBatch(RenderType.lines());
	}

	private static void drawBar(
		VertexConsumer vertices,
		Matrix4f pose,
		Vec3 camera,
		Vec3 start,
		Vec3 end,
		float r,
		float g,
		float b,
		float a
	) {
		drawLine(vertices, pose, camera, start, end, r, g, b, a);
	}

	private static void drawLine(VertexConsumer vertices, Matrix4f pose, Vec3 camera, Vec3 start, Vec3 end, float r, float g, float b, float a) {
		float x1 = (float) (start.x - camera.x);
		float y1 = (float) (start.y - camera.y);
		float z1 = (float) (start.z - camera.z);
		float x2 = (float) (end.x - camera.x);
		float y2 = (float) (end.y - camera.y);
		float z2 = (float) (end.z - camera.z);
		float dx = x2 - x1;
		float dy = y2 - y1;
		float dz = z2 - z1;
		float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		float nx = len > 0.00001F ? dx / len : 0.0F;
		float ny = len > 0.00001F ? dy / len : 1.0F;
		float nz = len > 0.00001F ? dz / len : 0.0F;
		int alpha = (int) (a * 255.0F);
		vertices.addVertex(pose, x1, y1, z1).setColor((int) (r * 255), (int) (g * 255), (int) (b * 255), alpha).setNormal(nx, ny, nz);
		vertices.addVertex(pose, x2, y2, z2).setColor((int) (r * 255), (int) (g * 255), (int) (b * 255), alpha).setNormal(nx, ny, nz);
	}
}
