package org.example.fuckoffgriefers.fuckoffgriefingcunts;

import net.fabricmc.api.ModInitializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    }
}
