package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.TickSpeed;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;

public class TickCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> literalargumentbuilder = Commands.literal("tick").requires((player) ->
                CarpetSettings.getBool("commandTick"));


        literalargumentbuilder.
                then((Commands.literal("rate").executes((p_198489_1_) ->
                    queryTps(p_198489_1_.getSource()))).                                  // suggests 20F
                then(Commands.argument("rate", FloatArgumentType.floatArg(0.1F, 500.0F)).executes((p_198490_1_) ->
                    setTps(p_198490_1_.getSource(), FloatArgumentType.getFloat(p_198490_1_, "rate")))));
        literalargumentbuilder.
                then((Commands.literal("warp").
                then(Commands.argument("ticks",IntegerArgumentType.integer(0,4000000)).executes((p_198490_1_) ->
                    setWarp(p_198490_1_.getSource(),IntegerArgumentType.getInteger(p_198490_1_,"ticks"))))));
        literalargumentbuilder.
                then((Commands.literal("freeze").executes( (p_198489_1_)-> toggleFreeze(p_198489_1_.getSource()))));
        literalargumentbuilder.
                then((Commands.literal("step").executes((p_198489_1_) ->
                        step(1))).                       // suggests 20F
                        then(Commands.argument("ticks", IntegerArgumentType.integer(1,72000)).executes((p_198490_1_) ->
                        step(IntegerArgumentType.getInteger(p_198490_1_,"ticks")))));
        literalargumentbuilder.
                then((Commands.literal("superHot").executes( (p_198489_1_)-> toggleSuperHot(p_198489_1_.getSource()))));


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
        source.sendFeedback(Messenger.m(null, "w Current tps is: ",String.format("wb %.1f", TickSpeed.tickrate)), false);
        return (int)TickSpeed.tickrate;
    }

    private static int setWarp(CommandSource source, int advance)
    {
        EntityPlayer player = null;
        try
        {
            player = source.asPlayer();
        }
        catch (CommandSyntaxException e)
        {
        }
        String s = null;
        // TODO: post command
        ITextComponent message = TickSpeed.tickrate_advance(player, advance, s, source);
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
            source.sendFeedback(Messenger.m(null, "gi Game is paused"),false);
        }
        else
        {
            source.sendFeedback(Messenger.m(null, "gi Game runs normally"), false);
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
            source.sendFeedback(Messenger.m(null,"gi Superhot enabled"), false);
        }
        else
        {
            source.sendFeedback(Messenger.m(null, "gi Superhot disabled"), false);
        }
        return 1;
    }

}

