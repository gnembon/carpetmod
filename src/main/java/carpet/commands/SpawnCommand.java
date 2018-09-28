package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.HopperCounter;
import carpet.utils.Messenger;
import carpet.utils.SpawnReporter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.command.Commands.literal;
import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.ISuggestionProvider.suggest;
import static net.minecraft.command.arguments.BlockPosArgument.blockPos;
import static net.minecraft.command.arguments.BlockPosArgument.getBlockPos;


public class SpawnCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> literalargumentbuilder = literal("spawn").
                requires((player) -> CarpetSettings.getBool("commandSpawn"));

        literalargumentbuilder.
                then(literal("list").
                        then(argument("pos", blockPos()).
                                executes( (c) -> listSpawns(c.getSource(), getBlockPos(c, "pos"))))).
                then(literal("tracking").
                        executes( (c) -> printTrackingReport(c.getSource())).
                        then(literal("start").
                                executes( (c) -> startTracking(c.getSource(), null, null)).
                                then(argument("from", blockPos()).
                                        then(argument("to", blockPos()).
                                                executes( (c) -> startTracking(
                                                        c.getSource(),
                                                        getBlockPos(c, "from"),
                                                        getBlockPos(c, "to")))))).
                        then(literal("stop").
                                executes( (c) -> stopTracking(c.getSource()))).
                        then(argument("type", word()).
                                suggests( (c, b) -> suggest(SpawnReporter.mob_groups,b)).
                                executes( (c) -> trackReportForType(c.getSource(), getString(c, "type"))))).
                then(literal("test").
                        executes( (c)-> runTest(c.getSource(), 72000, "white")).
                        then(argument("ticks", integer(10,720000)).
                                executes( (c)-> runTest(
                                        c.getSource(),
                                        getInteger(c, "ticks"),
                                        "white")).
                                then(argument("counter", word()).
                                        suggests( (c, b) -> suggest(HopperCounter.counterStringSet,b)).
                                        executes((c)-> runTest(
                                                c.getSource(),
                                                getInteger(c, "ticks"),
                                                getString(c, "counter")))))).
                then(literal("mocking").
                        executes( (c) -> toggleMocking(c.getSource()))).
                then(literal("rates").
                        executes( (c) -> generalMobcaps(c.getSource())).
                        then(literal("reset").
                                executes( (c) -> resetSpawnRates(c.getSource()))).
                        then(argument("type", StringArgumentType.word()).
                                suggests( (c, b) -> ISuggestionProvider.suggest(SpawnReporter.mob_groups,b)).
                                then(argument("rounds", integer(0)).
                                        suggests( (c, b) -> ISuggestionProvider.suggest(new String[]{"1"},b)).
                                        executes( (c) -> setSpawnRates(
                                                c.getSource(),
                                                getString(c, "type"),
                                                getInteger(c, "rounds")))))).
                then(literal("mobcaps").
                        executes( (c) -> generalMobcaps(c.getSource())).
                        then(literal("set").
                                then(argument("cap (hostile)", integer(1,1400)).
                                        executes( (c) -> setMobcaps(c.getSource(), getInteger(c, "cap (hostile)"))))).
                        then(argument("dimension", DimensionArgument.func_212595_a()).
                                executes( (c)-> mobcapsForDimension(c.getSource(), DimensionArgument.func_212592_a(c, "dimension"))))).
                then(literal("entities").
                        executes( (c) -> generalMobcaps(c.getSource()) ).
                        then(argument("type", string()).
                                suggests( (c, b)->ISuggestionProvider.suggest(SpawnReporter.mob_groups, b)).
                                executes( (c) -> listEntitiesOfType(c.getSource(), getString(c, "type")))));

        dispatcher.register(literalargumentbuilder);
    }

    private static int listSpawns(CommandSource source, BlockPos pos)
    {
        Messenger.send(source, SpawnReporter.report(pos, source.getWorld()));
        return 1;
    }

    private static int printTrackingReport(CommandSource source)
    {

        return 1;
    }

    private static int startTracking(CommandSource source, BlockPos from, BlockPos to)
    {

        return 1;
    }

    private static int stopTracking(CommandSource source)
    {

        return 1;
    }

    private static int trackReportForType(CommandSource source, String mob_type)
    {

        return 1;
    }

    private static int runTest(CommandSource source, int ticks, String counter)
    {

        return 1;
    }

    private static int toggleMocking(CommandSource source)
    {

        return 1;
    }

    private static int generalMobcaps(CommandSource source)
    {
        Messenger.send(source, SpawnReporter.printMobcapsForDimension(source.getWorld().getDimension().getType().getId()));
        return 1;
    }

    private static int resetSpawnRates(CommandSource source)
    {

        return 1;
    }

    private static int setSpawnRates(CommandSource source, String mobtype, int rounds)
    {

        return 1;
    }

    private static int setMobcaps(CommandSource source, int hostile_cap)
    {

        return 1;
    }

    private static int mobcapsForDimension(CommandSource source, DimensionType dim)
    {
        Messenger.send(source, SpawnReporter.printMobcapsForDimension(dim.getId()));
        return 1;
    }

    private static int listEntitiesOfType(CommandSource source, String mobtype)
    {

        return 1;
    }
}
