package carpet.script;

import java.util.*;
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
    public static ListValue of(Value ... list)
    {
        return ListValue.wrap(Arrays.asList(list));
    }

    private ListValue()
    {
        items = null;
    }

    @Override
    public Value add(Value v) {
        if (v instanceof ListValue)
        {
            return new ListValue(Stream.concat(items.stream(), ((ListValue) v).items.stream())
                    .collect(Collectors.toList()));
        }
        ListValue ret = new ListValue(items);
        ret.append(v);
        return ret;
    }
    public void append(Value v)
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


    public static class ListConstructorValue extends ListValue
    {
        public ListConstructorValue(Collection<? extends Value> list)
        {
            super(list);
        }
    }
    public int length()
    {
        return items.size();
    }

    @Override
    public Value in(Value value1)
    {
        for (int i = 0; i < items.size(); i++)
        {
            Value v = items.get(i);
            if (v.equals(value1))
            {
                return new NumericValue(i);
            }
        }
        return Value.NULL;
    }

    @Override
    public Value slice(long from, long to)
    {
        List<Value> items = getItems();
        int size = items.size();
        if (to < 0 || to > size) to = size;
        if (from < 0 || from > size) from = size;
        if (from > to)
            return ListValue.of();
        return new ListValue(getItems().subList((int)from, (int) to));
    }

    @Override
    public double readNumber()
    {
        return (double)items.size();
    }



}
