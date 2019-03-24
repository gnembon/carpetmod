package carpet.script;

public class NullValue extends NumericValue
{
    private static NullValue singleton = new NullValue();
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
        return singleton;
    }
    private NullValue() {super(0.0D);}
    public static NullValue getInstance() {return singleton;}
    @Override
    public boolean equals(final Value o)
    {
        return this == o;
    }

    @Override
    public int compareTo(Value o)
    {
        return this == o ? 0 : -1;
    }
}
