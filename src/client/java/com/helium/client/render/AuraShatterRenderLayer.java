package com.helium.client.render;

import com.helium.AuraShatterEffectManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class AuraShatterRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
	public AuraShatterRenderLayer(RenderLayerParent<T, M> renderer) {
		super(renderer);
	}

	@Override
	public void render(
		PoseStack poseStack,
		MultiBufferSource buffer,
		int packedLight,
		T entity,
		float limbSwing,
		float limbSwingAmount,
		float partialTick,
		float ageInTicks,
		float netHeadYaw,
		float headPitch
	) {
		float flash = AuraShatterEffectManager.getFlashStrength(entity, partialTick);
		if (flash <= 0.0F) {
			return;
		}

		M model = getParentModel();
		ResourceLocation texture = getTextureLocation(entity);
		int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);

		VertexConsumer emissiveConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture));
		model.renderToBuffer(
			poseStack,
			emissiveConsumer,
			packedLight,
			overlay,
			AuraShatterEffectManager.getEmissiveColor(entity, flash)
		);

		VertexConsumer glintConsumer = buffer.getBuffer(RenderType.entityGlintDirect());
		model.renderToBuffer(
			poseStack,
			glintConsumer,
			packedLight,
			overlay,
			AuraShatterEffectManager.getGlintColor(flash)
		);
	}
}
