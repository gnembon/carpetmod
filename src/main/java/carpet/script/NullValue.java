package carpet.script;

public class NullValue extends NumericValue
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
    public Value clone()
    {
        return new NullValue();
    }
    public NullValue() {super(0.0D);}
}
