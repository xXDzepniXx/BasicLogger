package org.example.fuckoffgriefers.fuckoffgriefingcunts.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.nbt.NbtCompound;
import org.example.fuckoffgriefers.fuckoffgriefingcunts.FuckOffGriefingCunts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.SERVER)
@Mixin(ChestBlockEntity.class)
public abstract class chestOnReadOnWriteEdit {
    @Inject(method = "readNbt", at = @At("RETURN"))
    private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("Owner")) {
            FuckOffGriefingCunts.ownerMap.put((BlockEntity) (Object) this, nbt.getString("Owner"));
        }
    }

    @Inject(method = "writeNbt", at = @At("RETURN"))
    private void onWriteNbt(NbtCompound nbt, CallbackInfo ci) {
        String owner = FuckOffGriefingCunts.ownerMap.get((BlockEntity) (Object) this);
        if (owner != null) {
            nbt.putString("Owner", owner);
        }
    }
}