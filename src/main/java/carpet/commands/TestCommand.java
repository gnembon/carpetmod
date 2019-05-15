package carpet.commands;

import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public class TestCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        /*final LiteralCommandNode<CommandSource> execute = dispatcher.register(literal("test"));
        dispatcher.register(
                literal("test")
                        .then(
                                literal("as")
                                        .then(
                                                argument("name", word())
                                                        .redirect(execute)
                                        )
                        )
                        .then(
                                literal("in")
                                        .then(
                                                argument("location", word())
                                                        .redirect(execute)
                                        )
                        )
                        .then(
                                literal("first")
                                        .executes((c)-> test(c, "called first"))
                        ).
                        then(
                                literal("second")
                                        .executes((c)-> test(c, "called second"))
                        )
                );
        */
        LiteralCommandNode<CommandSource> execute = dispatcher.register(literal("test")
                        .then(
                                literal("first")
                                        .executes((c)-> test(c, "called first"))
                        ).
                        then(
                                literal("second")
                                        .executes((c)-> test(c, "called second"))
                        )
        );
        dispatcher.register(literal("test").then(literal("in").then(argument("loc", word()).redirect(execute))));



        //LiteralCommandNode<CommandSource> literalCommandNode_1 = dispatcher.register(literal("test").
        //        then(literal("first").
        //                executes( (c)-> test(c, "called first"))).
        //        then(literal("second").
        //                executes( (c)-> test(c, "called second"))));
        //dispatcher.register(literal("test").then(literal("in").then(argument("context", word()).then(literalCommandNode_1.getChild("test")))));

    }

    private static int test(CommandContext<CommandSource> c, String term)
    {
        Messenger.m(c.getSource(),"w term is: ",term.substring(0,1)+"b "+term);
        return 1;
    }
}
