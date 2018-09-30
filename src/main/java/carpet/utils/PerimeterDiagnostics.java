package carpet.utils;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;

public class PerimeterDiagnostics
{
    public static class Result
    {
        public int liquid;
        public int ground;
        public int specific;
        public List<BlockPos> samples;
        Result()
        {
            samples = new ArrayList<>();
        }
    }
    private Biome.SpawnListEntry sle;
    private WorldServer worldServer;
    private EnumCreatureType ctype;
    private EntityLiving el;
    private PerimeterDiagnostics(WorldServer server, EnumCreatureType ctype, EntityLiving el)
    {
        this.sle = null;
        this.worldServer = server;
        this.ctype = ctype;
        this.el = el;
    }

    public static Result countSpots(WorldServer worldserver, BlockPos epos, EntityLiving el)
    {
        BlockPos pos;
        //List<BlockPos> samples = new ArrayList<BlockPos>();
        //if (el != null) CarpetSettings.LOG.error(String.format("Got %s to check",el.toString()));
        int eY = epos.getY();
        int eX = epos.getX();
        int eZ = epos.getZ();
        Result result = new Result();

        //int ground_spawns = 0;
        //int liquid_spawns = 0;
        //int specific_spawns = 0;
        boolean add_water = false;
        boolean add_ground = false;
        EnumCreatureType ctype = null;

        if (el != null)
        {
            if (el instanceof EntityWaterMob)
            {
                add_water = true;
                ctype = EnumCreatureType.WATER_CREATURE;
            }
            else if (el instanceof EntityAnimal)
            {
                add_ground = true;
                ctype = EnumCreatureType.CREATURE;
            }
            else if (el instanceof IMob)
            {
                add_ground = true;
                ctype = EnumCreatureType.MONSTER;
            }
            else if (el instanceof EntityAmbientCreature)
            {
                ctype = EnumCreatureType.AMBIENT;
            }
        }
        PerimeterDiagnostics diagnostic = new PerimeterDiagnostics(worldserver,ctype,el);
        for (int x = -128; x <= 128; ++x)
        {
            for (int z = -128; z <= 128; ++z)
            {
                if (x*x + z*z > 128*128) // cut out a cyllinder first
                {
                    continue;
                }
                for (int y= 0; y < 256; ++y)
                {
                    if ((Math.abs(y-eY)>128) )
                    {
                        continue;
                    }
                    int distsq = (x)*(x)+(eY-y)*(eY-y)+(z)*(z);
                    if (distsq > 128*128 || distsq < 24*24)
                    {
                        continue;
                    }
                    pos = new BlockPos(eX+x, y, eZ+z);

                    IBlockState iblockstate = worldserver.getBlockState(pos);
                    IBlockState iblockstate_down = worldserver.getBlockState(pos.down());
                    IBlockState iblockstate_up = worldserver.getBlockState(pos.up());

                    if ( iblockstate.getMaterial() == Material.WATER && iblockstate_down.getMaterial() == Material.WATER && !iblockstate_up.isNormalCube())
                    {
                        result.liquid++;
                        if (add_water && diagnostic.check_entity_spawn(pos))
                        {
                            result.specific++;
                            if (result.samples.size() < 10)
                            {
                                result.samples.add(pos);
                            }
                        }
                    }
                    else
                    {
                        if (iblockstate_down.isOpaqueCube(worldserver, pos))
                        {
                            Block block = iblockstate_down.getBlock();
                            boolean flag = block != Blocks.BEDROCK && block != Blocks.BARRIER;
                            if( flag && WorldEntitySpawner.isValidEmptySpawnBlock(iblockstate, iblockstate.getFluidState()) && WorldEntitySpawner.isValidEmptySpawnBlock(iblockstate_up, iblockstate_up.getFluidState()))
                            {
                                result.ground ++;
                                if (add_ground && diagnostic.check_entity_spawn(pos))
                                {
                                    result.specific++;
                                    if (result.samples.size() < 10)
                                    {
                                        result.samples.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        //ashMap<String,Integer> result= new HashMap<>();
        //result.put("Potential in-water spawning spaces", liquid_spawns);
        //result.put("Potential on-ground spawning spaces", ground_spawns);
        //if (el != null) result.put(String.format("%s spawning spaces",el.getDisplayName().getUnformattedText()),specific_spawns);
        return result;
    }


    private boolean check_entity_spawn(BlockPos pos)
    {
        if (sle == null || !worldServer.canCreatureTypeSpawnHere(ctype, sle, pos))
        {
            sle = null;
            for (Biome.SpawnListEntry sle: worldServer.getChunkProvider().getPossibleCreatures(ctype, pos))
            {
                if (el.getClass() == sle.entityType.getEntityClass())
                {
                    this.sle = sle;
                    break;
                }
            }
            if (sle == null || !worldServer.canCreatureTypeSpawnHere(ctype, sle, pos))
            {
                return false;
            }
        }

        EntitySpawnPlacementRegistry.SpawnPlacementType spt = EntitySpawnPlacementRegistry.getPlacementType(sle.entityType);

        if (WorldEntitySpawner.canCreatureTypeSpawnAtLocation(spt, worldServer, pos, sle.entityType))
        {
            el.setLocationAndAngles((float)pos.getX() + 0.5F, (float)pos.getY(), (float)pos.getZ()+0.5F, 0.0F, 0.0F);
            return el.canSpawn(worldServer, false) && el.isNotColliding();
        }
        return false;
    }
}
