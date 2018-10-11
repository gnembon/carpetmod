package carpet.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/*
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
}*/


public class TermArgumentType extends MyStringArgumentType
{
    private final Set<String> options;

    private TermArgumentType(final Collection<String> options) {
        super(StringType.TERM);
        this.options = new LinkedHashSet<>(options);
    }

    public static TermArgumentType term(Collection<String> options) {
        return new TermArgumentType(options);
    }

    public static TermArgumentType term(String ... options) {
        return term(Arrays.asList(options));
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        final String term = reader.readUnquotedString();
        if (!options.contains(term)) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidEscape().createWithContext(reader, term);
        }
        return term;
    }

    @Override
    public String toString() {
        return "term("+options+")";
    }

    @Override
    public Collection<String> getExamples() {
        return options;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        options.stream().filter((s) -> s.toLowerCase().startsWith(builder.getRemaining().toLowerCase())).sorted().forEachOrdered(builder::suggest);
        return builder.buildFuture();
    }
}
