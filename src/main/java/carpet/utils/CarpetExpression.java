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
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.world.EnumLightType;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiFunction;

public class CarpetExpression
{
    private CommandSource source;
    private BlockPos origin;
    private Expression expr;
    private static final LazyNumber TRUE = Expression.CreateLazyNumber(BigDecimal.ONE);
    private static final LazyNumber FALSE = Expression.CreateLazyNumber(BigDecimal.ZERO);

    public static class BlockValue implements LazyNumber
    {
        public static final BlockValue AIR = new BlockValue(Blocks.AIR.getDefaultState(), BlockPos.ORIGIN);
        public IBlockState blockState;
        public BlockPos pos;
        public BlockValue(IBlockState arg, BlockPos position)
        {
            blockState = arg;
            pos = position;
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

    private LazyNumber booleanStateTest(
            String name,
            List<LazyNumber> params,
            BiFunction<IBlockState,BlockPos,Boolean> test
    )
    {
        if (params.size() == 0)
        {
            throw new ExpressionException(name+" requires at least one parameter");
        }
        if (params.get(0) instanceof BlockValue)
            return test.apply(((BlockValue)params.get(0)).blockState, ((BlockValue)params.get(0)).pos)?TRUE:FALSE;
        BlockPos pos = locateBlockPos(params);
        return test.apply(source.getWorld().getBlockState(pos), pos)?TRUE:FALSE;
    }

    private LazyNumber stateStringQuery(
            String name,
            List<LazyNumber> params,
            BiFunction<IBlockState, BlockPos, String> test
    )
    {
        if (params.size() == 0)
        {
            throw new ExpressionException(name+" requires at least one parameter");
        }
        if (params.get(0) instanceof BlockValue)
            return new StringValue(test.apply(((BlockValue)params.get(0)).blockState, ((BlockValue)params.get(0)).pos));
        BlockPos pos = locateBlockPos(params);
        return new StringValue(test.apply(source.getWorld().getBlockState(pos), pos));
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
                    return new BlockValue(IRegistry.field_212618_g.get(new ResourceLocation(lazyParams.get(0).getString())).getDefaultState(), origin);
                }
                BlockPos pos = locateBlockPos(lazyParams);
                return new BlockValue(source.getWorld().getBlockState(pos), pos);
            }
        });

        this.expr.addLazyFunction(new AbstractLazyFunction("isSolid", -1, true)
        {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                return booleanStateTest("isSolid", lazyParams, (s, p) -> s.isSolid());
            }
        });

        this.expr.addLazyFunction(new AbstractLazyFunction("isLiquid", -1, true)
        {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                return booleanStateTest("isLiquid", lazyParams, (s, p) -> !s.getFluidState().isEmpty());
            }
        });

        this.expr.addFunction(new AbstractFunction("light", 3, false)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> params)
            {
                return new BigDecimal(source.getWorld().getLight(locateBlockPosNum(params)));
            }
        });

        this.expr.addFunction(new AbstractFunction("blockLight", 3, false)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> params)
            {
                return new BigDecimal(source.getWorld().getLightFor(EnumLightType.BLOCK, locateBlockPosNum(params)));
            }
        });

        this.expr.addFunction(new AbstractFunction("skyLight", 3, false)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> params)
            {
                return new BigDecimal(source.getWorld().getLightFor(EnumLightType.SKY, locateBlockPosNum(params)));
            }
        });

        this.expr.addLazyFunction(new AbstractLazyFunction("loaded", 3, true)
        {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                return source.getWorld().isBlockLoaded(locateBlockPos(lazyParams))?TRUE:FALSE;
            }
        });

        this.expr.addLazyFunction(new AbstractLazyFunction("loadedEP", 3, true)
        {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                BlockPos pos = locateBlockPos(lazyParams);
                return source.getWorld().isAreaLoaded(
                        pos.getX() - 32, 0, pos.getZ() - 32,
                        pos.getX() + 32, 0, pos.getZ() + 32, true)? TRUE: FALSE;
            }
        });

        this.expr.addLazyFunction(new AbstractLazyFunction("suffocates", -1, true) {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                return booleanStateTest("suffocates", lazyParams, (s, p) -> s.causesSuffocation());
            }
        });

        this.expr.addFunction(new AbstractFunction("power", 3, false) {
            @Override
            public BigDecimal eval(List<BigDecimal> params)
            {
                return new BigDecimal(source.getWorld().getRedstonePowerFromNeighbors(locateBlockPosNum(params)));
            }
        });

        this.expr.addLazyFunction(new AbstractLazyFunction("ticksRandomly", -1, true)
        {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                return booleanStateTest("ticksRandomly", lazyParams, (s, p) -> s.needsRandomTick());
            }
        });

        this.expr.addLazyFunction(new AbstractLazyFunction("update", -1, true)
        {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                return booleanStateTest("update", lazyParams, (s, p) -> {
                    source.getWorld().neighborChanged(p, s.getBlock(), p);
                    return false;
                });
            }
        });

        this.expr.addLazyFunction(new AbstractLazyFunction("forcetick", -1, true)
        {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                return booleanStateTest("forcetick", lazyParams, (s, p) -> {

                    s.randomTick(source.getWorld(), p, source.getWorld().rand);
                    return false;
                });
            }
        });

        this.expr.addLazyFunction(new AbstractLazyFunction("tick", -1, true)
        {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                return booleanStateTest("tick", lazyParams, (s, p) -> {
                    if (s.needsRandomTick() || s.getFluidState().getTickRandomly())
                    {
                        s.randomTick(source.getWorld(), p, source.getWorld().rand);
                    }
                    return false;
                });
            }
        });

        this.expr.addLazyFunction(new AbstractLazyFunction("set", -1, true)
        {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                return booleanStateTest("set", lazyParams, (s, p) -> s.needsRandomTick());
            }
        });

        this.expr.addLazyFunction(new AbstractLazyFunction("blocksMovement", -1, true)
        {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                return booleanStateTest("blocksMovement", lazyParams, (s, p)
                        -> !s.allowsMovement(source.getWorld(), p, PathType.LAND));
            }
        });

        this.expr.addLazyFunction(new AbstractLazyFunction("sound", -1, true)
        {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                return stateStringQuery("sound", lazyParams, (s, p)
                        -> BlockInfo.soundName.get(s.getBlock().getSoundType()));
            }
        });

        this.expr.addLazyFunction(new AbstractLazyFunction("material", -1, true)
        {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                return stateStringQuery("material", lazyParams, (s, p)
                        -> BlockInfo.materialName.get(s.getMaterial()));
            }
        });

        this.expr.addLazyFunction(new AbstractLazyFunction("mapColour", -1, true)
        {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams)
            {
                return stateStringQuery("mapColour", lazyParams, (s, p)
                        -> BlockInfo.mapColourName.get(s.getMapColor(source.getWorld(), p)));
            }
        });

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

    public String evalString(int x, int y, int z)
    {
        return this.expr.
                with("x",new BigDecimal(x-origin.getX())).
                with("y",new BigDecimal(y-origin.getY())).
                with("z",new BigDecimal(z-origin.getZ())).
                evalLazy().getString();
    }
}
