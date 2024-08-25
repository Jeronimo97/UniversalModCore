package cam72cam.mod.world;

import cam72cam.mod.serialization.TagCompound;

import java.io.File;

public interface LevelDataHandler {
    TagCompound saveLevelData(File levelDirectory);
    void loadLevelData(TagCompound data, File levelDirectory);
}
