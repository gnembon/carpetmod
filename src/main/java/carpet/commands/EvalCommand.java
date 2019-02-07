package carpet.commands;

import carpet.CarpetSettings;
import carpet.script.CarpetExpression;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public class EvalCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> command = literal("eval").
                requires((player) -> CarpetSettings.getBool("commandEval")).
                        then(argument("expr", StringArgumentType.string()).
                                executes((c) -> compute(
                                        c.getSource(),
                                        StringArgumentType.getString(c, "expr")
                                )));
        dispatcher.register(command);
    }
    private static int compute(CommandSource source, String expr)
    {
        BlockPos pos = new BlockPos(source.getPos());
        try
        {
            CarpetExpression ex = new CarpetExpression(expr, source, new BlockPos(0, 0, 0));
            ex.setLogOutput(true);
            Messenger.m(source, "wi "+expr,"wi  = ", "wb "+ex.eval(pos));
        }
        catch (CarpetExpression.CarpetExpressionException e)
        {
            Messenger.m(source, "r Exception white evaluating expression at "+pos+": "+e.getMessage());
        }
        return 1;
    }
}

