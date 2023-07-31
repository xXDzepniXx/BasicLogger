package org.example.fuckoffgriefers.fuckoffgriefingcunts.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.example.fuckoffgriefers.fuckoffgriefingcunts.FuckOffGriefingCunts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Environment(EnvType.SERVER)
@Mixin(ChestBlock.class)
public abstract class chestOnPlacedOwnerAdd { // this is for when a player places a chest
    @Inject(method = "onPlaced", at = @At("HEAD")) // before any nbt read or write is called
    private void onChestPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack, CallbackInfo ci) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof ChestBlockEntity) { // shouldn't be buuuut just in case
                FuckOffGriefingCunts.setOwner(blockEntity, placer.getEntityName());
                blockEntity.markDirty();
            }
        }
    }
}