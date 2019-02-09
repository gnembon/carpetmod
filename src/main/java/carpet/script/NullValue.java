package carpet.script;

public class NullValue extends Value
{
    @Override
    public String getString()
    {
        return "";
    }

    @Override
    public boolean getBoolean()
    {
        return false;
    }

    @Override
    public Value copy()
    {
        return this; // there is only one
    }
    public NullValue()
    {}
}
