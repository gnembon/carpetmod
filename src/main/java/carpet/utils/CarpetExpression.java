package carpet.utils;

import com.udojava.evalex.Expression;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;

import java.math.BigDecimal;

public class CarpetExpression
{
    private CommandSource source;
    private BlockPos origin;
    private Expression expr;
    public CarpetExpression(String expression, CommandSource source, BlockPos origin)
    {
        this.origin = origin;
        this.source = source;
        this.expr = new Expression(expression);

    }
    public long eval(BlockPos at)
    {
        return this.expr.
                with("x",new BigDecimal(origin.getX()-at.getX())).
                with("y",new BigDecimal(origin.getY()-at.getY())).
                with("z",new BigDecimal(origin.getZ()-at.getZ())).
                eval().longValue();
    }
    public long eval(int x, int y, int z)
    {
        return this.expr.
                with("x",new BigDecimal(origin.getX()-x)).
                with("y",new BigDecimal(origin.getY()-y)).
                with("z",new BigDecimal(origin.getZ()-z)).
                eval().longValue();
    }
}
