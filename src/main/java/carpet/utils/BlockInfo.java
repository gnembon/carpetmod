package carpet.utils;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.pathfinding.PathType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class BlockInfo
{
    public static String getSoundName(SoundType stype)
    {
        if (stype == SoundType.WOOD   ) { return "WOOD"  ;   }
        if (stype == SoundType.GROUND ) { return "GRAVEL";   }
        if (stype == SoundType.PLANT  ) { return "GRASS" ;   }
        if (stype == SoundType.STONE  ) { return "STONE" ;   }
        if (stype == SoundType.METAL  ) { return "METAL" ;   }
        if (stype == SoundType.GLASS  ) { return "GLASS" ;   }
        if (stype == SoundType.CLOTH  ) { return "WOOL"  ;   }
        if (stype == SoundType.SAND   ) { return "SAND"  ;   }
        if (stype == SoundType.SNOW   ) { return "SNOW"  ;   }
        if (stype == SoundType.LADDER ) { return "LADDER";   }
        if (stype == SoundType.ANVIL  ) { return "ANVIL" ;   }
        if (stype == SoundType.SLIME  ) { return "SLIME" ;   }
        return "Something new";
    }

    private static String getMapColourName(MaterialColor colour)
    {
        if (colour == MaterialColor.AIR        ) { return "AIR"        ; }
        if (colour == MaterialColor.GRASS      ) { return "GRASS"      ; }
        if (colour == MaterialColor.SAND       ) { return "SAND"       ; }
        if (colour == MaterialColor.WOOL       ) { return "WOOL"       ; }
        if (colour == MaterialColor.TNT        ) { return "TNT"        ; }
        if (colour == MaterialColor.ICE        ) { return "ICE"        ; }
        if (colour == MaterialColor.IRON       ) { return "IRON"       ; }
        if (colour == MaterialColor.FOLIAGE    ) { return "FOLIAGE"    ; }
        if (colour == MaterialColor.SNOW       ) { return "SNOW"       ; }
        if (colour == MaterialColor.CLAY       ) { return "CLAY"       ; }
        if (colour == MaterialColor.DIRT       ) { return "DIRT"       ; }
        if (colour == MaterialColor.STONE      ) { return "STONE"      ; }
        if (colour == MaterialColor.WATER      ) { return "WATER"      ; }
        if (colour == MaterialColor.WOOD       ) { return "WOOD"       ; }
        if (colour == MaterialColor.QUARTZ     ) { return "QUARTZ"     ; }
        if (colour == MaterialColor.ADOBE      ) { return "ADOBE"      ; }
        if (colour == MaterialColor.MAGENTA    ) { return "MAGENTA"    ; }
        if (colour == MaterialColor.LIGHT_BLUE ) { return "LIGHT_BLUE" ; }
        if (colour == MaterialColor.YELLOW     ) { return "YELLOW"     ; }
        if (colour == MaterialColor.LIME       ) { return "LIME"       ; }
        if (colour == MaterialColor.PINK       ) { return "PINK"       ; }
        if (colour == MaterialColor.GRAY       ) { return "GRAY"       ; }
        if (colour == MaterialColor.LIGHT_GRAY ) { return "LIGHT_GRAY" ; }
        if (colour == MaterialColor.CYAN       ) { return "CYAN"       ; }
        if (colour == MaterialColor.PURPLE     ) { return "PURPLE"     ; }
        if (colour == MaterialColor.BLUE       ) { return "BLUE"       ; }
        if (colour == MaterialColor.BROWN      ) { return "BROWN"      ; }
        if (colour == MaterialColor.GREEN      ) { return "GREEN"      ; }
        if (colour == MaterialColor.RED        ) { return "RED"        ; }
        if (colour == MaterialColor.BLACK      ) { return "BLACK"      ; }
        if (colour == MaterialColor.GOLD       ) { return "GOLD"       ; }
        if (colour == MaterialColor.DIAMOND    ) { return "DIAMOND"    ; }
        if (colour == MaterialColor.LAPIS      ) { return "LAPIS"      ; }
        if (colour == MaterialColor.EMERALD    ) { return "EMERALD"    ; }
        if (colour == MaterialColor.OBSIDIAN   ) { return "OBSIDIAN"   ; }
        if (colour == MaterialColor.NETHERRACK ) { return "NETHERRACK" ; }
        return "Something new";
    }

    private static String getMaterialName(Material material)
    {
        if (material == Material.AIR             ) { return "AIR"            ; }
        if (material == Material.GRASS           ) { return "GRASS"          ; }
        if (material == Material.GROUND          ) { return "DIRT"           ; }
        if (material == Material.WOOD            ) { return "WOOD"           ; }
        if (material == Material.ROCK            ) { return "STONE"          ; }
        if (material == Material.IRON            ) { return "IRON"           ; }
        if (material == Material.ANVIL           ) { return "ANVIL"          ; }
        if (material == Material.WATER           ) { return "WATER"          ; }
        if (material == Material.LAVA            ) { return "LAVA"           ; }
        if (material == Material.LEAVES          ) { return "LEAVES"         ; }
        if (material == Material.PLANTS          ) { return "PLANTS"         ; }
        if (material == Material.VINE            ) { return "VINE"           ; }
        if (material == Material.SPONGE          ) { return "SPONGE"         ; }
        if (material == Material.CLOTH           ) { return "WOOL"           ; }
        if (material == Material.FIRE            ) { return "FIRE"           ; }
        if (material == Material.SAND            ) { return "SAND"           ; }
        if (material == Material.CIRCUITS        ) { return "REDSTONE_COMPONENT"; }
        if (material == Material.CARPET          ) { return "CARPET"         ; }
        if (material == Material.GLASS           ) { return "GLASS"          ; }
        if (material == Material.REDSTONE_LIGHT  ) { return "REDSTONE_LAMP"  ; }
        if (material == Material.TNT             ) { return "TNT"            ; }
        if (material == Material.CORAL           ) { return "CORAL"          ; }
        if (material == Material.ICE             ) { return "ICE"            ; }
        if (material == Material.PACKED_ICE      ) { return "PACKED_ICE"     ; }
        if (material == Material.SNOW            ) { return "SNOW_LAYER"     ; }
        if (material == Material.CRAFTED_SNOW    ) { return "SNOW"           ; }
        if (material == Material.CACTUS          ) { return "CACTUS"         ; }
        if (material == Material.CLAY            ) { return "CLAY"           ; }
        if (material == Material.GOURD           ) { return "GOURD"          ; }
        if (material == Material.DRAGON_EGG      ) { return "DRAGON_EGG"     ; }
        if (material == Material.PORTAL          ) { return "PORTAL"         ; }
        if (material == Material.CAKE            ) { return "CAKE"           ; }
        if (material == Material.WEB             ) { return "COBWEB"         ; }
        if (material == Material.PISTON          ) { return "PISTON"         ; }
        if (material == Material.BARRIER         ) { return "BARRIER"        ; }
        if (material == Material.STRUCTURE_VOID  ) { return "STRUCTURE"      ; }
        return "Something new";
    }

    public static List<ITextComponent> blockInfo(BlockPos pos, World world)
    {
        IBlockState state = world.getBlockState(pos);
        Material material = state.getMaterial();
        Block block = state.getBlock();
        String metastring = "";
        for (net.minecraft.state.IProperty<?> iproperty : state.getProperties())
        {
            metastring += ", "+iproperty.getName() + '='+state.get(iproperty);
        }
        List<ITextComponent> lst = new ArrayList<>();
        lst.add(Messenger.s(null, ""));
        lst.add(Messenger.s(null, "====================================="));
        lst.add(Messenger.s(null, String.format("Block info for %s%s (id %d%s):",IRegistry.field_212618_g.getKey(block),metastring, IRegistry.field_212618_g.getId(block), metastring )));
        lst.add(Messenger.s(null, String.format(" - Material: %s", getMaterialName(material))));
        lst.add(Messenger.s(null, String.format(" - Map colour: %s", getMapColourName(state.getMapColor(world, pos)))));
        lst.add(Messenger.s(null, String.format(" - Sound type: %s", getSoundName(block.getSoundType()))));
        lst.add(Messenger.s(null, ""));
        lst.add(Messenger.s(null, String.format(" - Full block: %s", state.isFullCube() )));
        lst.add(Messenger.s(null, String.format(" - Normal cube: %s", state.isNormalCube())));
        lst.add(Messenger.s(null, String.format(" - Is liquid: %s", material.isLiquid())));
        lst.add(Messenger.s(null, String.format(" - Is solid: %s", material.isSolid())));
        lst.add(Messenger.s(null, ""));
        lst.add(Messenger.s(null, String.format(" - Light in: %d, above: %d", world.getLight(pos), world.getLight(pos.up()))));
        lst.add(Messenger.s(null, String.format(" - Brightness in: %.2f, above: %.2f", world.getBrightness(pos), world.getBrightness(pos.up()))));
        lst.add(Messenger.s(null, String.format(" - Is opaque: %s", material.isOpaque() )));
        lst.add(Messenger.s(null, String.format(" - Light opacity: %d", state.getOpacity(world,pos))));
        lst.add(Messenger.s(null, String.format(" - Blocks light: %s", state.propagatesSkylightDown(world, pos))));
        lst.add(Messenger.s(null, String.format(" - Emitted light: %d", state.getLightValue())));
        lst.add(Messenger.s(null, String.format(" - Picks neighbour light value: %s", state.useNeighborBrightness(world, pos))));
        lst.add(Messenger.s(null, ""));
        lst.add(Messenger.s(null, String.format(" - Causes suffocation: %s", state.causesSuffocation())));
        lst.add(Messenger.s(null, String.format(" - Blocks movement on land: %s", !state.allowsMovement(world,pos, PathType.LAND))));
        lst.add(Messenger.s(null, String.format(" - Blocks movement in air: %s", !state.allowsMovement(world,pos, PathType.AIR))));
        lst.add(Messenger.s(null, String.format(" - Blocks movement in liquids: %s", !state.allowsMovement(world,pos, PathType.WATER))));
        lst.add(Messenger.s(null, String.format(" - Can burn: %s", material.isFlammable())));
        lst.add(Messenger.s(null, String.format(" - Requires a tool: %s", !material.isToolNotRequired())));
        lst.add(Messenger.s(null, String.format(" - Hardness: %.2f", state.getBlockHardness(world, pos))));
        lst.add(Messenger.s(null, String.format(" - Blast resistance: %.2f", block.getExplosionResistance())));
        lst.add(Messenger.s(null, String.format(" - Ticks randomly: %s", block.getTickRandomly(state))));
        lst.add(Messenger.s(null, ""));
        lst.add(Messenger.s(null, String.format(" - Can provide power: %s", state.canProvidePower())));
        lst.add(Messenger.s(null, String.format(" - Strong power level: %d", world.getStrongPower(pos))));
        lst.add(Messenger.s(null, String.format(" - Redstone power level: %d", world.getRedstonePowerFromNeighbors(pos))));
        lst.add(Messenger.s(null, ""));
        lst.add(wander_chances(pos.up(), world));

        return lst;
    }

    private static ITextComponent wander_chances(BlockPos pos, World worldIn)
    {
        EntityCreature creature = new EntityPigZombie(worldIn);
        creature.onInitialSpawn(worldIn.getDifficultyForLocation(pos), null, null);
        creature.setLocationAndAngles(pos.getX()+0.5F, pos.getY(), pos.getZ()+0.5F, 0.0F, 0.0F);
        EntityAIWander wander = new EntityAIWander(creature, 0.8D);
        int success = 0;
        for (int i=0; i<1000; i++)
        {

            Vec3d vec = RandomPositionGenerator.findRandomTarget(creature, 10, 7);
            if (vec == null)
            {
                continue;
            }
            success++;
        }
        long total_ticks = 0;
        for (int trie=0; trie<1000; trie++)
        {
            int i;
            for (i=1;i<30*20*60; i++) //*60 used to be 5 hours, limited to 30 mins
            {
                if (wander.shouldExecute())
                {
                    break;
                }
            }
            total_ticks += 3*i;
        }
        creature.remove();
        long total_time = (total_ticks)/1000/20;
        return Messenger.s(null, String.format(" - Wander chance above: %.1f%%%%\n - Average standby above: %s",
                (100.0F*success)/1000,
                ((total_time>5000)?"INFINITY":(Long.toString(total_time)+" s"))
        ));
    }
}
