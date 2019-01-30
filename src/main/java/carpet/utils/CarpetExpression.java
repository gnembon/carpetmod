package carpet.utils;

import carpet.CarpetSettings;
import com.udojava.evalex.AbstractLazyFunction;
import com.udojava.evalex.Expression;
import com.udojava.evalex.Expression.ExpressionException;
import com.udojava.evalex.Expression.LazyNumber;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

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
            return blockState.getBlock().getTranslationKey();
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
        if (ypos < -1000255 || ypos > 1000255 || xpos > 10000 || xpos < -10000 || zpos > 10000 || zpos< -10000)
        {
            throw new ExpressionException("Attempting to locate block outside of 10k blocks range");
        }
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
                    throw new ExpressionException("average requires at least one parameter");
                }
                if (lazyParams.size() == 1)
                {
                    return new BlockValue(IRegistry.field_212618_g.get(new ResourceLocation(lazyParams.get(0).getString())).getDefaultState());
                }
                BlockPos pos = locateBlockPos(lazyParams);
                CarpetSettings.LOG.error("Block pos at "+pos);
                return new BlockValue(source.getWorld().getBlockState(pos));
            }
        });
        this.expr.addLazyFunction(new AbstractLazyFunction("solid", 1, true) {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                if (!(lazyParams.get(0) instanceof BlockValue))
                    throw new ExpressionException("solid function takes a block as argument");
                if (((BlockValue)lazyParams.get(0)).blockState.isSolid())
                    return TRUE;
                return FALSE;
            }
        });

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
