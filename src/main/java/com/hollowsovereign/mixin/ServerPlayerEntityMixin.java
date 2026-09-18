package com.hollowsovereign.mixin;

import com.hollowsovereign.data.PlayerData;
import com.hollowsovereign.data.PlayerDataAccess;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Carries progression across respawns (the new player copies from the old one). */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void hollowsovereign$copyData(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        PlayerData from = ((PlayerDataAccess) oldPlayer).hollowsovereign$getData();
        PlayerData to = ((PlayerDataAccess) (Object) this).hollowsovereign$getData();
        to.copyFrom(from);
    }
}
