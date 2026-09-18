package com.hollowsovereign.mixin.client;

import com.hollowsovereign.client.ClientState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the caster is inside a Starless Expanse domain, pull the fog in and dye it near-black so the
 * dome walls dissolve into an endless void — the "infinite space" look. This only overrides fog
 * uniforms (distance + colour); it never touches the framebuffer, so unlike a composite post-shader
 * it cannot black-screen the game.
 */
@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {

    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void hollowsovereign$voidFog(Camera camera, BackgroundRenderer.FogType fogType,
                                                float viewDistance, boolean thickFog, float tickDelta,
                                                CallbackInfo ci) {
        if (!ClientState.inDomain()) return;
        // Wide enough to keep the (now bigger) domain + its centrepiece visible, close enough that
        // the far walls still dissolve into black — the endless-void read.
        RenderSystem.setShaderFogStart(8.0f);
        RenderSystem.setShaderFogEnd(52.0f);
        RenderSystem.setShaderFogColor(0.01f, 0.0f, 0.02f);
    }
}
