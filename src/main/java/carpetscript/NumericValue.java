package carpetscript;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

public class NumericValue extends Value{
    private BigDecimal value;

    @Override
    public String getString() {
        return value.stripTrailingZeros().toString();
    }

    @Override
    public boolean getBoolean() {
        return value != null && value.compareTo(BigDecimal.ZERO) != 0;
    }

    @Override
    public Value add(Value v) {  // TODO test if definintn add(NumericVlaue) woud solve the casting
        if (v instanceof NumericValue)
        {
            return new NumericValue(value.add(((NumericValue) v).getNumber()));
        }
        return super.add(v);
    }
    public Value subtract(Value v) {  // TODO test if definintn add(NumericVlaue) woud solve the casting
        if (v instanceof NumericValue)
        {
            return new NumericValue(value.subtract(((NumericValue) v).getNumber()));
        }
        return super.subtract(v);
    }
    public Value multiply(Value v)
    {
        if (v instanceof NumericValue)
        {
            return new NumericValue(value.multiply(((NumericValue) v).getNumber()));
        }
        return new StringValue(StringUtils.repeat(v.getString(), value.intValue()));
    }
    public Value divide(Value v)
    {
        if (v instanceof NumericValue)
        {
            return new NumericValue(value.divide(((NumericValue) v).getNumber()));
        }
        return super.divide(v);
    }



    @Override
    public Value copy()
    {
        return new NumericValue(value);
    }

    @Override
    public int compareTo(Value o)
    {
        if (o instanceof NumericValue)
        {
            return value.compareTo(((NumericValue) o).getNumber());
        }
        return super.compareTo(o);
    }

    public NumericValue(BigDecimal value)
    {
        this.value = value;
    }
    public NumericValue(double value)
    {
        this(new BigDecimal(value));
    }
    public NumericValue(String value)
    {
        this(new BigDecimal(value));
    }
    public NumericValue(long value)
    {
        this(new BigDecimal(value));
    }

    public BigDecimal getNumber()
    {
        return value;
    }

}
