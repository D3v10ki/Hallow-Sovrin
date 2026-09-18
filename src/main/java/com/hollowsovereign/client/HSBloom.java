package com.hollowsovereign.client;

import com.hollowsovereign.HollowSovereign;
import ladysnake.satin.api.event.ShaderEffectRenderCallback;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import ladysnake.satin.api.managed.ShaderEffectManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

/**
 * Bloom post-processing via the Satin API. Loaded only when Satin is present
 * (see {@code HollowSovereignClient}). Any shader failure permanently disables
 * bloom for the session rather than affecting gameplay.
 */
@Environment(EnvType.CLIENT)
public final class HSBloom {
    private HSBloom() {}

    private static ManagedShaderEffect bloom;
    private static boolean broken = false;

    public static void init() {
        try {
            bloom = ShaderEffectManager.getInstance()
                    .manage(new Identifier(HollowSovereign.MOD_ID, "shaders/post/bloom.json"));
            ShaderEffectRenderCallback.EVENT.register(HSBloom::onRenderShaderEffects);
            HollowSovereign.LOGGER.info("Bloom shader registered (Satin present).");
        } catch (Throwable t) {
            broken = true;
            HollowSovereign.LOGGER.warn("Bloom could not be set up: {}", t.toString());
        }
    }

    private static void onRenderShaderEffects(float tickDelta) {
        if (broken || bloom == null) return;
        if (!HSClientConfig.get().bloom) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.getCameraEntity() == null) return;
        try {
            if (bloom.isErrored()) {
                broken = true;
                HollowSovereign.LOGGER.warn("Bloom shader failed to compile — disabled for this session.");
                return;
            }
            bloom.render(tickDelta);
        } catch (Throwable t) {
            broken = true;
            HollowSovereign.LOGGER.warn("Bloom disabled after a render error: {}", t.toString());
        }
    }
}
