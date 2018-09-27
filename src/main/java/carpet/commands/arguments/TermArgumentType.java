package carpet.commands.arguments;

import carpet.utils.Messenger;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class TermArgumentType implements ArgumentType<String>
{
    private static final Set<String> options;
    public static final DynamicCommandExceptionType WRONG_TYPE_EXCEPTION = new DynamicCommandExceptionType((name) ->
    {
        return Messenger.c("rb "+name.toString(), "r is not a valid here");
    });

    public <S> String parse(StringReader p_parse_1_) throws CommandSyntaxException
    {
        String type = p_parse_1_.readUnquotedString();

        if (!options.contains(type))
        {
            throw WRONG_TYPE_EXCEPTION.create(type);
        }
        else
        {
            return type;
        }
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> c, SuggestionsBuilder b)
    {
        return ISuggestionProvider.suggest(options, b);
    }

    public Collection<String> getExamples()
    {
        return options;
    }
    private TermArgumentType(Collection<String> arrr)
    {
        options = new HashSet(arrr);
    }

    public static TermArgumentType term(String [] arrr)
    {
        return new TermArgumentType(Arrays.asList(arrr));
    }

    public static String getType(CommandContext<CommandSource> c, String argName)
    {
        return c.getArgument(argName, String.class);
    }
}
