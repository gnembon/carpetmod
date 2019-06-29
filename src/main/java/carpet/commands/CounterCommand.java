package carpet.commands;

import carpet.helpers.HopperCounter;
import carpet.settings.CarpetSettings;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.text.ITextComponent;

public class CounterCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> literalargumentbuilder = Commands.literal("counter").executes((context)
         -> listAllCounters(context.getSource(), false)).requires((player) ->
                CarpetSettings.hopperCounters);

        literalargumentbuilder.
                then((Commands.literal("reset").executes( (p_198489_1_)->
                        resetCounter(p_198489_1_.getSource(), null))));
        for (EnumDyeColor enumDyeColor: EnumDyeColor.values())
        {
            String color = enumDyeColor.toString();
            literalargumentbuilder.
                    then((Commands.literal(color).executes( (p_198489_1_)-> displayCounter(p_198489_1_.getSource(), color, false))));
            literalargumentbuilder.then(Commands.literal(color).
                    then(Commands.literal("reset").executes((context) ->
                            resetCounter(context.getSource(), color))));
            literalargumentbuilder.then(Commands.literal(color).
                    then(Commands.literal("realtime").executes((context) ->
                            displayCounter(context.getSource(), color, true))));
        }
        dispatcher.register(literalargumentbuilder);
    }

    private static int displayCounter(CommandSource source, String color, boolean realtime)
    {
        HopperCounter counter = HopperCounter.getCounter(color);
        if (counter == null) throw new CommandException(Messenger.s("Unknown wool color: "+color));

        for (ITextComponent message: counter.format(source.getServer(), realtime, false))
        {
            source.sendFeedback(message, false);
        }
        return 1;
    }

    private static int resetCounter(CommandSource source, String color)
    {
        if (color == null)
        {
            HopperCounter.resetAll(source.getServer());
            Messenger.m(source, "w Restarted all counters");
        }
        else
        {
            HopperCounter counter = HopperCounter.getCounter(color);
            if (counter == null) throw new CommandException(Messenger.s("Unknown wool color"));
            counter.reset(source.getServer());
            Messenger.m(source, "w Restarted "+color+" counter");
        }
        return 1;
    }

    private static int listAllCounters(CommandSource source, boolean realtime)
    {
        for (ITextComponent message: HopperCounter.formatAll(source.getServer(), realtime))
        {
            source.sendFeedback(message, false);
        }
        return 1;
    }

}
