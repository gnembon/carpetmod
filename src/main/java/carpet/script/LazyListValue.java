package carpet.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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

    public List<Value> unroll()
    {
        List<Value> result = new ArrayList<>();
        this.forEachRemaining(result::add);
        return result;
    }

    @Override
    public Value slice(long from, long to)
    {
        if (to < 0) to = Integer.MAX_VALUE;
        if (from < 0) from = 0;
        if (from > to)
            return ListValue.of();
        List<Value> result = new ArrayList<>();
        int i;
        for (i = 0; i < from; i++)
        {
            if (hasNext())
                next();
            else
                return ListValue.wrap(result);
        }
        for (i = (int)from; i < to; i++)
        {
            if (hasNext())
                result.add(next());
            else
                return ListValue.wrap(result);
        }
        return ListValue.wrap(result);
    }
}
