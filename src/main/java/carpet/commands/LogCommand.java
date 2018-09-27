package carpet.commands;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.entity.player.EntityPlayer;

import java.util.*;

public class LogCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> literalargumentbuilder = Commands.literal("log").
                requires((player) -> CarpetSettings.getBool("commandLog")).
                executes((context) -> listLogs(context.getSource())).
                then(Commands.literal("clear").
                        executes( (c) -> unsubFromAll(c.getSource(), c.getSource().getName())).
                        then(Commands.argument("player", StringArgumentType.word()).
                                suggests( (c, b)->ISuggestionProvider.suggest(c.getSource().getPlayerNames(),b)).
                                executes( (c) -> unsubFromAll(c.getSource(), StringArgumentType.getString(c, "player")))));

        literalargumentbuilder.then(Commands.argument("log name",StringArgumentType.word()).
                suggests( (c, b)-> ISuggestionProvider.suggest(LoggerRegistry.getLoggerNames(),b)).
                executes( (c)-> toggleSubscription(c.getSource(), c.getSource().getName(), StringArgumentType.getString(c, "log name"))).
                then(Commands.literal("clear").
                        executes( (c) -> unsubFromLogger(
                                c.getSource(),
                                c.getSource().getName(),
                                StringArgumentType.getString(c, "log name")))).
                then(Commands.argument("option", StringArgumentType.word()).
                        suggests( (c, b) -> ISuggestionProvider.suggest(
                                LoggerRegistry.getLogger(StringArgumentType.getString(c, "log name")).getOptions(),b)).
                        executes( (c) -> subscribePlayer(
                                c.getSource(),
                                c.getSource().getName(),
                                StringArgumentType.getString(c, "log name"),
                                StringArgumentType.getString(c, "option"))).
                        then(Commands.argument("player", StringArgumentType.word()).
                                suggests( (c, b) -> ISuggestionProvider.suggest(c.getSource().getPlayerNames(),b)).
                                executes( (c) -> subscribePlayer(
                                        c.getSource(),
                                        StringArgumentType.getString(c, "player"),
                                        StringArgumentType.getString(c, "log name"),
                                        StringArgumentType.getString(c, "option"))))));
        /*
        for (String logname: LoggerRegistry.getLoggerNames())
        {
            for (String option: LoggerRegistry.getLogger(logname).getOptions())
            {
                literalargumentbuilder.then(Commands.literal(logname).
                        then(Commands.literal(option).
                                executes( (c) -> subscribePlayer(c.getSource().getName(),logname, option ) ).
                                then(Commands.argument("player", StringArgumentType.word()).
                                        suggests( (c, b)->ISuggestionProvider.suggest(c.getSource().getPlayerNames(),b)).
                                        executes( (c) -> subscribePlayer(StringArgumentType.getString(c, "player"), logname, option) ))));
            }
        }
        */

        dispatcher.register(literalargumentbuilder);
    }
    private static int listLogs(CommandSource source)
    {
        EntityPlayer player;
        try
        {
            player = source.asPlayer();
        }
        catch (CommandSyntaxException e)
        {
            source.sendFeedback(Messenger.m(null, "For players only"), false);
            return 0;
        }
        Map<String,String> subs = LoggerRegistry.getPlayerSubscriptions(source.getName());
        if (subs == null)
        {
            subs = new HashMap<>();
        }
        List<String> all_logs = new ArrayList<>(LoggerRegistry.getLoggerNames());
        Collections.sort(all_logs);
        Messenger.m(player, "w _____________________");
        Messenger.m(player, "w Available logging options:");
        for (String lname: all_logs)
        {
            List<Object> comp = new ArrayList<>();
            String color = subs.containsKey(lname)?"w":"g";
            comp.add("w  - "+lname+": ");
            Logger logger = LoggerRegistry.getLogger(lname);
            String [] options = logger.getOptions();
            if (options.length == 0)
            {
                if (subs.containsKey(lname))
                {
                    comp.add("l Subscribed ");
                }
                else
                {
                    comp.add(color + " [Subscribe] ");
                    comp.add("^w subscribe to " + lname);
                    comp.add("!/log " + lname);
                }
            }
            else
            {
                for (String option : logger.getOptions())
                {
                    if (subs.containsKey(lname) && subs.get(lname).equalsIgnoreCase(option))
                    {
                        comp.add("l [" + option + "] ");
                    } else
                    {
                        comp.add(color + " [" + option + "] ");
                        comp.add("^w subscribe to " + lname + " " + option);
                        comp.add("!/log " + lname + " " + option);
                    }

                }
            }
            if (subs.containsKey(lname))
            {
                comp.add("nb [X]");
                comp.add("^w Click to unsubscribe");
                comp.add("!/log "+lname);
            }
            Messenger.m(player,comp.toArray(new Object[0]));
        }
        return 1;
    }
    private static int unsubFromAll(CommandSource source, String player_name)
    {
        EntityPlayer player = source.getServer().getPlayerList().getPlayerByUsername(player_name);
        if (player == null)
        {
            source.sendFeedback(Messenger.m(null, "r No player specified"), false);
            return 0;
        }
        for (String logname : LoggerRegistry.getLoggerNames())
        {
            LoggerRegistry.unsubscribePlayer(player_name, logname);
        }
        source.sendFeedback(Messenger.m(null, "gi Unsubscribed from all logs"), false);
        return 1;
    }
    private static int unsubFromLogger(CommandSource source, String player_name, String logname)
    {
        EntityPlayer player = source.getServer().getPlayerList().getPlayerByUsername(player_name);
        if (player == null)
        {
            source.sendFeedback(Messenger.m(null, "r No player specified"), false);
            return 0;
        }
        LoggerRegistry.unsubscribePlayer(player_name, logname);
        source.sendFeedback(Messenger.m(null, "gi Unsubscribed from "+logname), false);
        return 1;
    }

    private static int toggleSubscription(CommandSource source, String player_name, String logName)
    {
        EntityPlayer player = source.getServer().getPlayerList().getPlayerByUsername(player_name);
        if (player == null)
        {
            source.sendFeedback(Messenger.m(null, "r No player specified"), false);
            return 0;
        }
        boolean subscribed = LoggerRegistry.togglePlayerSubscription(player_name, logName);
        if (subscribed)
        {
            source.sendFeedback(Messenger.m(null, "gi "+player_name+" subscribed to " + logName + "."), false);
        }
        else
        {
            source.sendFeedback(Messenger.m(null, "gi "+player_name+" unsubscribed from " + logName + "."), false);
        }
        return 1;
    }
    private static int subscribePlayer(CommandSource source, String player_name, String logname, String option)
    {
        EntityPlayer player = source.getServer().getPlayerList().getPlayerByUsername(player_name);
        if (player == null)
        {
            source.sendFeedback(Messenger.m(null, "r No player specified"), false);
            return 0;
        }
        LoggerRegistry.subscribePlayer(player_name, logname, option);
        if (option!=null)
        {
            source.sendFeedback(Messenger.m(null, "gi Subscribed to " + logname + "(" + option + ")"), false);
        }
        else
        {
            source.sendFeedback(Messenger.m(null, "gi Subscribed to " + logname), false);
        }
            return 1;
    }
}
