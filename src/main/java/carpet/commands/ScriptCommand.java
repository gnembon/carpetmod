package carpet.commands;

import carpet.CarpetSettings;
import carpet.script.CarpetExpression;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockWorldState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.BlockPredicateArgument;
import net.minecraft.command.arguments.BlockStateArgument;
import net.minecraft.command.arguments.BlockStateInput;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.WorldServer;

import java.util.function.Predicate;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public class ScriptCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> command = literal("script").
                requires((player) -> CarpetSettings.getBool("commandScript")).
                then(literal("run").
                        then(argument("expr", StringArgumentType.greedyString()).
                                executes((c) -> compute(
                                        c.getSource(),
                                        StringArgumentType.getString(c, "expr")
                                )))).
                then(literal("space").
                        then(argument("origin", BlockPosArgument.blockPos()).
                                then(argument("from", BlockPosArgument.blockPos()).
                                        then(argument("to", BlockPosArgument.blockPos()).
                                                then(argument("expr", StringArgumentType.string()).
                                                        executes( (c) -> scriptSpace(
                                                                c.getSource(),
                                                                BlockPosArgument.getBlockPos(c, "origin"),
                                                                BlockPosArgument.getBlockPos(c, "from"),
                                                                BlockPosArgument.getBlockPos(c, "to"),
                                                                StringArgumentType.getString(c, "expr")
                                                        ))))))).
                then(literal("fill").
                        then(argument("origin", BlockPosArgument.blockPos()).
                                then(argument("from", BlockPosArgument.blockPos()).
                                        then(argument("to", BlockPosArgument.blockPos()).
                                                then(argument("expr", StringArgumentType.string()).
                                                        then(argument("block", BlockStateArgument.blockState()).
                                                                executes((c) -> scriptFill(
                                                                        c.getSource(),
                                                                        BlockPosArgument.getBlockPos(c, "origin"),
                                                                        BlockPosArgument.getBlockPos(c, "from"),
                                                                        BlockPosArgument.getBlockPos(c, "to"),
                                                                        StringArgumentType.getString(c, "expr"),
                                                                        BlockStateArgument.getBlockStateInput(c, "block"),
                                                                        null, "solid"
                                                                )).
                                                        then(literal("replace").
                                                                then(argument("filter", BlockPredicateArgument.blockPredicateArgument())
                                                                        .executes((c) -> scriptFill(
                                                                                c.getSource(),
                                                                                BlockPosArgument.getBlockPos(c, "origin"),
                                                                                BlockPosArgument.getBlockPos(c, "from"),
                                                                                BlockPosArgument.getBlockPos(c, "to"),
                                                                                StringArgumentType.getString(c, "expr"),
                                                                                BlockStateArgument.getBlockStateInput(c, "block"),
                                                                                BlockPredicateArgument.getBlockPredicate(c, "filter"),
                                                                                "solid"
                                                                        )))))))))).
                then(literal("outline").
                        then(argument("origin", BlockPosArgument.blockPos()).
                                then(argument("from", BlockPosArgument.blockPos()).
                                        then(argument("to", BlockPosArgument.blockPos()).
                                                then(argument("expr", StringArgumentType.string()).
                                                        then(argument("block", BlockStateArgument.blockState()).
                                                                executes((c) -> scriptFill(
                                                                        c.getSource(),
                                                                        BlockPosArgument.getBlockPos(c, "origin"),
                                                                        BlockPosArgument.getBlockPos(c, "from"),
                                                                        BlockPosArgument.getBlockPos(c, "to"),
                                                                        StringArgumentType.getString(c, "expr"),
                                                                        BlockStateArgument.getBlockStateInput(c, "block"),
                                                                        null, "outline"
                                                                )).
                                                                then(literal("replace").
                                                                        then(argument("filter", BlockPredicateArgument.blockPredicateArgument())
                                                                                .executes((c) -> scriptFill(
                                                                                        c.getSource(),
                                                                                        BlockPosArgument.getBlockPos(c, "origin"),
                                                                                        BlockPosArgument.getBlockPos(c, "from"),
                                                                                        BlockPosArgument.getBlockPos(c, "to"),
                                                                                        StringArgumentType.getString(c, "expr"),
                                                                                        BlockStateArgument.getBlockStateInput(c, "block"),
                                                                                        BlockPredicateArgument.getBlockPredicate(c, "filter"),
                                                                                        "outline"
                                                                                ))))))))));

        dispatcher.register(command);
    }
    private static int compute(CommandSource source, String expr)
    {
        BlockPos pos = new BlockPos(source.getPos());
        try
        {
            CarpetExpression ex = new CarpetExpression(expr, source, new BlockPos(0, 0, 0));
            if (source.getWorld().getGameRules().getBoolean("commandBlockOutput"))
                ex.setLogOutput(true);
            long start = System.nanoTime();
            String result = ex.eval(pos);
            int time = (int)(System.nanoTime()-start)/1000;
            String metric = "\u00B5s";
            if (time > 2000)
            {
                time /= 1000;
                metric = "ms";
            }
            Messenger.m(source, "wi "+expr,"wi  = ", "wb "+result, "gi  ("+time+metric+")");
        }
        catch (CarpetExpression.CarpetExpressionException e)
        {
            Messenger.m(source, "r Exception white evaluating expression at "+pos+": "+e.getMessage());
        }
        catch (ArithmeticException e)
        {
            Messenger.m(source, "r Your math is wrong, sorry: "+e.getMessage());
        }
        return 1;
    }

    private static int scriptSpace(CommandSource source, BlockPos origin, BlockPos a, BlockPos b, String expr)
    {
        MutableBoundingBox area = new MutableBoundingBox(a, b);
        CarpetExpression cexpr = new CarpetExpression(expr, source, origin);
        if (area.getXSize() * area.getYSize() * area.getZSize() > CarpetSettings.getInt("fillLimit"))
        {
            Messenger.m(source, "r too many blocks to evaluate: " + area.getXSize() * area.getYSize() * area.getZSize());
            return 1;
        }
        int successCount = 0;
        try
        {
            for (int x = area.minX; x <= area.maxX; x++)
            {
                for (int y = area.minY; y <= area.maxY; y++)
                {
                    for (int z = area.minZ; z <= area.maxZ; z++)
                    {
                        try
                        {
                            if (cexpr.test(x, y, z)) successCount++;
                        }
                        catch (ArithmeticException ignored)
                        {
                        }
                    }
                }
            }
        }
        catch (CarpetExpression.CarpetExpressionException exc)
        {
            Messenger.m(source, "r Error while processing command: "+exc);
            return 0;
        }
        Messenger.m(source, "w Expression successful in " + successCount + " out of " + area.getXSize() * area.getYSize() * area.getZSize() + " blocks");
        return successCount;

    }


    private static int scriptFill(CommandSource source, BlockPos origin, BlockPos a, BlockPos b, String expr,
                                BlockStateInput block, Predicate<BlockWorldState> replacement, String mode)
    {
        MutableBoundingBox area = new MutableBoundingBox(a, b);
        CarpetExpression cexpr = new CarpetExpression(expr, source, origin);
        if (area.getXSize() * area.getYSize() * area.getZSize() > CarpetSettings.getInt("fillLimit"))
        {
            Messenger.m(source, "r too many blocks to evaluate: "+ area.getXSize() * area.getYSize() * area.getZSize());
            return 1;
        }

        boolean[][][] volume = new boolean[area.getXSize()][area.getYSize()][area.getZSize()];

        BlockPos.MutableBlockPos mbpos = new BlockPos.MutableBlockPos(origin);
        WorldServer world = source.getWorld();


        for (int x = area.minX; x <= area.maxX; x++)
        {
            for (int y = area.minY; y <= area.maxY; y++)
            {
                for (int z = area.minZ; z <= area.maxZ; z++)
                {
                    try
                    {
                        if (cexpr.test(x, y, z))
                        {
                            volume[x-area.minX][y-area.minY][z-area.minZ]=true;
                        }
                    }
                    catch (CarpetExpression.CarpetExpressionException e)
                    {
                        CarpetSettings.LOG.error("Exception: "+e);
                        return 0;
                    }
                    catch (ArithmeticException e)
                    {
                    }
                }
            }
        }
        final int maxx = area.getXSize()-1;
        final int maxy = area.getYSize()-1;
        final int maxz = area.getZSize()-1;
        if ("edge".equalsIgnoreCase(mode))
        {
            boolean[][][] newVolume = new boolean[area.getXSize()][area.getYSize()][area.getZSize()];
            for (int x = 0; x <= maxx; x++)
            {
                for (int y = 0; y <= maxy; y++)
                {
                    for (int z = 0; z <= maxz; z++)
                    {
                        if (volume[x][y][z])
                        {
                            if ( (  (x != 0    && !volume[x-1][y  ][z  ]) ||
                                    (x != maxx && !volume[x+1][y  ][z  ]) ||
                                    (y != 0    && !volume[x  ][y-1][z  ]) ||
                                    (y != maxy && !volume[x  ][y+1][z  ]) ||
                                    (z != 0    && !volume[x  ][y  ][z-1]) ||
                                    (z != maxz && !volume[x  ][y  ][z+1])
                            ))
                            {
                                newVolume[x][y][z] = true;
                            }
                        }
                    }
                }
            }
            volume = newVolume;
        }
        int affected = 0;
        for (int x = 0; x <= maxx; x++)
        {
            for (int y = 0; y <= maxy; y++)
            {
                for (int z = 0; z <= maxz; z++)
                {
                    if (volume[x][y][z])
                    {
                        mbpos.setPos(x+area.minX, y+area.minY, z+area.minZ);
                        if (replacement == null || replacement.test(
                                new BlockWorldState( world, mbpos, true)))
                        {
                            TileEntity tileentity = world.getTileEntity(mbpos);
                            if (tileentity instanceof IInventory)
                            {
                                ((IInventory)tileentity).clear();
                            }
                            if (block.place(
                                    world,
                                    mbpos,
                                    2 | (CarpetSettings.getBool("fillUpdates") ?0:1024)
                            ))
                            {
                                ++affected;
                            }
                        }
                    }
                }
            }
        }
        if (CarpetSettings.getBool("fillUpdates") && block != null)
        {
            for (int x = 0; x <= maxx; x++)
            {
                for (int y = 0; y <= maxy; y++)
                {
                    for (int z = 0; z <= maxz; z++)
                    {
                        if (volume[x][y][z])
                        {
                            mbpos.setPos(x+area.minX, y+area.minY, z+area.minZ);
                            Block blokc = world.getBlockState(mbpos).getBlock();
                            world.notifyNeighbors(mbpos, blokc);
                        }
                    }
                }
            }
        }
        Messenger.m(source, "gi Affected "+affected+" blocks in "+area.getXSize() * area.getYSize() * area.getZSize()+" block volume");
        return 1;
    }
}

