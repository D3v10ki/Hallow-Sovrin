package com.hollowsovereign.mixin;

import com.hollowsovereign.data.PlayerData;
import com.hollowsovereign.data.PlayerDataAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements PlayerDataAccess {

    @Unique
    private final PlayerData hollowsovereign$data = new PlayerData();

    @Override
    public PlayerData hollowsovereign$getData() {
        return hollowsovereign$data;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void hollowsovereign$write(NbtCompound nbt, CallbackInfo ci) {
        NbtCompound tag = new NbtCompound();
        hollowsovereign$data.writeNbt(tag);
        nbt.put("HollowSovereign", tag);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void hollowsovereign$read(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("HollowSovereign")) {
            hollowsovereign$data.readNbt(nbt.getCompound("HollowSovereign"));
        }
    }
}
