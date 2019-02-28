package carpet.script;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class Context
{
    static final int NONE = 0;
    static final int VOID = 1;
    static final int BOOLEAN = 2;
    static final int NUMBER = 3;
    static final int STRING = 4;
    static final int LIST = 5;
    static final int ITERATOR = 6;
    static final int SIGNATURE = 7;

    private Map<String, LazyValue> variables = new HashMap<>();
    private Consumer<String> logOutput;

    public Consumer<String> getLogger()
    {
        return logOutput;
    }

    Context(Expression expr)
    {
        variables.putAll(expr.defaultVariables);
        logOutput = expr.getLogger();
    }

    LazyValue getVariable(String name)
    {
        if (variables.containsKey(name))
        {
            return variables.get(name);
        }
        return Expression.global_variables.get(name);
    }

    void setVariable(String name, LazyValue lv)
    {
        if (name.startsWith("global_"))
        {
            Expression.global_variables.put(name, lv);
            return;
        }
        variables.put(name, lv);
    }
    void setVariable(String name, Value val)
    {
        setVariable(name, (c, t) -> val.boundTo(name));
    }


    boolean isAVariable(String name)
    {
        return variables.containsKey(name) || Expression.global_variables.containsKey(name);
    }


    void delVariable(String variable)
    {
        if (variable.startsWith("global_"))
        {
            Expression.global_variables.remove(variable);
            return;
        }
        variables.remove(variable);
    }
    void clearAll(String variable)
    {
        if (variable.startsWith("global_"))
        {
            Expression.global_variables.remove(variable);
            return;
        }
        variables.remove(variable);
    }

    public Context with(String variable, LazyValue lv)
    {
        variables.put(variable, lv);
        return this;
    }

    public Set<String> getAllVariableNames()
    {
        return variables.keySet();
    }
}
