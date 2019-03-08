package carpet.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListValue extends Value
{
    protected List<Value> items;
    @Override
    public String getString()
    {
        return "["+items.stream().map(Value::getString).collect(Collectors.joining(", "))+"]";
    }

    @Override
    public boolean getBoolean() {
        return !items.isEmpty();
    }

    @Override
    public Value clone()
    {
        return new ListValue(items);
    }
    public ListValue(Collection<? extends Value> list)
    {
        items = new ArrayList<>();
        items.addAll(list);
    }
    public static ListValue wrap(List<Value> list)
    {
        ListValue created = new ListValue();
        created.items = list;
        return created;
    }

    private ListValue()
    {
        items = null;
    }

    public ListValue(int to)
    {
        this(1, to);
    }
    public ListValue(int from, int to)
    {
        items = new ArrayList<>();
        for (int i = from; i <= to; i++)
        {
            items.add(new NumericValue(i));
        }
    }

    @Override
    public Value add(Value v) {
        if (v instanceof ListValue)
        {
            return new ListValue(Stream.concat(items.stream(), ((ListValue) v).items.stream())
                    .collect(Collectors.toList()));
        }
        ListValue ret = new ListValue(items);
        ret.addTo(v);
        return ret;
    }
    public void addTo(Value v)
    {
        items.add(v);

    }
    public Value subtract(Value v)
    {
        throw new UnsupportedOperationException(); // TODO
    }
    public void subtractFrom(Value v)
    {
        throw new UnsupportedOperationException(); // TODO
    }


    public Value multiply(Value v)
    {
        throw new UnsupportedOperationException(); // TODO
    }
    public Value divide(Value v)
    {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public int compareTo(Value o)
    {
        throw new UnsupportedOperationException(); // TODO
    }

    public List<Value> getItems()
    {
        return items;
    }

    public Iterator<Value> iterator()
    {
        return items.iterator();
    }
    public void fatality()
    {
    }







}
