package com.helium;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ShotBeamRenderer {
	private static final long BEAM_DURATION_NANOS = 1_200_000_000L;
	private static final float BEAM_BOLD_OFFSET = 0.035F;
	private static final List<ShotBeam> BEAMS = new ArrayList<>();

	private ShotBeamRenderer() {
	}

	public static void addBeam(int shooterEntityId, Vec3 start, Vec3 end) {
		long created = System.nanoTime();
		BEAMS.add(new ShotBeam(shooterEntityId, start, end, created, created + BEAM_DURATION_NANOS));
	}

	public static void render(WorldRenderContext context) {
		if (BEAMS.isEmpty()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		Vec3 cameraPos = context.camera().getPosition();
		PoseStack poseStack = context.matrixStack();
		if (poseStack == null) {
			return;
		}

		MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
		VertexConsumer vertices = buffers.getBuffer(RenderType.lines());
		Matrix4f pose = poseStack.last().pose();

		int cameraEntityId = minecraft.getCameraEntity() != null ? minecraft.getCameraEntity().getId() : -1;
		boolean firstPerson = minecraft.options.getCameraType() == CameraType.FIRST_PERSON;
		long now = System.nanoTime();

		Iterator<ShotBeam> iterator = BEAMS.iterator();
		while (iterator.hasNext()) {
			ShotBeam beam = iterator.next();
			if (beam.expiresAtNanos <= now) {
				iterator.remove();
				continue;
			}

			float age = (float) (now - beam.createdAtNanos);
			float life = (float) (beam.expiresAtNanos - beam.createdAtNanos);
			float lifeT = life > 0.0F ? Math.min(1.0F, age / life) : 1.0F;
			float retractT = smoothstep(lifeT);
			float fadeT = 1.0F - (lifeT * 0.35F);
			fadeT = Math.max(0.0F, fadeT);

			boolean selfFirstPerson = firstPerson && beam.shooterEntityId == cameraEntityId;
			Vec3 visibleStart = beam.start.lerp(beam.end, retractT);

			float x1 = (float) (visibleStart.x - cameraPos.x);
			float y1 = (float) (visibleStart.y - cameraPos.y);
			float z1 = (float) (visibleStart.z - cameraPos.z);
			float x2 = (float) (beam.end.x - cameraPos.x);
			float y2 = (float) (beam.end.y - cameraPos.y);
			float z2 = (float) (beam.end.z - cameraPos.z);
			float dx = x2 - x1;
			float dy = y2 - y1;
			float dz = z2 - z1;
			float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (length <= 0.00001F) {
				continue;
			}
			float nx = length > 0.00001F ? dx / length : 0.0F;
			float ny = length > 0.00001F ? dy / length : 1.0F;
			float nz = length > 0.00001F ? dz / length : 0.0F;

			int startAlpha = (int) ((selfFirstPerson ? 90 : 255) * fadeT);
			int endAlpha = (int) ((selfFirstPerson ? 65 : 210) * fadeT);
			renderBoldBeam(vertices, pose, x1, y1, z1, x2, y2, z2, nx, ny, nz, startAlpha, endAlpha);
		}

		buffers.endBatch(RenderType.lines());
	}

	private static void renderBoldBeam(
		VertexConsumer vertices,
		Matrix4f pose,
		float x1,
		float y1,
		float z1,
		float x2,
		float y2,
		float z2,
		float nx,
		float ny,
		float nz,
		int startAlpha,
		int endAlpha
	) {
		Vec3 forward = new Vec3(nx, ny, nz).normalize();
		Vec3 up = Math.abs(forward.y) > 0.95D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 right = forward.cross(up).normalize();
		Vec3 orthoUp = forward.cross(right).normalize();

		drawBeamLine(vertices, pose, x1, y1, z1, x2, y2, z2, nx, ny, nz, 255, 35, 35, startAlpha, endAlpha);
		drawOffsetBeam(vertices, pose, right, x1, y1, z1, x2, y2, z2, nx, ny, nz, startAlpha, endAlpha);
		drawOffsetBeam(vertices, pose, right.scale(-1.0D), x1, y1, z1, x2, y2, z2, nx, ny, nz, startAlpha, endAlpha);
		drawOffsetBeam(vertices, pose, orthoUp, x1, y1, z1, x2, y2, z2, nx, ny, nz, startAlpha, endAlpha);
		drawOffsetBeam(vertices, pose, orthoUp.scale(-1.0D), x1, y1, z1, x2, y2, z2, nx, ny, nz, startAlpha, endAlpha);
	}

	private static void drawOffsetBeam(
		VertexConsumer vertices,
		Matrix4f pose,
		Vec3 dir,
		float x1,
		float y1,
		float z1,
		float x2,
		float y2,
		float z2,
		float nx,
		float ny,
		float nz,
		int startAlpha,
		int endAlpha
	) {
		float ox = (float) (dir.x * BEAM_BOLD_OFFSET);
		float oy = (float) (dir.y * BEAM_BOLD_OFFSET);
		float oz = (float) (dir.z * BEAM_BOLD_OFFSET);
		drawBeamLine(vertices, pose, x1 + ox, y1 + oy, z1 + oz, x2 + ox, y2 + oy, z2 + oz, nx, ny, nz, 255, 55, 55, (int) (startAlpha * 0.85F), (int) (endAlpha * 0.85F));
	}

	private static void drawBeamLine(
		VertexConsumer vertices,
		Matrix4f pose,
		float x1,
		float y1,
		float z1,
		float x2,
		float y2,
		float z2,
		float nx,
		float ny,
		float nz,
		int r,
		int g,
		int b,
		int startAlpha,
		int endAlpha
	) {
		vertices.addVertex(pose, x1, y1, z1).setColor(r, g, b, startAlpha).setNormal(nx, ny, nz);
		vertices.addVertex(pose, x2, y2, z2).setColor(255, 210, 210, endAlpha).setNormal(nx, ny, nz);
	}

	private static float smoothstep(float t) {
		float clamped = Math.max(0.0F, Math.min(1.0F, t));
		return clamped * clamped * (3.0F - 2.0F * clamped);
	}

	private record ShotBeam(int shooterEntityId, Vec3 start, Vec3 end, long createdAtNanos, long expiresAtNanos) {
	}
}
