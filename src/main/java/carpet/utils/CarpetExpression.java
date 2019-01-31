package carpet.utils;

import com.udojava.evalex.AbstractFunction;
import com.udojava.evalex.AbstractLazyFunction;
import com.udojava.evalex.Expression;
import com.udojava.evalex.Expression.ExpressionException;
import com.udojava.evalex.Expression.LazyNumber;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.init.Blocks;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.IRegistry;

import java.math.BigDecimal;
import java.util.List;

public class CarpetExpression
{
    private CommandSource source;
    private BlockPos origin;
    private Expression expr;
    private static final LazyNumber TRUE = Expression.CreateLazyNumber(BigDecimal.ONE);
    private static final LazyNumber FALSE = Expression.CreateLazyNumber(BigDecimal.ZERO);

    public static class BlockValue implements LazyNumber
    {
        public static final BlockValue AIR = new BlockValue(Blocks.AIR.getDefaultState());
        public IBlockState blockState;
        public BlockValue(IBlockState arg)
        {
            blockState = arg;
        }

        @Override
        public BigDecimal eval() {
            if(blockState.getBlock() == Blocks.AIR)
                return BigDecimal.ZERO;
            return BigDecimal.valueOf(blockState.getBlock().hashCode());
        }

        @Override
        public String getString() {
            return IRegistry.field_212618_g.getKey(blockState.getBlock()).getPath();
        }
    }
    public static class StringValue implements LazyNumber
    {
        public static final StringValue EMPTY = new StringValue("");
        public String str;
        public StringValue(String arg)
        {
            str = arg;
        }

        @Override
        public BigDecimal eval() {
            return BigDecimal.valueOf(str.hashCode());
        }

        @Override
        public String getString() {
            return str;
        }
    }


    private BlockPos locateBlockPos(List<LazyNumber> params)
    {
        if (params.size() != 3)
        {
            throw new ExpressionException("Need three integers for params");
        }
        int xpos = params.get(0).eval().intValue();
        int ypos = params.get(1).eval().intValue();
        int zpos = params.get(2).eval().intValue();
        /*
        Try without it first
        if (ypos < -1000255 || ypos > 1000255 || xpos > 10000 || xpos < -10000 || zpos > 10000 || zpos< -10000)
        {
            throw new ExpressionException("Attempting to locate block outside of 10k blocks range");
        }
        */
        return new BlockPos(origin.getX()+xpos, origin.getY()+ypos, origin.getZ()+zpos );
    }

    private BlockPos locateBlockPosNum(List<BigDecimal> params)
    {
        if (params.size() != 3)
        {
            throw new ExpressionException("Need three integers for params");
        }
        int xpos = params.get(0).intValue();
        int ypos = params.get(1).intValue();
        int zpos = params.get(2).intValue();
        /*
        Try without it first
        if (ypos < -1000255 || ypos > 1000255 || xpos > 10000 || xpos < -10000 || zpos > 10000 || zpos< -10000)
        {
            throw new ExpressionException("Attempting to locate block outside of 10k blocks range");
        }
        */
        return new BlockPos(origin.getX()+xpos, origin.getY()+ypos, origin.getZ()+zpos );
    }

    public CarpetExpression(String expression, CommandSource source, BlockPos origin)
    {
        this.origin = origin;
        this.source = source;
        this.expr = new Expression(expression);
        this.expr.addLazyFunction(new AbstractLazyFunction("block", -1)
        {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                if (lazyParams.size() == 0)
                {
                    throw new ExpressionException("block requires at least one parameter");
                }
                if (lazyParams.size() == 1)
                {
                    return new BlockValue(IRegistry.field_212618_g.get(new ResourceLocation(lazyParams.get(0).getString())).getDefaultState());
                }
                BlockPos pos = locateBlockPos(lazyParams);
                return new BlockValue(source.getWorld().getBlockState(pos));
            }
        });
        this.expr.addLazyFunction(new AbstractLazyFunction("isSolid", -1, true) {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                if (lazyParams.size() == 0)
                {
                    throw new ExpressionException("isSolid requires at least one parameter");
                }
                if (lazyParams.get(0) instanceof BlockValue)
                    return ((BlockValue)lazyParams.get(0)).blockState.isSolid()?TRUE:FALSE;
                BlockPos pos = locateBlockPos(lazyParams);
                return source.getWorld().getBlockState(pos).isSolid()?TRUE:FALSE;
            }
        });
        this.expr.addLazyFunction(new AbstractLazyFunction("isLiquid", -1, true) {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                if (lazyParams.size() == 0)
                {
                    throw new ExpressionException("isLiquid requires at least one parameter");
                }
                if (lazyParams.get(0) instanceof BlockValue)
                    return ((BlockValue)lazyParams.get(0)).blockState.getFluidState().isEmpty()?FALSE:TRUE;
                BlockPos pos = locateBlockPos(lazyParams);
                return source.getWorld().getBlockState(pos).getFluidState().isEmpty()?FALSE:TRUE;
            }
        });
        this.expr.addFunction(new AbstractFunction("blockLight", 3, false) {
            @Override
            public BigDecimal eval(List<BigDecimal> params)
            {
                BlockPos pos = locateBlockPosNum(params);
                return new BigDecimal(source.getWorld().getLight(pos));
            }
        });
        this.expr.addFunction(new AbstractFunction("skyLight", 3, false) {
            @Override
            public BigDecimal eval(List<BigDecimal> params)
            {
                BlockPos pos = locateBlockPosNum(params);
                return new BigDecimal(source.getWorld().getLightSubtracted(pos, 0));
            }
        });
        this.expr.addFunction(new AbstractFunction("power", 3, false) {
            @Override
            public BigDecimal eval(List<BigDecimal> params)
            {
                BlockPos pos = locateBlockPosNum(params);
                return new BigDecimal(source.getWorld().getRedstonePowerFromNeighbors(pos));
            }
        });

        /*
        this.expr.addLazyFunction(new AbstractLazyFunction("s", 1, false) {  // why not
            @Override
            public LazyNumber lazyEval(List<LazyNumber> params)
            {
                return new StringValue(params.get(0).getString());
            }
        });
        */
        this.expr.addLazyFunction(new AbstractLazyFunction("property", 2, false) {  // why not
            @Override
            public LazyNumber lazyEval(List<LazyNumber> params)
            {
                if (!(params.get(0) instanceof BlockValue))
                    throw new ExpressionException("First Argument of tag should be a block");
                IBlockState state = ((BlockValue) params.get(0)).blockState;
                String tag = params.get(1).getString();
                StateContainer<Block, IBlockState> states = state.getBlock().getStateContainer();
                IProperty<?> property = states.getProperty(tag);
                if (property == null)
                    return new StringValue("");
                return new StringValue(state.get(property).toString());
            }
        });

        this.expr.addFunction(new AbstractFunction("relu", 1, false) {  // why not
            @Override
            public BigDecimal eval(List<BigDecimal> params)
            {
                if (params.get(0).compareTo(BigDecimal.ZERO) < 0)
                    return BigDecimal.ZERO;
                return params.get(0);
            }
        });

    }

    public long eval(BlockPos at)
    {
        return this.expr.
                with("x",new BigDecimal(at.getX()-origin.getX())).
                with("y",new BigDecimal(at.getY()-origin.getY())).
                with("z",new BigDecimal(at.getZ()-origin.getZ())).
                eval().longValue();
    }

    public long eval(int x, int y, int z)
    {
        return this.expr.
                with("x",new BigDecimal(x-origin.getX())).
                with("y",new BigDecimal(y-origin.getY())).
                with("z",new BigDecimal(z-origin.getZ())).
                eval().longValue();
    }
}
