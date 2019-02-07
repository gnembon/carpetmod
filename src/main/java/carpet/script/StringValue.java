package carpet.script;

public class StringValue extends Value
{
    private String str;

    @Override
    public String getString() {
        return str;
    }

    @Override
    public boolean getBoolean() {
        return str != null && !str.isEmpty();
    }

    @Override
    public Value copy()
    {
        return new StringValue(str);
    }
    public StringValue(String str)
    {
        this.str = str;
    }

}
