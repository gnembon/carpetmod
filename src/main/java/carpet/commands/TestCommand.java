package carpet.commands;

import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.ColorArgument;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.dimension.DimensionType;

public class TestCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> literalargumentbuilder = Commands.literal("test").
                executes(TestCommand::test).
                then(Commands.argument("color", ColorArgument.color()).
                        executes( (c)-> test_dim(c, ColorArgument.getColor(c, "color"))));

        dispatcher.register(literalargumentbuilder);
    }

    private static int test(CommandContext<CommandSource> c)
    {
        try
        {
            Messenger.m(c.getSource(),"w dimension is: ","wb "+c.getSource().asPlayer().dimension.toString());
        } catch (CommandSyntaxException e)
        {
            e.printStackTrace();
        }
        return 1;
    }
    private static int test_dim(CommandContext<CommandSource> c, TextFormatting dim)
    {
        Messenger.m(c.getSource(),"w dimension is: ","wb "+dim.name()+" "+dim.toString(), "w  ", "w "+dim.getFriendlyName());
        return 1;
    }
}
