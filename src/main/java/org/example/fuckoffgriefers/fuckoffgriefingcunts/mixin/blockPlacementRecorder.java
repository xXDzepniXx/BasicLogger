/*
package org.example.fuckoffgriefers.fuckoffgriefingcunts.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Environment(EnvType.SERVER)
@Mixin(World.class)
public abstract class blockPlacementRecorder {
    @Shadow @Nullable public abstract MinecraftServer getServer();

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void onBlockPlaced(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<Boolean> cir) {
        if (!Objects.requireNonNull(getServer()).isSingleplayer()) {
            Block block = state.getBlock();
            LocalDateTime currentTime = LocalDateTime.now();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTime = currentTime.format(timeFormatter);
            if (block == Blocks.TNT) {

            }
        }
    }
}
 */

// IN THE EVENT WE NEED A setBlockState MIXIN