package carpet.commands;

import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

public class TestCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> literalargumentbuilder = Commands.literal("test").
                executes(TestCommand::test);

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
}
