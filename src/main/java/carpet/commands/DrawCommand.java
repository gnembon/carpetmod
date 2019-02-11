package carpet.commands;

import carpet.CarpetSettings;
import carpet.script.CarpetExpression;
import carpet.utils.Messenger;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

import java.util.List;
import java.util.function.Predicate;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public class DrawCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> command = literal("draw").
                requires((player) -> CarpetSettings.getBool("commandDraw")).
                then(literal("sphere").
                        then(argument("center",BlockPosArgument.blockPos()).
                                then(argument("radius",IntegerArgumentType.integer(1)).
                                        then(argument("block",BlockStateArgument.blockState()).
                                                executes((c)-> drawCircle(
                                                        c.getSource(),
                                                        BlockPosArgument.getBlockPos(c, "center"),
                                                        IntegerArgumentType.getInteger(c, "radius"),
                                                        BlockStateArgument.getBlockStateInput(c,"block"),
                                                        null
                                                        )
                                                ).
                                                then(literal("replace").
                                                        then(argument("filter",BlockPredicateArgument.blockPredicateArgument())
                                                                .executes((c)-> drawCircle(
                                                                        c.getSource(),
                                                                        BlockPosArgument.getBlockPos(c, "center"),
                                                                        IntegerArgumentType.getInteger(c, "radius"),
                                                                        BlockStateArgument.getBlockStateInput(c,"block"),
                                                                        BlockPredicateArgument.getBlockPredicate(c,
                                                                                "filter")
                                                                        ))))))));
        dispatcher.register(command);
    }


    private static int drawCircle(CommandSource source, BlockPos pos, int radius, BlockStateInput block,
                                  Predicate<BlockWorldState> replacement)
    {
        return drawCircle(source, pos, (double)radius, (double)radius, (double)radius, block, replacement, false);
    }
    private static int drawCircle(CommandSource source, BlockPos pos, double radiusX, double radiusY, double radiusZ,
                                  BlockStateInput block, Predicate<BlockWorldState> replacement, boolean solid)
    {
        int affected = 0;
        WorldServer world = source.getWorld();

        radiusX += 0.5;
        radiusY += 0.5;
        radiusZ += 0.5;

        final double invRadiusX = 1 / radiusX;
        final double invRadiusY = 1 / radiusY;
        final double invRadiusZ = 1 / radiusZ;

        final int ceilRadiusX = (int) Math.ceil(radiusX);
        final int ceilRadiusY = (int) Math.ceil(radiusY);
        final int ceilRadiusZ = (int) Math.ceil(radiusZ);

        BlockPos.MutableBlockPos mbpos = new BlockPos.MutableBlockPos(pos);
        List<BlockPos> list = Lists.<BlockPos>newArrayList();

        double nextXn = 0;

        forX: for (int x = 0; x <= ceilRadiusX; ++x) {
            final double xn = nextXn;
            nextXn = (x + 1) * invRadiusX;
            double nextYn = 0;
            forY: for (int y = 0; y <= ceilRadiusY; ++y) {
                final double yn = nextYn;
                nextYn = (y + 1) * invRadiusY;
                double nextZn = 0;
                forZ: for (int z = 0; z <= ceilRadiusZ; ++z) {
                    final double zn = nextZn;
                    nextZn = (z + 1) * invRadiusZ;

                    double distanceSq = lengthSq(xn, yn, zn);
                    if (distanceSq > 1) {
                        if (z == 0) {
                            if (y == 0) {
                                break forX;
                            }
                            break forY;
                        }
                        break forZ;
                    }

                    if (!solid) {
                        if (lengthSq(nextXn, yn, zn) <= 1 && lengthSq(xn, nextYn, zn) <= 1 && lengthSq(xn, yn, nextZn) <= 1) {
                            continue;
                        }
                    }

                    for (int xmod = -1; xmod < 2; xmod+= 2)
                    {
                        for (int ymod = -1; ymod < 2; ymod += 2)
                        {
                            for (int zmod = -1; zmod < 2; zmod += 2)
                            {
                                mbpos.setPos(pos.getX()+xmod*x, pos.getY()+ymod*y, pos.getZ()+zmod*z);
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
                                        list.add(mbpos.toImmutable());
                                        ++affected;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (CarpetSettings.getBool("fillUpdates"))
        {

            for (BlockPos blockpos1 : list)
            {
                Block blokc = world.getBlockState(blockpos1).getBlock();
                world.notifyNeighbors(blockpos1, blokc);
            }
        }
        Messenger.m(source, "gi Filled "+affected+" blocks");

        return 1;
    }
    private static double lengthSq(double x, double y, double z)
        {
        return (x * x) + (y * y) + (z * z);
    }
}
