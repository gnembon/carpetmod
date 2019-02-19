package carpet.script;

import java.util.Collections;

public abstract class LazyListValue extends ListValue
{
    int current_elements;

    public static LazyListValue range(int range_limit)
    {
        return new LazyListValue(0)
        {
            {
                this.limit = range_limit;
            }
            private int current;
            private int limit;
            @Override
            protected Value nextElement()
            {
                return new NumericValue(current++);
            }

            @Override
            public boolean hasNext()
            {
                return current < limit;
            }
        };
    }

    public LazyListValue(int precompute)
    {
        super(Collections.emptyList());
        this.current_elements = 0;
        while (precompute-- > 0)
        {
            next();
        }
        start();
    }

    @Override
    public String getString()
    {
        return null;
    }

    @Override
    public boolean getBoolean()
    {
        return items.isEmpty() && !hasNext();
    }
    protected abstract Value nextElement();
    public abstract boolean hasNext();

    public Value next()
    {
        if (items.size() < current_elements)
        {
            return items.get(current_elements++);
        }
        Value v = nextElement();
        items.add(v);
        current_elements++;
        return v;
    }


    public void start()
    {
        current_elements = 0;
    }


    @Override
    public Value clone()
    {
        LazyListValue el = (LazyListValue)super.clone();
        el.current_elements = this.current_elements;
        return el;
    }
}
