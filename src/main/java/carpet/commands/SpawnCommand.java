package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.HopperCounter;
import carpet.helpers.TickSpeed;
import carpet.utils.Messenger;
import carpet.utils.SpawnReporter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;

import javax.swing.*;

import java.util.Arrays;

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
                                executes( (c) -> recentSpawnsForType(c.getSource(), getString(c, "type"))))).
                then(literal("test").
                        executes( (c)-> runTest(c.getSource(), 72000, null)).
                        then(argument("ticks", integer(10,720000)).
                                executes( (c)-> runTest(
                                        c.getSource(),
                                        getInteger(c, "ticks"),
                                        null)).
                                then(argument("counter", word()).
                                        suggests( (c, b) -> suggest(HopperCounter.counterStringSet,b)).
                                        executes((c)-> runTest(
                                                c.getSource(),
                                                getInteger(c, "ticks"),
                                                getString(c, "counter")))))).
                then(literal("mocking").
                        then(argument("to do or not to do?", BoolArgumentType.bool()).
                            executes( (c) -> toggleMocking(c.getSource(), BoolArgumentType.getBool(c, "to do or not to do?"))))).
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
                        then(argument("dimension", DimensionArgument.getDimension()).
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
        Messenger.send(source, SpawnReporter.tracking_report(source.getWorld()));
        return 1;
    }

    private static int startTracking(CommandSource source, BlockPos a, BlockPos b)
    {
        if (SpawnReporter.track_spawns != 0L)
        {
            Messenger.m(source, "r You are already tracking spawning.");
            return 0;
        }
        BlockPos lsl = null;
        BlockPos usl = null;
        if (a != null && b != null)
        {
            lsl = new BlockPos(
                    Math.min(a.getX(), b.getX()),
                    Math.min(a.getY(), b.getY()),
                    Math.min(a.getZ(), b.getZ()) );
            usl = new BlockPos(
                    Math.max(a.getX(), b.getX()),
                    Math.max(a.getY(), b.getY()),
                    Math.max(a.getZ(), b.getZ()) );
        }
        SpawnReporter.reset_spawn_stats(false);
        SpawnReporter.track_spawns = (long) source.getServer().getTickCounter();
        SpawnReporter.lower_spawning_limit = lsl;
        SpawnReporter.upper_spawning_limit = usl;
        Messenger.m(source, "gi Spawning tracking started.");
        return 1;
    }

    private static int stopTracking(CommandSource source)
    {
        Messenger.send(source, SpawnReporter.tracking_report(source.getWorld()));
        SpawnReporter.reset_spawn_stats(false);
        SpawnReporter.track_spawns = 0L;
        SpawnReporter.lower_spawning_limit = null;
        SpawnReporter.upper_spawning_limit = null;
        Messenger.m(source, "gi Spawning tracking stopped.");
        return 1;
    }

    private static int recentSpawnsForType(CommandSource source, String mob_type)
    {
        if (!Arrays.asList(SpawnReporter.mob_groups).contains(mob_type))
        {
            Messenger.m(source, "r Wrong mob type: "+mob_type);
            return 0;
        }
        Messenger.send(source, SpawnReporter.recent_spawns(source.getWorld(), mob_type));
        return 1;
    }

    private static int runTest(CommandSource source, int ticks, String counter)
    {
        //stop tracking
        SpawnReporter.reset_spawn_stats(false);
        //start tracking
        SpawnReporter.track_spawns = (long) source.getServer().getTickCounter();
        //counter reset
        HopperCounter.reset_hopper_counter(source.getServer(), counter);

        // tick warp 0
        TickSpeed.tickrate_advance(null, 0, null, null);
        // tick warp given player
        CommandSource csource = null;
        EntityPlayer player = null;
        try
        {
            player = source.asPlayer();
            csource = source;
        }
        catch (CommandSyntaxException ignored)
        {
        }
        TickSpeed.tickrate_advance(player, ticks, null, csource);
        Messenger.m(source, String.format("gi Started spawn test for %d ticks", ticks));
        return 1;
    }

    private static int toggleMocking(CommandSource source, boolean domock)
    {
        if (domock)
        {
            SpawnReporter.initialize_mocking();
            Messenger.m(source, "gi Mock spawns started, Spawn statistics reset");
        }
        else
        {
            SpawnReporter.stop_mocking();
            Messenger.m(source, "gi  Normal mob spawning, Spawn statistics reset");
        }
        return 1;
    }

    private static int generalMobcaps(CommandSource source)
    {
        Messenger.send(source, SpawnReporter.printMobcapsForDimension(source.getWorld().getDimension().getType().getId()));
        return 1;
    }

    private static int resetSpawnRates(CommandSource source)
    {
        for (String s: SpawnReporter.spawn_tries.keySet())
        {
            SpawnReporter.spawn_tries.put(s,1);
        }
        Messenger.m(source, "gi Spawn rates brought to 1 round per tick for all groups.");

        return 1;
    }

    private static int setSpawnRates(CommandSource source, String mobtype, int rounds)
    {
        String code = SpawnReporter.get_creature_code_from_string(mobtype);
        SpawnReporter.spawn_tries.put(code, rounds);
        Messenger.m(source, "gi "+mobtype+" mobs will now spawn "+rounds+" times per tick");
        return 1;
    }

    private static int setMobcaps(CommandSource source, int hostile_cap)
    {
        double desired_ratio = (double)hostile_cap/ EnumCreatureType.MONSTER.getMaxNumberOfCreature();
        SpawnReporter.mobcap_exponent = 4.0*Math.log(desired_ratio)/Math.log(2.0);
        Messenger.m(source, String.format("gi Mobcaps for hostile mobs changed to %d, other groups will follow", hostile_cap));
        return 1;
    }

    private static int mobcapsForDimension(CommandSource source, DimensionType dim)
    {
        Messenger.send(source, SpawnReporter.printMobcapsForDimension(dim.getId()));
        return 1;
    }

    private static int listEntitiesOfType(CommandSource source, String mobtype)
    {
        Messenger.send(source, SpawnReporter.printEntitiesByType(mobtype, source.getWorld()));
        return 1;
    }
}
