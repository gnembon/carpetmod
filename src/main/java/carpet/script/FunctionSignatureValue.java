package carpet.script;

import java.util.List;

public class FunctionSignatureValue extends Value
{
    private String identifier;
    private List<String> arguments;

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
    public Value copy()
    {
        throw new Expression.ExpressionException("Function "+identifier+" is not defined yet");
    }
    public FunctionSignatureValue(String name, List<String> args)
    {
        this.identifier = name;
        this.arguments = args;
    }
    public String getName()
    {
        return identifier;
    }
    public List<String> getArgs()
    {
        return arguments;
    }


}
