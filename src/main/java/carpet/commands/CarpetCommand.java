package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CarpetCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> literalargumentbuilder = Commands.literal("carpet").requires((player) ->
                player.hasPermissionLevel(2) && !CarpetSettings.locked);

        literalargumentbuilder.executes((context)->listAllSettings(context.getSource())).
                then(Commands.literal("list").
                        executes( (c) -> listSettings(c.getSource(),
                                "All CarpetMod Settings",
                                CarpetSettings.findAll(null))).
                        then(Commands.literal("defaults").
                                executes( (c)-> listSettings(c.getSource(),
                                        "Current CarpetMod Startup Settings from carpet.conf",
                                        CarpetSettings.findStartupOverrides(c.getSource().getServer())))).
                        then(Commands.argument("tag",StringArgumentType.word()).
                                suggests( (c, b)->ISuggestionProvider.suggest(Arrays.stream(CarpetSettings.RuleCategory.values()).map(v -> v.name().toLowerCase(Locale.ENGLISH)), b)).
                                executes( (c) -> listSettings(c.getSource(),
                                        String.format("CarpetMod Settings matching \"%s\"", StringArgumentType.getString(c, "tag")),
                                        CarpetSettings.findAll(StringArgumentType.getString(c, "tag"))))));

        for (String ruleName: CarpetSettings.findAll(null))
        {
            literalargumentbuilder.then(Commands.literal(ruleName).executes( (context) ->
                    displayRuleMenu(context.getSource(), ruleName)));

            literalargumentbuilder.then(Commands.literal("removeDefault").
                    then(Commands.literal(ruleName).executes((context) ->
                            removeDefault(context.getSource(), ruleName))));

            literalargumentbuilder.then(Commands.literal(ruleName).
                    then(Commands.argument("value", StringArgumentType.word()).
                            suggests((c, b)-> ISuggestionProvider.suggest(CarpetSettings.getOptions(ruleName), b)).
                            executes((context) ->
                            setRule(context.getSource(), ruleName, StringArgumentType.getString(context, "value")))));
            
            literalargumentbuilder.then(Commands.literal("setDefault").
                    then(Commands.literal(ruleName).
                    then(Commands.argument("value", StringArgumentType.word()).
                            suggests((c, b)-> ISuggestionProvider.suggest(CarpetSettings.getOptions(ruleName),b)).
                            executes((context) ->
                            setDefault(context.getSource(), ruleName, StringArgumentType.getString(context, "value"))))));
        }
        dispatcher.register(literalargumentbuilder);
    }

    private static int displayRuleMenu(CommandSource source, String ruleName)
    {
        EntityPlayer player;
        try
        {
            player = source.asPlayer();
        }
        catch (CommandSyntaxException e)
        {
            Messenger.m(source, "w "+ruleName +" is set to: ","wb "+CarpetSettings.get(ruleName));
            return 1;
        }

        Messenger.m(player, "");
        Messenger.m(player, "wb "+ruleName,"!/carpet "+ruleName,"^g refresh");
        Messenger.m(player, "w "+CarpetSettings.getDescription(ruleName));

        Arrays.stream(CarpetSettings.getExtraInfo(ruleName)).forEach(s -> Messenger.m(player, "g  "+s));

        List<ITextComponent> tags = new ArrayList<>();
        tags.add(Messenger.c("w Tags: "));
        for (CarpetSettings.RuleCategory ctgy: CarpetSettings.getCategories(ruleName))
        {
            String t = ctgy.name().toLowerCase(Locale.ENGLISH);
            tags.add(Messenger.c("c ["+t+"]", "^g list all "+t+" settings","!/carpet list "+t));
            tags.add(Messenger.c("w , "));
        }
        tags.remove(tags.size()-1);
        Messenger.m(player, tags.toArray(new Object[0]));

        Messenger.m(player, "w Current value: ",
                String.format("%s %s (%s value)",
                        CarpetSettings.get(ruleName).equalsIgnoreCase("true")?"lb":"nb",
                        CarpetSettings.get(ruleName),
                        CarpetSettings.get(ruleName).equalsIgnoreCase(CarpetSettings.getDefault(ruleName))?"default":"modified"
                )
        );
        List<ITextComponent> options = new ArrayList<>();
        options.add(Messenger.c("w Options: ", "y [ "));
        for (String o: CarpetSettings.getOptions(ruleName))
        {
            options.add(Messenger.c(
                    String.format("%s%s %s",(o.equals(CarpetSettings.getDefault(ruleName)))?"u":"", (o.equals(CarpetSettings.get(ruleName)))?"bl":"y", o ),
                    "^g switch to "+o,
                    String.format("?/carpet %s %s",ruleName,o)));
            options.add(Messenger.c("w  "));
        }
        options.remove(options.size()-1);
        options.add(Messenger.c("y  ]"));
        Messenger.m(player, options.toArray(new Object[0]));

        return 1;
    }

    private static int setRule(CommandSource source, String ruleName, String newValue)
    {
        CarpetSettings.set(ruleName, newValue);
        Messenger.m(source, "w "+ruleName+", ", "c [change permanently?]",
                "^w Click to keep the settings in carpet.conf to save across restarts",
                "?/carpet setDefault "+ruleName+" "+CarpetSettings.get(ruleName));
        return 1;
    }
    private static int setDefault(CommandSource source, String ruleName, String defaultValue)
    {
        CarpetSettings.addOrSetPermarule(source.getServer(), ruleName, defaultValue);
        Messenger.m(source ,"gi rule "+ruleName+" will now default to "+ defaultValue);
        return 1;
    }
    private static int removeDefault(CommandSource source, String ruleName)
    {
        CarpetSettings.removeDefaultRule(source.getServer(), ruleName);
        Messenger.m(source ,"gi rule "+ruleName+" defaults to Vanilla");
        return 1;
    }


    private static ITextComponent displayInteractiveSetting(String ruleName)
    {
        String def = CarpetSettings.getDefault(ruleName);
        String val = CarpetSettings.get(ruleName);

        List<Object> args = new ArrayList<>();
        args.add("w - "+ruleName+" ");
        args.add("!/carpet "+ruleName);
        args.add("^y "+CarpetSettings.getDescription(ruleName));
        for (String option: CarpetSettings.getOptions(ruleName))
        {
            String style = val.equalsIgnoreCase(def)?"g":(option.equalsIgnoreCase(def)?"y":"e");
            if (option.equalsIgnoreCase(def))
                style = style+"b";
            else if (option.equalsIgnoreCase(val))
                style = style+"u";
            args.add(style+" ["+option+"]");
            if (!option.equalsIgnoreCase(val))
            {
                args.add("!/carpet " + ruleName + " " + option);
                args.add("^w switch to " + option);
            }
            args.add("w  ");
        }
        args.remove(args.size()-1);
        return Messenger.c(args.toArray(new Object[0]));
    }

    private static int listSettings(CommandSource source, String title, String[] settings_list)
    {
        try
        {
            EntityPlayer player = source.asPlayer();
            Messenger.m(player,String.format("wb %s:",title));
            Arrays.stream(settings_list).forEach(e -> Messenger.m(player,displayInteractiveSetting(e)));

        }
        catch (CommandSyntaxException e)
        {
            Messenger.m(source, "w s:"+title);
            Arrays.stream(settings_list).forEach(r -> Messenger.m(source, "w  - "+ r.toString()));
        }
        return 1;
    }
    private static int listAllSettings(CommandSource source)
    {
        listSettings(source, "Current CarpetMod Settings", CarpetSettings.findNonDefault());

        Messenger.m(source, "Carpet Mod version: "+CarpetSettings.carpetVersion);
        try
        {
            EntityPlayer player = source.asPlayer();
            List<Object> tags = new ArrayList<>();
            tags.add("w Browse Categories:\n");
            for (CarpetSettings.RuleCategory ctgy : CarpetSettings.RuleCategory.values())
            {
                String t = ctgy.name().toLowerCase(Locale.ENGLISH);
                tags.add("c [" + t+"]");
                tags.add("^g list all " + t + " settings");
                tags.add("!/carpet list " + t);
                tags.add("w  ");
            }
            tags.remove(tags.size() - 1);
            Messenger.m(player, tags.toArray(new Object[0]));
        }
        catch (CommandSyntaxException e)
        {
        }
        return 1;
    }
}
