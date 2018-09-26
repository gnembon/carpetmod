package carpet.utils;

import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;

public class WoolTool
{
    private static final HashMap<MaterialColor,EnumDyeColor> Material2Dye = new HashMap<>();
    static
    {
        for (EnumDyeColor color: EnumDyeColor.values())
        {
            Material2Dye.put(color.getMapColor(),color);
        }
    }
    /*
    public static void carpetPlacedAction(EnumDyeColor color, EntityPlayer placer, BlockPos pos, World worldIn)
    {
		if (!CarpetSettings.getBool("carpets"))
		{
			return;
		}
        switch (color)
        {
            case PINK:
                if (CarpetSettings.getBool("commandSpawn"))
                    Messenger.send(placer, SpawnReporter.report(pos, worldIn));

                break;
            case BLACK:
                if (CarpetSettings.getBool("commandSpawn"))
                    Messenger.send(placer, SpawnReporter.show_mobcaps(pos, worldIn));
                break;
            case BROWN:
                if (CarpetSettings.getBool("commandDistance"))
                {
                    DistanceCalculator.report_distance(placer, pos);
                }
                break;
            case GRAY:
                if (CarpetSettings.getBool("commandBlockInfo"))
                    Messenger.send(placer, BlockInfo.blockInfo(pos.down(), worldIn));
                break;
            case YELLOW:
                if (CarpetSettings.getBool("commandEntityInfo"))
                    EntityInfo.issue_entity_info(placer);
                break;
			case GREEN:
                if (CarpetSettings.getBool("hopperCounters"))
                {
                    EnumDyeColor under = getWoolColorAtPosition(worldIn, pos.down());
                    if (under == null) return;
                    Messenger.send(placer, HopperCounter.query_hopper_stats_for_color(worldIn.getMinecraftServer(), under.toString(), false, false));
                }
				break;
			case RED:
                if (CarpetSettings.getBool("hopperCounters"))
                {
                    EnumDyeColor under = getWoolColorAtPosition(worldIn, pos.down());
                    if (under == null) return;
                    HopperCounter.reset_hopper_counter(worldIn, under.toString());
                    Messenger.s(placer, String.format("%s counter reset",under.toString() ));
                }
			    break;
        }
    }*/

    public static EnumDyeColor getWoolColorAtPosition(World worldIn, BlockPos pos)
    {
        IBlockState state = worldIn.getBlockState(pos);
        if (state.getMaterial() != Material.CLOTH || !state.isNormalCube())
            return null;
        return Material2Dye.get(state.getMapColor(worldIn, pos));
    }
}
