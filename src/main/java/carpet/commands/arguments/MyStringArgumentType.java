package carpet.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Arrays;
import java.util.Collection;

public class MyStringArgumentType implements ArgumentType<String>
{
    private final MyStringArgumentType.StringType type;

    MyStringArgumentType(final MyStringArgumentType.StringType type) {
        this.type = type;
    }

    public static MyStringArgumentType word() {
        return new MyStringArgumentType(MyStringArgumentType.StringType.SINGLE_WORD);
    }

    public static MyStringArgumentType string() {
        return new MyStringArgumentType(MyStringArgumentType.StringType.QUOTABLE_PHRASE);
    }

    public static MyStringArgumentType greedyString() {
        return new MyStringArgumentType(MyStringArgumentType.StringType.GREEDY_PHRASE);
    }

    public static String getString(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    public MyStringArgumentType.StringType getType() {
        return type;
    }

    @Override
    public <S> String parse(final StringReader reader) throws CommandSyntaxException
    {
        if (type == MyStringArgumentType.StringType.GREEDY_PHRASE) {
            final String text = reader.getRemaining();
            reader.setCursor(reader.getTotalLength());
            return text;
        } else if (type == MyStringArgumentType.StringType.SINGLE_WORD) {
            return reader.readUnquotedString();
        } else {
            return reader.readString();
        }
    }

    @Override
    public String toString() {
        return "string()";
    }

    @Override
    public Collection<String> getExamples() {
        return type.getExamples();
    }

    public static String escapeIfRequired(final String input) {
        for (final char c : input.toCharArray()) {
            if (!StringReader.isAllowedInUnquotedString(c)) {
                return escape(input);
            }
        }
        return input;
    }

    private static String escape(final String input) {
        final StringBuilder result = new StringBuilder("\"");

        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c == '\\' || c == '"') {
                result.append('\\');
            }
            result.append(c);
        }

        result.append("\"");
        return result.toString();
    }

    public enum StringType {
        SINGLE_WORD("word", "words_with_underscores"),
        QUOTABLE_PHRASE("\"quoted phrase\"", "word", "\"\""),
        GREEDY_PHRASE("word", "words with spaces", "\"and symbols\""),
        TERM("red","green","blue","tokens"),;

        private final Collection<String> examples;

        StringType(final String... examples) {
            this.examples = Arrays.asList(examples);
        }

        public Collection<String> getExamples() {
            return examples;
        }
    }
}