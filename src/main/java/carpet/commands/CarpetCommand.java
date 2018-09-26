package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CarpetCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> literalargumentbuilder = Commands.literal("carpet").requires((player) ->
                player.hasPermissionLevel(2) && !CarpetSettings.locked);
        for (CarpetSettings.CarpetSettingEntry rule: CarpetSettings.getAllCarpetSettings())
        {
            literalargumentbuilder.then(Commands.literal(rule.getName()).executes( (context) ->
                    displayRuleMenu(context.getSource(),rule)));
            literalargumentbuilder.then(Commands.literal("removeDefault").
                    then(Commands.literal(rule.getName()).executes((context) ->
                            removeDefault(context.getSource(), rule))));
            for (String possibleValue: rule.getOptions())
            {
                literalargumentbuilder.then(Commands.literal(rule.getName()).
                                then(Commands.literal(possibleValue).executes((context) ->
                                        setRule(context.getSource(), rule, possibleValue))));
                literalargumentbuilder.then(Commands.literal("setDefault").
                        then(Commands.literal(rule.getName()).then(Commands.literal(possibleValue).executes((context) ->
                                setDefault(context.getSource(), rule, possibleValue)))));
            }
        }
        dispatcher.register(literalargumentbuilder);
    }

    private static int displayRuleMenu(CommandSource source, CarpetSettings.CarpetSettingEntry rule)
    {
        EntityPlayer player;
        try
        {
            player = source.asPlayer();
        }
        catch (CommandSyntaxException e)
        {
            source.sendFeedback(Messenger.m(null, "w "+rule.getName() +" is set to: ","wb "+rule.getStringValue()), false);
            return 1;
        }

        Messenger.s(player, "");
        Messenger.m(player, "wb "+rule.getName(),"!/carpet "+rule.getName(),"^g refresh");
        Messenger.s(player, rule.getToast());

        Arrays.stream(rule.getInfo()).forEach(s -> Messenger.s(player, " "+s,"g"));

        List<ITextComponent> tags = new ArrayList<>();
        tags.add(Messenger.m(null, "w Tags: "));
        for (String t: rule.getTags())
        {
            tags.add(Messenger.m(null, "c ["+t+"]", "^g list all "+t+" settings","!/carpet list "+t));
            tags.add(Messenger.s(null, ", "));
        }
        tags.remove(tags.size()-1);
        Messenger.m(player, tags.toArray(new Object[0]));

        Messenger.m(player, "w Current value: ",String.format("%s %s (%s value)",rule.getBoolValue()?"lb":"nb", rule.getStringValue(),rule.isDefault()?"default":"modified"));
        List<ITextComponent> options = new ArrayList<>();
        options.add(Messenger.m(null, "w Options: ", "y [ "));
        for (String o: rule.getOptions())
        {
            options.add(Messenger.m(null,
                    String.format("%s%s %s",(o.equals(rule.getDefault()))?"u":"", (o.equals(rule.getStringValue()))?"bl":"y", o ),
                    "^g switch to "+o,
                    String.format("?/carpet %s %s",rule.getName(),o)));
            options.add(Messenger.s(null, " "));
        }
        options.remove(options.size()-1);
        options.add(Messenger.m(null, "y  ]"));
        Messenger.m(player, options.toArray(new Object[0]));

        return 1;
    }

    private static int setRule(CommandSource source, CarpetSettings.CarpetSettingEntry rule, String newValue)
    {
        CarpetSettings.set(rule.getName(), newValue);
        source.sendFeedback(Messenger.m(null, "w "+rule.toString()+", ", "c [change permanently?]",
                "^w Click to keep the settings in carpet.conf to save across restarts",
                "?/carpet setDefault "+rule.getName()+" "+rule.getStringValue()), false);
        return 1;
    }
    private static int setDefault(CommandSource source, CarpetSettings.CarpetSettingEntry rule, String defaultValue)
    {
        CarpetSettings.setDefaultRule(source.getServer(), rule.getName(), defaultValue);
        source.sendFeedback(Messenger.m(null ,"gi rule "+ rule.getName()+" will now default to "+ defaultValue), false);
        return 1;
    }
    private static int removeDefault(CommandSource source, CarpetSettings.CarpetSettingEntry rule)
    {
        CarpetSettings.removeDefaultRule(source.getServer(), rule.getName());
        source.sendFeedback(Messenger.m(null ,"gi rule "+ rule.getName()+" defaults to Vanilla"), false);
        return 1;
    }
}
