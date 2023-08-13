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
import net.minecraft.block.*;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

import com.google.gson.JsonObject;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class FuckOffGriefingCunts implements ModInitializer { // To make sure mod is run by Fabric
    // ownerMap is stuck in memory as long as the server is on, but will lose all data upon a restart
    // BlockEntity is unique with each entry
    public static final jsonHashMap jsonOwnerMap = new jsonHashMap();
    public static final ArrayList<String> playerNames = new ArrayList<>(10);
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
            dispatcher.register(CommandManager.literal("setChestOwner")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("players", StringArgumentType.string())
                            .suggests((context, builder) -> CommandSource.suggestMatching(playerNames, builder))
                            .executes(this::setChestOwnerForce)));
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> new writeFromMemoryToJson(jsonOwnerMap, ownerMapPath));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
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
                        switch (entry.getKey()) {
                            case "x" -> x = (double) entry.getValue();
                            case "y" -> y = (double) entry.getValue();
                            case "z" -> z = (double) entry.getValue();
                            case "Owner" -> owner = (String) entry.getValue();
                            case "World" -> worldKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier((String) entry.getValue()));
                        }
                    }

                    BlockPos pos = new BlockPos((int)x, (int)y, (int)z);
                    Chunk chunk = server.getWorld(worldKey).getChunk(pos); // this loads the chunk
                    BlockEntity chestBlock = chunk.getBlockEntity(pos);
                    jsonOwnerMap.setOwner(chestBlock, owner);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity) {
                if (jsonOwnerMap.ownerMap.containsKey(blockEntity)) {
                    String chestOwner = jsonOwnerMap.getOwner(blockEntity);
                    if (chestOwner.equals(player.getEntityName())) { // implies not null
                        jsonOwnerMap.deleteOwner(blockEntity);
                        new writeFromMemoryToJson(jsonOwnerMap, ownerMapPath);
                    } else if (!chestOwner.equals(player.getEntityName())) { // since this implies it's not null
                        LocalDateTime currentTime = LocalDateTime.now();
                        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        String formattedTime = currentTime.format(timeFormatter);
                        Identifier id = Registries.BLOCK_ENTITY_TYPE.getId(blockEntity.getType());
                        String idString = id.toString().toUpperCase();
                        String textToAppend = player.getEntityName() + " BROKE " + chestOwner + "'s " +
                                idString + " at " +
                                blockEntity.getPos() + " " + formattedTime + "\n";
                        new playerFileNameOrganizer(player.getEntityName(), textToAppend);
                        jsonOwnerMap.deleteOwner(blockEntity);
                        new writeFromMemoryToJson(jsonOwnerMap, ownerMapPath);
                    }
                }
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockState blockState = world.getBlockState(hitResult.getBlockPos());
            Item playerItemInHand = player.getStackInHand(hand).getItem();

            if (blockState.getBlock() instanceof ChestBlock || blockState.getBlock() instanceof BarrelBlock) { // make sure it is a chest or barrel
                BlockEntity blockEntity = world.getBlockEntity(hitResult.getBlockPos());
                if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity) { // because it could be a ChestMinecartEntity
                    if (jsonOwnerMap.ownerMap.containsKey(blockEntity)) {
                        String chestOwner = jsonOwnerMap.getOwner(blockEntity);
                        if (!chestOwner.equals(player.getEntityName())) {
                            LocalDateTime currentTime = LocalDateTime.now();
                            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            String formattedTime = currentTime.format(timeFormatter);
                            String textToAppend = player.getEntityName() + " INTERACTED with " + chestOwner + "'s "
                                    + blockState.getBlock().asItem().toString().toUpperCase() + " at " +
                                    blockEntity.getPos() + " " + formattedTime + "\n";
                            new playerFileNameOrganizer(player.getEntityName(), textToAppend);
                        }
                    }
                }
            } else if (!player.isSpectator() && // does not check if player is spectator
                    !player.getMainHandStack().isEmpty() &&
                    playerItemInHand == Blocks.HOPPER.asItem()) {

                BlockPos hopperPos = hitResult.getBlockPos();
                BlockPos chestPos = hopperPos.up().up();
                BlockEntity blockEntityAbove = world.getBlockEntity(chestPos);
                BlockState blockStateAbove = world.getBlockState(chestPos);

                if (blockEntityAbove instanceof ChestBlockEntity || blockEntityAbove instanceof BarrelBlockEntity) {
                    if (jsonOwnerMap.ownerMap.containsKey(blockEntityAbove)) {
                        if (!jsonOwnerMap.ownerMap.get(blockEntityAbove).equals(player.getEntityName())) {
                            LocalDateTime currentTime = LocalDateTime.now();
                            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            String formattedTime = currentTime.format(timeFormatter);
                            String blockType = blockStateAbove.getBlock().asItem().toString().toUpperCase();
                            String stringToAppend = player.getEntityName() + " placed " +
                                    playerItemInHand.toString().toUpperCase() + " BELOW " +
                                    jsonOwnerMap.ownerMap.get(blockEntityAbove) + "'s " + blockType + " at " + blockEntityAbove.getPos() + " " + formattedTime + "\n";
                            new playerFileNameOrganizer(player.getEntityName(), stringToAppend);
                        }
                    }
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

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos currentHalf = ((BlockHitResult)hitResult).getBlockPos();
                BlockState halfBlockState = player.getWorld().getBlockState(currentHalf);
                Block block = halfBlockState.getBlock();
                BlockEntity blockEntity = player.getWorld().getBlockEntity(currentHalf);

                if (block instanceof ChestBlock && blockEntity instanceof ChestBlockEntity) { // now we know for sure it's a chest
                    ChestType chestType = halfBlockState.get(ChestBlock.CHEST_TYPE);
                    Direction chestFacing = halfBlockState.get(ChestBlock.FACING);
                    Direction directionOfOtherHalf;
                    jsonOwnerMap.setOwner(blockEntity, commandInput);

                    if (chestType == ChestType.LEFT) {
                        directionOfOtherHalf = chestFacing.rotateYClockwise();
                        BlockPos otherHalf = currentHalf.offset(directionOfOtherHalf);
                        if (player.getWorld().getBlockEntity(otherHalf) instanceof ChestBlockEntity) {
                            jsonOwnerMap.setOwner(player.getWorld().getBlockEntity(otherHalf), commandInput);
                        }
                    } else if (chestType == ChestType.RIGHT) {
                        directionOfOtherHalf = chestFacing.rotateYCounterclockwise();
                        BlockPos otherHalf = currentHalf.offset(directionOfOtherHalf);
                        if (player.getWorld().getBlockEntity(otherHalf) instanceof ChestBlockEntity) {
                            jsonOwnerMap.setOwner(player.getWorld().getBlockEntity(otherHalf), commandInput);
                        }
                    }
                } else if (block instanceof BarrelBlock && blockEntity instanceof BarrelBlockEntity) {
                    jsonOwnerMap.setOwner(blockEntity, commandInput);
                }
            }
        }

        new writeFromMemoryToJson(jsonOwnerMap, ownerMapPath);
        System.out.println("Successfully changed chest owner to " + commandInput);

        return Command.SINGLE_SUCCESS;
    }
}
