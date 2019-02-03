package com.udojava.evalex;

import carpet.CarpetSettings;

import java.math.BigDecimal;

public class Value
{
    public static Value FALSE = new Value(new BigDecimal(0));
    public static Value TRUE = new Value(new BigDecimal(1));
    public static Value ZERO = FALSE;
    public static Value EMPTY = new Value("");

    public Class type; // String, BigDecimal
    public Object value;
    public String boundVariable;

    public boolean isBound()
    {
        return boundVariable != null;
    }
    public String getVariable()
    {
        return boundVariable;
    }
    public Value boundedTo(String varname)
    {
        Value copy = new Value();
        copy.type = this.type;
        copy.value = this.value;
        copy.boundVariable = varname;
        return copy;
    }

    public String getString()
    {
        return this.getString(true);
    }
    public String getString(boolean force_cast)
    {
        if (type == String.class)
            return (String)value;
        if (force_cast)
            return value.toString();
        return null;
    }
    public BigDecimal getNumber()
    {
        if (type != BigDecimal.class)
        {
            return null;
        }
        return (BigDecimal)value;
    }

    public boolean getBoolean()
    {
        if (value == null)
        {
            return false;
        }
        if (type == BigDecimal.class)
        {
            return ((BigDecimal)value).compareTo(BigDecimal.ZERO) != 0;
        }
        return !getString().isEmpty();
    }

    public<T> Value(T val)
    {
        value = val;
        type = val.getClass();
    }
    private Value()
    {
        value = null;
        type = null;
    }

}
