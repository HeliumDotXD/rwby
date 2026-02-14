package com.helium.mixin.client;

import com.helium.AuraShatterEffectManager;
import com.helium.client.render.AuraShatterRenderLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Shadow
	protected abstract boolean addLayer(net.minecraft.client.renderer.entity.layers.RenderLayer layer);

	@Inject(
		method = "<init>(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;Lnet/minecraft/client/model/EntityModel;F)V",
		at = @At("TAIL")
	)
	private void rwby$addAuraShatterLayer(EntityRendererProvider.Context context, EntityModel<?> model, float shadowRadius, CallbackInfo ci) {
		@SuppressWarnings("unchecked")
		LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> renderer =
			(LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>>) (Object) this;
		addLayer(new AuraShatterRenderLayer(renderer));
	}

	@Inject(
		method = "getWhiteOverlayProgress(Lnet/minecraft/world/entity/LivingEntity;F)F",
		at = @At("RETURN"),
		cancellable = true
	)
	private void rwby$boostWhiteOverlay(LivingEntity entity, float partialTick, CallbackInfoReturnable<Float> cir) {
		float shatterFlash = AuraShatterEffectManager.getFlashStrength(entity, partialTick);
		if (shatterFlash <= 0.0F) {
			return;
		}
		cir.setReturnValue(Math.max(cir.getReturnValue(), Math.min(1.0F, shatterFlash * 1.25F)));
	}
}
