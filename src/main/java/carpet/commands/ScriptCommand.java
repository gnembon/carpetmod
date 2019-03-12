package carpet.commands;

import carpet.CarpetSettings;
import carpet.script.CarpetExpression;
import carpet.script.Expression;
import carpet.script.Tokenizer;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;
import static net.minecraft.command.ISuggestionProvider.suggest;

public class ScriptCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> command = literal("script").
                requires((player) -> CarpetSettings.getBool("commandScript")).
                then(literal("globals").executes( (c) -> listGlobals(c.getSource()))).
                then(literal("stop").executes( (c) -> { Expression.stopAll = true; return 1;})).
                then(literal("resume").executes( (c) -> { Expression.stopAll = false; return 1;})).
                then(literal("run").
                        then(argument("expr", StringArgumentType.greedyString()).
                                executes((c) -> compute(
                                        c.getSource(),
                                        StringArgumentType.getString(c, "expr")
                                )))).

                then(literal("invoke").
                        then(argument("call", StringArgumentType.word()).suggests( (c, b)->suggest(getGlobalCalls(),b)).
                                executes( (c) -> invoke(
                                        c.getSource(),
                                        StringArgumentType.getString(c, "call"),
                                        null,
                                        null,
                                        ""
                                )).
                                then(argument("arguments", StringArgumentType.greedyString()).
                                        executes( (c) -> invoke(
                                                c.getSource(),
                                                StringArgumentType.getString(c, "call"),
                                                null,
                                                null,
                                                StringArgumentType.getString(c, "arguments")
                                        ))))).
                then(literal("invokepoint").
                        then(argument("call", StringArgumentType.word()).suggests( (c, b)->suggest(getGlobalCalls(),b)).
                                then(argument("origin", BlockPosArgument.blockPos()).
                                        executes( (c) -> invoke(
                                                c.getSource(),
                                                StringArgumentType.getString(c, "call"),
                                                BlockPosArgument.getBlockPos(c, "origin"),
                                                null,
                                                ""
                                        )).
                                        then(argument("arguments", StringArgumentType.greedyString()).
                                                executes( (c) -> invoke(
                                                        c.getSource(),
                                                        StringArgumentType.getString(c, "call"),
                                                        BlockPosArgument.getBlockPos(c, "origin"),
                                                        null,
                                                        StringArgumentType.getString(c, "arguments")
                                                )))))).
                then(literal("invokearea").
                        then(argument("call", StringArgumentType.word()).suggests( (c, b)->suggest(getGlobalCalls(),b)).
                                then(argument("from", BlockPosArgument.blockPos()).
                                        then(argument("to", BlockPosArgument.blockPos()).
                                                executes( (c) -> invoke(
                                                        c.getSource(),
                                                        StringArgumentType.getString(c, "call"),
                                                        BlockPosArgument.getBlockPos(c, "from"),
                                                        BlockPosArgument.getBlockPos(c, "to"),
                                                        ""
                                                )).
                                                then(argument("arguments", StringArgumentType.greedyString()).
                                                        executes( (c) -> invoke(
                                                                c.getSource(),
                                                                StringArgumentType.getString(c, "call"),
                                                                BlockPosArgument.getBlockPos(c, "from"),
                                                                BlockPosArgument.getBlockPos(c, "to"),
                                                                StringArgumentType.getString(c, "arguments")
                                                        ))))))).
                then(literal("scan").
                        then(argument("origin", BlockPosArgument.blockPos()).
                                then(argument("from", BlockPosArgument.blockPos()).
                                        then(argument("to", BlockPosArgument.blockPos()).
                                                then(argument("expr", StringArgumentType.string()).
                                                        executes( (c) -> scriptScan(
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
    private static Set<String> getGlobalCalls()
    {
        return Expression.global_functions.keySet().stream().filter((s) -> !s.startsWith("_")).collect(Collectors.toSet());
    }
    private static int listGlobals(CommandSource source)
    {
        Messenger.m(source, "w Global functions:");
        for (String fname : getGlobalCalls())
        {
            Expression.UserDefinedFunction acf = Expression.global_functions.get(fname);

            String expr = acf.getExpression().getCodeString();
            Tokenizer.Token tok = acf.getToken();
            List<String> snippet = Expression.getExpressionSnippet(tok, expr);
            Messenger.m(source, "w Function "+fname+" defined at: line "+(tok.lineno+1)+" pos "+(tok.linepos+1));
            for (String snippetLine: snippet)
            {
                Messenger.m(source, "li "+snippetLine);
            }
            Messenger.m(source, "gi ----------------");
        }
        //Messenger.m(source, "w "+code);
        Messenger.m(source, "w Global Variables:");

        for (String vname : Expression.global_variables.keySet())
        {
            Messenger.m(source, "w Variable "+vname+": ", "wb "+Expression.global_variables.get(vname).evalValue(null).getString());
        }
        return 1;
    }

    private static void handleCall(CommandSource source, Supplier<String> call)
    {
        try
        {
            CarpetExpression.setChatErrorSnooper(source);
            long start = System.nanoTime();
            String result = call.get();
            int time = (int)((System.nanoTime()-start)/1000);
            String metric = "\u00B5s";
            if (time > 2000)
            {
                time /= 1000;
                metric = "ms";
            }
            Messenger.m(source, "wi  = ", "wb "+result, "gi  ("+time+metric+")");
        }
        catch (CarpetExpression.CarpetExpressionException e)
        {
            Messenger.m(source, "r Exception white evaluating expression at "+new BlockPos(source.getPos())+": "+e.getMessage());
        }
        CarpetExpression.resetErrorSnooper();
    }

    private static int invoke(CommandSource source, String call, BlockPos pos1, BlockPos pos2,  String args)
    {
        if (call.startsWith("__"))
        {
            Messenger.m(source, "r Hidden functions are only callable in scripts");
            return 0;
        }
        List<String> arguments = new ArrayList<>();
        if (pos1 != null)
        {
            arguments.add(Integer.toString(pos1.getX()));
            arguments.add(Integer.toString(pos1.getY()));
            arguments.add(Integer.toString(pos1.getZ()));
        }
        if (pos2 != null)
        {
            arguments.add(Integer.toString(pos2.getX()));
            arguments.add(Integer.toString(pos2.getY()));
            arguments.add(Integer.toString(pos2.getZ()));
        }
        if (!(args.trim().isEmpty()))
            arguments.addAll(Arrays.asList(args.trim().split("\\s+")));
        handleCall(source, () -> CarpetExpression.invokeGlobal(source, call,arguments));
        return 1;
    }


    private static int compute(CommandSource source, String expr)
    {
        handleCall(source, () -> {
            CarpetExpression ex = new CarpetExpression(expr, source, new BlockPos(0, 0, 0));
            if (source.getWorld().getGameRules().getBoolean("commandBlockOutput"))
                ex.setLogOutput(true);
            return ex.eval(new BlockPos(source.getPos()));
        });
        return 1;
    }

    private static int scriptScan(CommandSource source, BlockPos origin, BlockPos a, BlockPos b, String expr)
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

