package carpet.script;

import java.util.Collections;
import java.util.Iterator;

public abstract class LazyListValue extends ListValue implements Iterator<Value>
{
    protected boolean called;

    public static LazyListValue range(long range_limit)
    {
        return new LazyListValue()
        {
            {
                this.limit = range_limit;
            }
            private long current;
            private long limit;
            @Override
            public Value next()
            {
                called = true;
                return new NumericValue(current++);
            }

            @Override
            public boolean hasNext()
            {
                return current < limit;
            }
        };
    }

    public LazyListValue()
    {
        super(Collections.emptyList());
        called = false;
    }

    @Override
    public String getString()
    {
        return null;
    }

    @Override
    public boolean getBoolean()
    {
        return hasNext();
    }
    public abstract boolean hasNext();

    public abstract Value next();

    @Override
    public Iterator<Value> iterator() {return this;}
}
