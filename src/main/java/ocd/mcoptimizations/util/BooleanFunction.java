package ocd.mcoptimizations.util;

import net.minecraft.util.math.shapes.IBooleanFunction;

public class BooleanFunction implements IBooleanFunction
{
    private static final IBooleanFunction[] funcs = new IBooleanFunction[16];

    static
    {
        for (int i = 0; i < 16; ++i)
            funcs[i] = new BooleanFunction(i);
    }

    public static final IBooleanFunction FALSE = getFunction(IBooleanFunction.FALSE);
    public static final IBooleanFunction NOT_OR = getFunction(IBooleanFunction.NOT_OR);
    public static final IBooleanFunction ONLY_SECOND = getFunction(IBooleanFunction.ONLY_SECOND);
    public static final IBooleanFunction NOT_FIRST = getFunction(IBooleanFunction.NOT_FIRST);
    public static final IBooleanFunction ONLY_FIRST = getFunction(IBooleanFunction.ONLY_FIRST);
    public static final IBooleanFunction NOT_SECOND = getFunction(IBooleanFunction.NOT_SECOND);
    public static final IBooleanFunction NOT_SAME = getFunction(IBooleanFunction.NOT_SAME);
    public static final IBooleanFunction NOT_AND = getFunction(IBooleanFunction.NOT_AND);
    public static final IBooleanFunction AND = getFunction(IBooleanFunction.AND);
    public static final IBooleanFunction SAME = getFunction(IBooleanFunction.SAME);
    public static final IBooleanFunction SECOND = getFunction(IBooleanFunction.SECOND);
    public static final IBooleanFunction CAUSES = getFunction(IBooleanFunction.CAUSES);
    public static final IBooleanFunction FIRST = getFunction(IBooleanFunction.FIRST);
    public static final IBooleanFunction CAUSED_BY = getFunction(IBooleanFunction.CAUSED_BY);
    public static final IBooleanFunction OR = getFunction(IBooleanFunction.OR);
    public static final IBooleanFunction TRUE = getFunction(IBooleanFunction.TRUE);

    private final int index;
    private final int swappedIndex;


    public BooleanFunction(final int index)
    {
        this.index = index;
        this.swappedIndex = (index & 9) | ((index & 4) >>> 1) | ((index & 2) << 1);
    }

    @Override
    public boolean apply(final boolean arg1, final boolean arg2)
    {
        return ((arg2 ? 10 : 5) & (arg1 ? 12 : 3) & this.index) != 0;
    }

    @Override
    public IBooleanFunction swapArgs()
    {
        return funcs[this.swappedIndex];
    }

    public static int getIndex(final IBooleanFunction func)
    {
        return (func.apply(false, false) ? 1 : 0) |
                (func.apply(false, true) ? 2 : 0) |
                (func.apply(true, false) ? 4 : 0) |
                (func.apply(true, true) ? 8 : 0);
    }

    public static int getSwappedIndex(final IBooleanFunction func)
    {
        return (func.apply(false, false) ? 1 : 0) |
                (func.apply(false, true) ? 4 : 0) |
                (func.apply(true, false) ? 2 : 0) |
                (func.apply(true, true) ? 8 : 0);
    }

    public static IBooleanFunction getFunction(final IBooleanFunction func)
    {
        return funcs[getIndex(func)];
    }

    public static IBooleanFunction getSwappedFunction(final IBooleanFunction func)
    {
        return funcs[getSwappedIndex(func)];
    }
}

