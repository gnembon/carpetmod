package carpet.script;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Value implements Comparable<Value>, Cloneable
{
    public static Value FALSE = new NumericValue(0);
    public static Value TRUE = new NumericValue(1);
    public static Value ZERO = FALSE;
    public static Value NULL = new NullValue();

    public String boundVariable;

    public boolean isBound()
    {
        return boundVariable != null;
    }
    public String getVariable()
    {
        return boundVariable;
    }
    public Value boundTo(String var)
    {
        Value copy = null;
        try
        {
            copy = (Value)clone();
        }
        catch (CloneNotSupportedException e)
        {
            // should not happen
            e.printStackTrace();
        }
        copy.boundVariable = var;
        return copy;
    }

    public abstract String getString();

    public abstract boolean getBoolean();

    public Value add(Value v) {
        String lstr = this.getString();
        if (lstr == null) // null should not happen
            return new StringValue(v.getString());
        String rstr = v.getString();
        if (rstr == null)
        {
            return new StringValue(lstr);
        }
        return new StringValue(lstr+rstr);
    }
    public Value subtract(Value v)
    {
        return new StringValue(this.getString().replace(v.getString(),""));
    }
    public Value multiply(Value v)
    {
        return new StringValue(this.getString()+"."+v.getString());
    }
    public Value divide(Value v)
    {
        if (v instanceof NumericValue)
        {
            String lstr = getString();
            return new StringValue(lstr.substring(0, (int)(lstr.length()/ ((NumericValue) v).getNumber().floatValue())));
        }
        return new StringValue(getString()+"/"+v.getString());
    }

    public Value(Value other)
    {
        this();
    }
    public Value()
    {
        this.boundVariable = null;
    }

    @Override
    public int compareTo(final Value o)
    {
        String lstr = getString();
        String rstr = o.getString();
        if (lstr == null)
        {
            if (rstr == null)
                return 0;
            return 1;
        }
        if (rstr == null)
            return -1;
        return lstr.compareTo(rstr);
    }

    public void assertAssignable()
    {
        if (boundVariable == null || boundVariable.startsWith("_"))
        {
            if (boundVariable != null)
            {
                throw new Expression.ExpressionException(boundVariable+ " cannot be assigned a new value");
            }
            throw new Expression.ExpressionException(getString()+ "is not a variable");
        }

    }

    public Value in(Value value1)
    {
        final Pattern p = Pattern.compile(value1.getString());
        final Matcher m = p.matcher(this.getString());
        boolean matches = m.find();
        return matches?Value.TRUE:Value.FALSE;
    }
}
