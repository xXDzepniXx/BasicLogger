package org.example.fuckoffgriefers.fuckoffgriefingcunts;

import com.google.gson.*;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class FuckOffGriefingCunts implements ModInitializer { // To make sure mod is run by Fabric
    // ownerMap is stuck in memory as long as the server is on, but will lose all data upon a restart
    // BlockEntity is unique with each entry
    public static final HashMap<BlockEntity, String> ownerMap = new HashMap<>();
    public static final String currentDir = System.getProperty("user.dir");
    public static final Path path = Paths.get(currentDir, "GriefingCuntsLogs");
    public static final Path ownerMapPath = Paths.get(currentDir, "GriefingCuntsLogs", "GriefingCuntsOwnerMaps.json");
    @Override
    public void onInitialize() {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!Files.exists(ownerMapPath)) {
            try {
                Files.createFile(ownerMapPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ArrayList<String> playerNames = new ArrayList<>(10);

            Gson gson = new Gson();
            try (Reader reader = new FileReader("usercache.json")) {
                JsonElement jsonElement = gson.fromJson(reader, JsonElement.class);
                if (jsonElement.isJsonArray()) {
                    jsonElement.getAsJsonArray().forEach(element -> {
                        JsonObject playerObject = element.getAsJsonObject();
                        playerNames.add(playerObject.get("name").getAsString());
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            dispatcher.register(CommandManager.literal("setChestOwner")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("players", StringArgumentType.string())
                            .suggests((context, builder) -> CommandSource.suggestMatching(playerNames, builder))
                            .executes(this::setChestOwnerForce)));
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Map.Entry<BlockEntity, String> entry : ownerMap.entrySet()) {
                Gson gson = new Gson();
                JsonObject json = new JsonObject();

                json.addProperty("x", entry.getKey().getPos().getX());
                json.addProperty("y", entry.getKey().getPos().getY());
                json.addProperty("z", entry.getKey().getPos().getZ());
                json.addProperty("World", entry.getKey().getWorld().getRegistryKey().getValue().toString());
                json.addProperty("Owner", entry.getValue());

                String jsonString = gson.toJson(json) + "\n";
                try {
                    Files.writeString(ownerMapPath, jsonString, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Gson gson = new Gson();

            try (BufferedReader reader = new BufferedReader(new FileReader(String.valueOf(ownerMapPath)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Map<String, Object> tempOwnerMap = gson.fromJson(line, Map.class);
                    double x = 0;
                    double y = 0;
                    double z = 0;
                    RegistryKey<World> worldKey = null;
                    String owner = null;

                    for (Map.Entry<String, Object> entry : tempOwnerMap.entrySet()) {
                        if (entry.getKey().equals("x")) {
                            x = (double) entry.getValue();
                        } else if (entry.getKey().equals("y")) {
                            y = (double) entry.getValue();
                        } else if (entry.getKey().equals("z")) {
                            z = (double) entry.getValue();
                        } else if (entry.getKey().equals("Owner")) {
                            owner = (String) entry.getValue();
                        } else if (entry.getKey().equals("World")) {
                            worldKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier((String) entry.getValue()));
                        }
                    }

                    BlockPos pos = new BlockPos((int)x, (int)y, (int)z);
                    Chunk chunk = server.getWorld(worldKey).getChunk(pos);
                    BlockEntity chestBlock = chunk.getBlockEntity(pos);
                    setOwner(chestBlock, owner);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                new FileOutputStream(String.valueOf(ownerMapPath)).close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            String chestOwner = getOwner(blockEntity);
            if (blockEntity instanceof ChestBlockEntity) {
                if (chestOwner.equals(player.getEntityName())) { // implies not null
                    deleteOwner(blockEntity);
                } else if (!chestOwner.equals(player.getEntityName())) { // since this implies it's not null
                    LocalDateTime currentTime = LocalDateTime.now();
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    String formattedTime = currentTime.format(timeFormatter);
                    String textToAppend = player.getEntityName() + " BROKE " + chestOwner + "'s CHEST at " +
                            blockEntity.getPos() + " " + formattedTime + "\n";
                    new playerFileNameOrganizer(player.getEntityName(), textToAppend);
                }
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockState blockState = world.getBlockState(hitResult.getBlockPos());

            if (blockState.getBlock() instanceof ChestBlock) { // make sure it is a chest
                BlockEntity blockEntity = world.getBlockEntity(hitResult.getBlockPos());
                if (blockEntity instanceof ChestBlockEntity) { // because it could be a ChestMinecartEntity
                    //ChestBlockEntity chest = (ChestBlockEntity) blockEntity;
                    //ChestType chestType = blockEntity.getCachedState().get(ChestBlock.CHEST_TYPE);
                    //NbtCompound chestNbtData = blockEntity.createNbt();
                    //String chestOwner = chestNbtData.getString("Owner");
                    String chestOwner = getOwner(blockEntity);
                    if (!chestOwner.equals(player.getEntityName())) {
                        LocalDateTime currentTime = LocalDateTime.now();
                        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        String formattedTime = currentTime.format(timeFormatter);
                        String textToAppend = player.getEntityName() + " INTERACTED with " + chestOwner + "'s CHEST at " +
                                blockEntity.getPos() + " " + formattedTime + "\n";
                        new playerFileNameOrganizer(player.getEntityName(), textToAppend);
                    }

                    /* TESTING STUFF
                    if (chestOwner != null) {
                        if (!chestOwner.equals(player.getEntityName())) {
                            System.out.println("ROBBER");
                        } else {
                            System.out.println("Owner");
                        }
                    }

                    if (chestType == ChestType.SINGLE) {
                        for (int slot = 0; slot < 27; slot++) {
                            ItemStack slotItem = chest.getStack(slot);
                            System.out.println(slotItem);
                        }
                    } else if (chestType == ChestType.LEFT || chestType == ChestType.RIGHT) {
                        BlockPos otherHalf = blockEntity.getPos().offset(ChestBlock.getFacing(chest.getCachedState()));
                        BlockEntity otherHalfEntity = world.getBlockEntity(otherHalf);
                        for (int slot = 0; slot < 27; slot++) {
                            ItemStack slotItem = chest.getStack(slot);
                            System.out.println(slotItem);
                        }
                        if (otherHalfEntity instanceof ChestBlockEntity otherHalfChest) { // apparently you should check...
                            for (int slot = 0; slot < 27; slot++) {
                                ItemStack slotItem = otherHalfChest.getStack(slot);
                                System.out.println(slotItem);
                            }
                        }
                    }
                    */
                }
            }

            return ActionResult.PASS;
        });

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
                String stringToAppend = player.getEntityName() + " placed " + playerItemInHand.toString().toUpperCase() + " at " + hitResult.getBlockPos() + " " + formattedTime + "\n";
                playerFileNameOrganizer playerOrganizer = new playerFileNameOrganizer(player.getEntityName(), stringToAppend);
                // ^^^ does this upset you
                System.out.println(stringToAppend);
            }
            return ActionResult.PASS;
        });
    }

    public int setChestOwnerForce(CommandContext<ServerCommandSource> context) {
        String commandInput = StringArgumentType.getString(context, "players");

        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player != null) { // in case someone runs it from console for some reason
            double maxDistance = 5.0D;
            Vec3d vec3d = player.getCameraPosVec(1.0F);
            Vec3d vec3d2 = player.getRotationVec(1.0F);
            Vec3d vec3d3 = vec3d.add(vec3d2.x * maxDistance, vec3d2.y * maxDistance, vec3d2.z * maxDistance);
            HitResult hitResult = player.getWorld().raycast(new RaycastContext(vec3d, vec3d3, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
            // vec3d2, vec3d3 and hitResult stuff i found online

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
                BlockState blockState = player.getWorld().getBlockState(blockPos);
                Block block = blockState.getBlock();
                BlockEntity blockEntity = player.getWorld().getBlockEntity(blockPos);

                if (block instanceof ChestBlock && blockEntity instanceof ChestBlockEntity) { // now we know for sure it's a chest
                    setOwner(blockEntity, commandInput);
                }
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    public static void setOwner(BlockEntity chestEntity, String ownerName) {
        ownerMap.put(chestEntity, ownerName);
    }

    public static String getOwner(BlockEntity chestEntity) {
        return ownerMap.get(chestEntity);
    }

    public static void deleteOwner(BlockEntity chestEntity) {
        ownerMap.remove(chestEntity);
    }
}
