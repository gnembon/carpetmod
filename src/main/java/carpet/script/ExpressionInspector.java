package carpet.script;

import net.minecraft.command.CommandSource;

import java.util.List;
import java.util.Map;

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

    public static Expression Expression_globalFunctions_get_getExpression(String name)
    {
        return ScriptHost.globalHost.globalFunctions.get(name).getExpression();
    }

    public static Tokenizer.Token Expression_globalFunctions_get_getToken(String name)
    {
        return ScriptHost.globalHost.globalFunctions.get(name).getToken();
    }

    public static List<String> Expression_getExpressionSnippet(Tokenizer.Token token, String expr)
    {
        return Expression.getExpressionSnippet(token, expr);
    }

    public static void CarpetExpression_setChatErrorSnooper(CommandSource source)
    {
        CarpetExpression.setChatErrorSnooper(source);
    }

    public static void CarpetExpression_resetErrorSnooper()
    {
        CarpetExpression.resetErrorSnooper();
    }


    public static class CarpetExpressionException extends Expression.ExpressionException
    {
        CarpetExpressionException(String message)
        {
            super(message);
        }
    }
    public static void CarpetExpression_resetExpressionEngine() { CarpetExpression.resetExpressionEngine(); }
}
