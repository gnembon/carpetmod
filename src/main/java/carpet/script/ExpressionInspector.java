package carpet.script;

import java.util.List;

/**
 * sole purpose of this package is to provide public access to package-private methods of Expression and CarpetExpression
 * classes so they don't leave garbage in javadocs
 */
public class ExpressionInspector
{
    public static String Expression_getCodeString(Expression e)
    {
        return e.getCodeString();
    }

    public static List<String> Expression_getExpressionSnippet(Tokenizer.Token token, String expr)
    {
        return Expression.getExpressionSnippet(token, expr);
    }

    public static class CarpetExpressionException extends Expression.ExpressionException
    {
        CarpetExpressionException(String message)
        {
            super(message);
        }
    }
}
