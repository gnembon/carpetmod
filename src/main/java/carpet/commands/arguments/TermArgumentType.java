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
    private final Set<String> options;

    private static final DynamicCommandExceptionType WRONG_TYPE_EXCEPTION = new DynamicCommandExceptionType(
            (term) -> Messenger.c("rb "+term.toString(), "r is not a valid here"));

    public String parse(StringReader reader) throws CommandSyntaxException
    {
        String term = reader.readUnquotedString();
        if (!options.contains(term)) throw WRONG_TYPE_EXCEPTION.create(term);
        return term;
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> c, SuggestionsBuilder b)
    {
        return ISuggestionProvider.suggest(options, b);
    }

    public Collection<String> getExamples() { return options; }

    private TermArgumentType(Collection<String> arrr) { options = new HashSet<>(arrr); }

    public static TermArgumentType term(String ... arrr) { return new TermArgumentType(Arrays.asList(arrr)); }

    public static String getTerm(CommandContext<CommandSource> c, String argName)
    {
        return c.getArgument(argName, String.class);
    }
}
