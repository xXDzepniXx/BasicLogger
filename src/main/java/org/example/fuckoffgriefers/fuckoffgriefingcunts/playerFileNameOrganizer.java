package org.example.fuckoffgriefers.fuckoffgriefingcunts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class playerFileNameOrganizer {
    public final String currentDir = System.getProperty("user.dir");
    public final String mainFolderName = "GriefingCuntsLogs";
    public playerFileNameOrganizer(String playerName, String textToAppend) {
        Path folderPath = Paths.get(currentDir, mainFolderName, playerName);
        Path filePath = Paths.get(currentDir, mainFolderName, playerName, playerName + ".log");
        if (Files.exists(folderPath)) {
            try {
                Files.writeString(filePath, textToAppend, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                Files.createDirectories(folderPath);
                Files.createFile(filePath);
                Files.writeString(filePath, textToAppend, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
