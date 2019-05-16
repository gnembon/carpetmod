package carpet.commands;

import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;

import java.util.Arrays;
import java.util.Collections;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public class TestCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        /*LiteralCommandNode<CommandSource> execute = dispatcher.register(literal("test"));
        dispatcher.register(literal("test").
                then(literal("as").
                        then(argument("name", word()).
                                fork(execute, (c) -> Collections.singletonList(c.getSource())))).
                then(literal("in").
                        then(argument("location", word()).
                                fork(execute, (c) -> Collections.singletonList(c.getSource())))).
                then(literal("first").
                        executes((c)-> test(c, "l called first"))).
                then(literal("second").
                        executes((c)-> test(c, "l called second"))));*/
        LiteralArgumentBuilder<CommandSource> a = literal("first").
                executes((c)-> test(c, "l called first"));
        LiteralArgumentBuilder<CommandSource> b = literal("second").
                executes((c)-> test(c, "l called second"));
        dispatcher.register(literal("test").then(a).then(b));
        dispatcher.register(literal("test").then(literal("as").then(argument("name", word()).
                then(a).then(b))));
    }
    private static int test(CommandContext<CommandSource> c, String term)
    {
        Messenger.m(c.getSource(),"w term is: ",term.substring(0,1)+"b "+term+" arg is "+StringArgumentType.getString(c, "name"));
        return 1;
    }
}