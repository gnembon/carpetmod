package carpet.utils;

import carpet.CarpetSettings;
import carpet.helpers.HopperCounter;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    public static void carpetPlacedAction(EnumDyeColor color, EntityPlayer placer, BlockPos pos, World worldIn)
    {
		if (!CarpetSettings.carpets)
		{
			return;
		}
        switch (color)
        {
            case PINK:
                if (CarpetSettings.commandSpawn)
                    Messenger.send(placer, SpawnReporter.report(pos, worldIn));

                break;
            case BLACK:
                if (CarpetSettings.commandSpawn)
                    Messenger.send(placer, SpawnReporter.show_mobcaps(pos, worldIn));
                break;
            case BROWN:
                if (CarpetSettings.commandDistance)
                {
                    CommandSource source = placer.getCommandSource();
                    if (!DistanceCalculator.hasStartingPoint(source) || placer.isSneaking()) {
                        DistanceCalculator.setStart(source, new Vec3d(pos));
                    }
                    else {
                        DistanceCalculator.setEnd(source, new Vec3d(pos));
                    }
                }
                break;
            case GRAY:
                if (CarpetSettings.commandInfo)
                    Messenger.send(placer, BlockInfo.blockInfo(pos.down(), worldIn));
                break;
            case YELLOW:
                if (CarpetSettings.commandInfo)
                    EntityInfo.issue_entity_info(placer);
                break;
			case GREEN:
                if (CarpetSettings.hopperCounters)
                {
                    EnumDyeColor under = getWoolColorAtPosition(worldIn, pos.down());
                    if (under == null) return;
                    Messenger.send(placer, HopperCounter.query_hopper_stats_for_color(worldIn.getServer(), under.toString(), false, false));
                }
				break;
			case RED:
                if (CarpetSettings.hopperCounters)
                {
                    EnumDyeColor under = getWoolColorAtPosition(worldIn, pos.down());
                    if (under == null) return;
                    HopperCounter.reset_hopper_counter(placer.getServer(), under.toString());
                    List<ITextComponent> res = new ArrayList<>();
                    res.add(Messenger.s(String.format("%s counter reset",under.toString())));
                    Messenger.send(placer, res);
                }
			    break;
        }
    }

    public static EnumDyeColor getWoolColorAtPosition(World worldIn, BlockPos pos)
    {
        IBlockState state = worldIn.getBlockState(pos);
        if (state.getMaterial() != Material.CLOTH || !state.isFullCube())
            return null;
        return Material2Dye.get(state.getMapColor(worldIn, pos));
    }
}
