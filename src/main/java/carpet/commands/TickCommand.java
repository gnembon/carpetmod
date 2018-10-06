package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.TickSpeed;
import carpet.utils.CarpetProfiler;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;

import static com.mojang.brigadier.arguments.FloatArgumentType.*;
import static com.mojang.brigadier.arguments.IntegerArgumentType.*;
import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.command.Commands.*;

public class TickCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> literalargumentbuilder = literal("tick").
                requires((player) -> CarpetSettings.getBool("commandTick")).
                then(literal("rate").
                        executes((c) -> queryTps(c.getSource())).
                        then(argument("rate", floatArg(0.1F, 500.0F)).
                                suggests( (c, b) -> ISuggestionProvider.suggest(new String[]{"20.0"},b)).
                                executes((c) -> setTps(c.getSource(), getFloat(c, "rate"))))).
                then(literal("warp").
                        executes( (c)-> setWarp(c.getSource(), 0, null)).
                        then(argument("ticks", integer(0,4000000)).
                                suggests( (c, b) -> ISuggestionProvider.suggest(new String[]{"3600","72000"},b)).
                                executes((c) -> setWarp(c.getSource(), getInteger(c,"ticks"), null)).
                                then(argument("tail command", greedyString()).
                                        executes( (c) -> setWarp(
                                                c.getSource(),
                                                getInteger(c,"ticks"),
                                                getString(c, "tail command")))))).
                then(literal("freeze").executes( (c)-> toggleFreeze(c.getSource()))).
                then(literal("step").
                        executes((c) -> step(1))).
                        then(argument("ticks", integer(1,72000)).
                                suggests( (c, b) -> ISuggestionProvider.suggest(new String[]{"20"},b)).
                                executes((c) -> step(getInteger(c,"ticks")))).
                then(literal("superHot").executes( (c)-> toggleSuperHot(c.getSource()))).
                then(literal("health").
                        executes( (c) -> healthReport(c.getSource(), 100)).
                        then(argument("ticks", integer(20,24000)).
                                executes( (c) -> healthReport(c.getSource(), getInteger(c, "ticks"))))).
                then(literal("entities").
                        executes((c) -> healthEntities(c.getSource(), 100)).
                        then(argument("ticks", integer(20,24000)).
                                executes((c) -> healthEntities(c.getSource(), getInteger(c, "ticks")))));


        dispatcher.register(literalargumentbuilder);
    }


    private static int setTps(CommandSource source, float tps)
    {
        TickSpeed.tickrate(tps);
        queryTps(source);
        return (int)tps;
    }

    private static int queryTps(CommandSource source)
    {
        Messenger.m(source, "w Current tps is: ",String.format("wb %.1f", TickSpeed.tickrate));
        return (int)TickSpeed.tickrate;
    }

    private static int setWarp(CommandSource source, int advance, String tail_command)
    {
        EntityPlayer player = null;
        try
        {
            player = source.asPlayer();
        }
        catch (CommandSyntaxException ignored)
        {
        }
        ITextComponent message = TickSpeed.tickrate_advance(player, advance, tail_command, source);
        if (message != null)
        {
            source.sendFeedback(message, false);
        }
        return 1;
    }

    private static int toggleFreeze(CommandSource source)
    {
        TickSpeed.is_paused = !TickSpeed.is_paused;
        if (TickSpeed.is_paused)
        {
            Messenger.m(source, "gi Game is paused");
        }
        else
        {
            Messenger.m(source, "gi Game runs normally");
        }
        return 1;
    }

    private static int step(int advance)
    {
        TickSpeed.add_ticks_to_run_in_pause(advance);
        return 1;
    }

    private static int toggleSuperHot(CommandSource source)
    {
        TickSpeed.is_superHot = !TickSpeed.is_superHot;
        if (TickSpeed.is_superHot)
        {
            Messenger.m(source,"gi Superhot enabled");
        }
        else
        {
            Messenger.m(source, "gi Superhot disabled");
        }
        return 1;
    }

    private static int healthReport(CommandSource source, int ticks)
    {
        CarpetProfiler.prepare_tick_report(ticks);
        return 1;
    }

    private static int healthEntities(CommandSource source, int ticks)
    {
        CarpetProfiler.prepare_entity_report(ticks);
        return 1;
    }

}

