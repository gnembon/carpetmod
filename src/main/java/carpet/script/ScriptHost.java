package carpet.script;

import java.util.HashMap;
import java.util.Map;

public class ScriptHost
{

    public final Map<String, Expression.UserDefinedFunction> globalFunctions = new HashMap<>();

    public final Map<String, LazyValue> globalVariables = new HashMap<>();

    public ScriptHost()
    {
        globalVariables.put("euler", (c, t) -> Expression.euler);
        globalVariables.put("pi", (c, t) -> Expression.PI);
        globalVariables.put("null", (c, t) -> Value.NULL);
        globalVariables.put("true", (c, t) -> Value.TRUE);
        globalVariables.put("false", (c, t) -> Value.FALSE);

        //special variables for second order functions so we don't need to check them all the time
        globalVariables.put("_", (c, t) -> Value.ZERO);
        globalVariables.put("_i", (c, t) -> Value.ZERO);
        globalVariables.put("_a", (c, t) -> Value.ZERO);
    }
    public Expression getExpressionForFunction(String name)
    {
        return globalFunctions.get(name).getExpression();
    }
    public Tokenizer.Token getTokenForFunction(String name)
    {
        return globalFunctions.get(name).getToken();
    }
}
