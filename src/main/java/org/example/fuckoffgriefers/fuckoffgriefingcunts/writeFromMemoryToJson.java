package org.example.fuckoffgriefers.fuckoffgriefingcunts;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.block.entity.BlockEntity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

public class writeFromMemoryToJson {
    public writeFromMemoryToJson(jsonHashMap jsonHashMap, Path ownerMapPath) {
        Gson gson = new Gson();
        JsonObject json = new JsonObject();
        StringBuilder jsonString = new StringBuilder();

        Iterator<Map.Entry<BlockEntity, String>> iterator = jsonHashMap.ownerMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockEntity, String> entry = iterator.next();
            if (entry.getKey() == null || entry.getValue() == null) {
                iterator.remove(); // in case of a reference that is null, taking up memory
            } else {
                json.addProperty("x", entry.getKey().getPos().getX());
                json.addProperty("y", entry.getKey().getPos().getY());
                json.addProperty("z", entry.getKey().getPos().getZ());
                json.addProperty("World", entry.getKey().getWorld().getRegistryKey().getValue().toString());
                json.addProperty("Owner", entry.getValue());

                String stringToAppend = gson.toJson(json) + "\n";
                jsonString.append(stringToAppend);
            }
        }

        OutputStream os = null;
        try {
            os = new FileOutputStream(String.valueOf(ownerMapPath));
            os.write(jsonString.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
