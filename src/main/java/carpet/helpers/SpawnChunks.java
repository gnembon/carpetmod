package carpet.helpers;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class SpawnChunks {
    public static List<ChunkPos> listIncludedChunks(World world)
    {
        BlockPos spawnPoint = world.getSpawnPoint();
        int spawnChunkX = spawnPoint.getX() / 16;
        int spawnChunkZ = spawnPoint.getZ() / 16;

        List<ChunkPos> spawnChunks = new ArrayList<>();

        for (int x = spawnChunkX - 9; x <= spawnChunkX + 9; x++)
        {
            for (int z = spawnChunkZ - 9; z <= spawnChunkZ + 9; z++)
            {
                if (world.isSpawnChunk(x, z))
                {
                    spawnChunks.add(new ChunkPos(x, z));
                }
            }
        }

        return spawnChunks;
    }
}
