package org.example.fuckoffgriefers.fuckoffgriefingcunts;

import net.minecraft.block.entity.BlockEntity;

import java.util.HashMap;

public class jsonHashMap {
    public HashMap<BlockEntity, String> ownerMap;

    public jsonHashMap() {
        this.ownerMap = new HashMap<>();
    }

    public void setOwner(BlockEntity chestEntity, String ownerName) {
        this.ownerMap.put(chestEntity, ownerName);
    }

    public String getOwner(BlockEntity chestEntity) {
        return this.ownerMap.get(chestEntity);
    }

    public void deleteOwner(BlockEntity chestEntity) {
        this.ownerMap.remove(chestEntity);
    }
}
