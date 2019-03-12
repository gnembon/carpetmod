package carpet.script;

import java.util.List;

public class FunctionSignatureValue extends Value
{
    private String identifier;
    private List<String> arguments;
    private List<String> locals;

    @Override
    public String getString()
    {
        throw new Expression.ExpressionException("Function "+identifier+" is not defined yet");
    }

    @Override
    public boolean getBoolean()
    {
        throw new Expression.ExpressionException("Function "+identifier+" is not defined yet");
    }

    @Override
    public Value clone()
    {
        throw new Expression.ExpressionException("Function "+identifier+" is not defined yet");
    }
    public FunctionSignatureValue(String name, List<String> args, List<String> locals)
    {
        this.identifier = name;
        this.arguments = args;
        this.locals = locals;
    }
    public String getName()
    {
        return identifier;
    }
    public List<String> getArgs()
    {
        return arguments;
    }
    public List<String> getLocals() {return locals;}


}
