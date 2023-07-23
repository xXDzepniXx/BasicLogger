package org.example.fuckoffgriefers.fuckoffgriefingcunts;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FuckOffGriefingCunts implements ModInitializer { // To make sure mod is run by Fabric
    @Override
    public void onInitialize() {
        String currentDir = System.getProperty("user.dir");
        Path path = Paths.get(currentDir, "GriefingCuntsLog.log");
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            Item playerItemInHand = player.getStackInHand(hand).getItem();
            if (!player.isSpectator() && // does not check if player is spectator
                    !player.getMainHandStack().isEmpty() &&
                    playerItemInHand == Blocks.TNT.asItem() ||
                    playerItemInHand == Items.FLINT_AND_STEEL.asItem() ||
                    playerItemInHand == Items.FIRE_CHARGE.asItem()) {
                LocalDateTime currentTime = LocalDateTime.now();
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String formattedTime = currentTime.format(timeFormatter);
                String stringToAppend = player.getEntityName() + " placed "+ playerItemInHand.toString().toUpperCase() + " at " + hitResult.getBlockPos() + " " + formattedTime + "\n";
                appendToFile call = new appendToFile(stringToAppend);
                // ^^^ does this upset you
                System.out.println(stringToAppend);
            }
            return ActionResult.PASS;
        });
    }
}
