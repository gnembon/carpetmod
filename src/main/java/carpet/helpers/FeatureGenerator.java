package carpet.helpers;

import carpet.CarpetSettings;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.*;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.feature.structure.*;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FeatureGenerator
{
    @FunctionalInterface
    private interface Thing
    {
        Boolean plop(World world, BlockPos pos);
    }
    private static Thing simplePlop(Feature<NoFeatureConfig> feature)
    {
        return (w, p) -> feature.func_212245_a(w, w.getChunkProvider().getChunkGenerator(), w.rand, p, IFeatureConfig.NO_FEATURE_CONFIG);
    }
    private static Thing simplePlop(Feature feature, IFeatureConfig config)
    {
        return (w, p) -> feature.func_212245_a(w, w.getChunkProvider().getChunkGenerator(), w.rand, p, config);
    }


    public static boolean spawn(String name, World world, BlockPos pos)
    {
        if (featureMap.containsKey(name))
            return featureMap.get(name).plop(world, pos);
        return false;
    }

    private static Map<String, Thing> featureMap = new HashMap<String, Thing>() {{
        put("chorus", simplePlop(Feature.CHORUS_PLANT));
        put("ice_spike", simplePlop(Feature.ICE_SPIKE));
        put("oak", simplePlop(Feature.BIG_TREE));
        put("tree", simplePlop(Feature.TREE));
        put("shrub", simplePlop(Feature.SHRUB));
        put("swamp_tree", simplePlop(Feature.SWAMP_TREE));
        put("glowstone", simplePlop(Feature.GLOWSTONE));
        put("iceberg", simplePlop(Feature.ICEBERG, new IcebergConfig(Blocks.PACKED_ICE.getDefaultState())));
        put("iceberg blue", simplePlop(Feature.ICEBERG, new IcebergConfig(Blocks.BLUE_ICE.getDefaultState())));
        put("boulder", simplePlop(Feature.BLOCK_BLOB, new BlockBlobConfig(Blocks.MOSSY_COBBLESTONE, 0)));

        put("monument",  Feature.OCEAN_MONUMENT::plopAnywhere);

        put("village", (w, p) -> new VillageStructure()
            {
                @Override
                protected ChunkPos getStartPositionForPosition(IChunkGenerator<?> c, Random r, int x, int z, int ox, int oz) {
                    return new ChunkPos(new BlockPos(x,0,z));
                }

                @Override
                protected boolean hasStartAt(IChunkGenerator<?> c, Random r, int x, int z) { return true; }
                @Override
                protected boolean isEnabledIn(IWorld worldIn) { return true; }

                @Override
                protected StructureStart makeStart(IWorld worldIn, IChunkGenerator<?> generator, SharedSeedRandom random, int x, int z)
                {
                    IChunkGenerator chunkgen = new ChunkGeneratorDebug(w, null, null)
                    {
                        @Nullable
                        @Override
                        public IFeatureConfig getStructureConfig(Biome biomeIn, Structure<? extends IFeatureConfig> structureIn)
                        {
                            return new VillageConfig(0, VillagePieces.Type.ACACIA);
                        }
                    };
                    return new VillageStructure.Start(worldIn, chunkgen, random, x, z, Biomes.DEFAULT);
                }

                public boolean force()
                {
                    ChunkPos cp = new ChunkPos(p);
                    SharedSeedRandom sred = new SharedSeedRandom(w.rand.nextInt());
                    StructureStart structurestart1 = makeStart(w, w.getChunkProvider().getChunkGenerator(), sred,cp.x, cp.z);
                    if (structurestart1 == NO_STRUCTURE)
                    {
                        CarpetSettings.LOG.error("Unable to make a structure");
                        return false;
                    }
                    CarpetSettings.skipGenerationChecks = true;
                    structurestart1.generateStructure(
                            w,
                            sred,
                            new MutableBoundingBox(p.getX()-512, p.getX()-512, p.getX()+512, p.getZ()+512),
                            new ChunkPos(p) );
                    CarpetSettings.skipGenerationChecks = false;
                    return true;
                }
            }.force()
        );

        put("fortress", (w, p) -> new FortressStructure()
                {
                    @Override
                    protected ChunkPos getStartPositionForPosition(IChunkGenerator<?> c, Random r, int x, int z, int ox, int oz) {
                        return new ChunkPos(new BlockPos(x,0,z));
                    }

                    @Override
                    protected boolean hasStartAt(IChunkGenerator<?> c, Random r, int x, int z) { return true; }
                    @Override
                    protected boolean isEnabledIn(IWorld worldIn) { return true; }


                    @Override
                    protected StructureStart makeStart(IWorld worldIn, IChunkGenerator<?> generator, SharedSeedRandom random, int x, int z)
                    {
                        return new FortressStructure.Start(worldIn, random, x, z, Biomes.NETHER);
                    }

                    public boolean force()
                    {
                        ChunkPos cp = new ChunkPos(p);
                        SharedSeedRandom sred = new SharedSeedRandom(w.rand.nextInt());
                        StructureStart structurestart1 = makeStart(w, w.getChunkProvider().getChunkGenerator(), sred,cp.x, cp.z);
                        if (structurestart1 == NO_STRUCTURE)
                        {
                            CarpetSettings.LOG.error("No structure");
                            return false;
                        }
                        CarpetSettings.LOG.error("generating structure");
                        CarpetSettings.skipGenerationChecks = true;
                        structurestart1.generateStructure(
                                w,
                                sred,
                                new MutableBoundingBox(p.getX()-512, p.getX()-512, p.getX()+512, p.getZ()+512),
                                new ChunkPos(p) );
                        CarpetSettings.skipGenerationChecks = false;
                        return true;
                    }
                }.force()
        );



    }};

}
