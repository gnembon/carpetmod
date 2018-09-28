package carpet.commands;

import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;

import static carpet.commands.arguments.TermArgumentType.getTerm;
import static carpet.commands.arguments.TermArgumentType.term;
import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public class TestCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(literal("test").
                then(argument("color", term("red", "lime", "gray")).
                        executes( (c)-> test_dim(c, getTerm(c, "color")+" 1"))).
                then(argument("other", term("white", "lime", "magenta")).
                        executes( (c)-> test_dim(c, getTerm(c, "other")+" 2"))));
    }

    private static int test_dim(CommandContext<CommandSource> c, String term)
    {
        Messenger.m(c.getSource(),"w term is: ",term.substring(0,1)+"b "+term);
        return 1;
    }
}
