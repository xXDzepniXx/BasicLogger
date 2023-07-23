package org.example.fuckoffgriefers.fuckoffgriefingcunts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class appendToFile {
    public String currentDir = System.getProperty("user.dir");
    public Path path = Paths.get(currentDir, "GriefingCuntsLog.log");
    public appendToFile(String textToAppend) {
        try {
            Files.writeString(path, textToAppend, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
