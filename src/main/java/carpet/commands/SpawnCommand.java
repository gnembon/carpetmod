package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.math.BlockPos;

public class SpawnCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> literalargumentbuilder = Commands.literal("spawn").
                requires((player) -> CarpetSettings.getBool("commandSpawn"));

/*
        literalargumentbuilder.
                then(Commands.literal("list").
                        then(Commands.argument("pos", BlockPosArgument.blockPos()).
                                executes( (c) -> listSpawns(c.getSource(), BlockPosArgument.getBlockPos(c, "pos"))))).
                then(Commands.literal("tracking").
                        executes( (c) -> printTrackingReport(c.getSource())).
                        then(Commands.literal("start").
                                executes( (c) -> startTracking(c.getSource())).
                                then(Commands.argument("from", BlockPosArgument.blockPos()).
                                        then(Commands.argument("to", BlockPosArgument.blockPos()).
                                                executes( (c) -> startTracking(
                                                        c.getSource(),
                                                        BlockPosArgument.getBlockPos(c, "from"),
                                                        BlockPosArgument.getBlockPos(c, "to")))))).
                        then(Commands.literal("stop").
                                executes( (c) -> stopTracking(c.getSource()))).
                        then(Commands.argument("type", TermArgumentType.term("hostile","passive","water","ambient")).
                                executes( (c) -> trackReportForType(c.getSource(), TermArgumentType.getType(c, "type"))))).
                then(Commands.literal("test").
                        executes( (c)-> runTest(c.getSource(), 72000, "white")).
                        then(Commands.argument().
                                executes( (c)-> runTest(c.getSource(), time, "white")).
                                then(Commands.argument().
                                        executes((c)-> runTest(c.getSource(), arg, color))))).
                then(Commands.literal("mocking").
                        executes()).
                then(Commands.literal("rates").
                        executes().
                        then(Commands.literal("reset").
                                executes()).
                        then(Commands.argument("type", TermArgumentType.mobType()).
                                executes())).
                then(Commands.literal("mobcaps").
                        executes().
                        then(Commands.literal("set").
                                then(Commands.argument("cap(hostile)", ).
                                        suggests().
                                        executes())).
                        then(Commands.argument("dimension", DimensionArgument.func_212595_a()).
                                executes())).
                then(Commands.literal("entities").
                        executes().
                        then(Commands.argument("type", TermArgumentType.mobType()).
                                executes()));
*/
        dispatcher.register(literalargumentbuilder);
    }

    private static int list(CommandSource source, BlockPos pos)
    {
        Messenger.m(source,"w listing spawns at position", Messenger.tp("wb",pos ));
        return 1;
    }
}
