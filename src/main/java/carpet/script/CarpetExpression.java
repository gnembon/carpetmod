package carpet.script;

import carpet.CarpetSettings;
import carpet.script.*;
import carpet.script.Expression.ExpressionException;
import carpet.script.Expression.LazyValue;
import carpet.utils.BlockInfo;
import carpet.utils.Messenger;
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
import net.minecraft.world.WorldServer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class CarpetExpression
{
    public static class CarpetExpressionException extends ExpressionException
    {
        public CarpetExpressionException(String message)
        {
            super(message);
        }
    }

    private CommandSource source;
    private BlockPos origin;
    private Expression expr;

    public static class BlockValue extends StringValue
    {
        public static final BlockValue AIR = new BlockValue(Blocks.AIR.getDefaultState(), BlockPos.ORIGIN);
        public IBlockState blockState;
        public BlockPos pos;

        public BlockValue(IBlockState arg, BlockPos position)
        {
            super(IRegistry.field_212618_g.getKey(arg.getBlock()).getPath());
            blockState = arg;
            pos = position;
        }

        @Override
        public boolean getBoolean()
        {
            return !blockState.isAir();
        }

        @Override
        public String getString()
        {
            return IRegistry.field_212618_g.getKey(blockState.getBlock()).getPath();
        }

        public Value copy()
        {
            return new BlockValue(blockState, pos);
        }

    }

    private BlockPos locateBlockPos(List<Value> params)
    {
        if (params.size() < 3)
            throw new ExpressionException("Need three integers for params");
        int xpos = ((NumericValue) params.get(0)).getNumber().intValue();
        int ypos = ((NumericValue) params.get(1)).getNumber().intValue();
        int zpos = ((NumericValue) params.get(2)).getNumber().intValue();
        return new BlockPos(origin.getX() + xpos, origin.getY() + ypos, origin.getZ() + zpos);
    }

    private BlockPos locateBlockPos(int xpos, int ypos, int zpos)
    {
        return new BlockPos(origin.getX() + xpos, origin.getY() + ypos, origin.getZ() + zpos);
    }

    private Value booleanStateTest(
            String name,
            List<Value> params,
            BiFunction<IBlockState, BlockPos, Boolean> test
    )
    {
        if (params.size() == 0)
        {
            throw new ExpressionException(name + " requires at least one parameter");
        }
        if (params.get(0) instanceof BlockValue)
            return test.apply(((BlockValue) params.get(0)).blockState, ((BlockValue) params.get(0)).pos) ? Value.TRUE :
                    Value.FALSE;
        BlockPos pos = locateBlockPos(params);
        return test.apply(source.getWorld().getBlockState(pos), pos) ? Value.TRUE : Value.FALSE;
    }

    private Value stateStringQuery(
            String name,
            List<Value> params,
            BiFunction<IBlockState, BlockPos, String> test
    )
    {
        if (params.size() == 0)
        {
            throw new ExpressionException(name + " requires at least one parameter");
        }
        if (params.get(0) instanceof BlockValue)
            return new StringValue(test.apply(((BlockValue) params.get(0)).blockState, ((BlockValue) params.get(0)).pos));
        BlockPos pos = locateBlockPos(params);
        return new StringValue(test.apply(source.getWorld().getBlockState(pos), pos));
    }


    private <T extends Comparable<T>> IBlockState setProperty(IProperty<T> property, String name, String value,
                                                              IBlockState bs)
    {
        Optional<T> optional = property.parseValue(value);

        if (optional.isPresent())
        {
            bs = bs.with(property, optional.get());
        }
        else
        {
            throw new CarpetExpressionException(value + " is not a valid value for property " + name);
        }
        return bs;
    }

    public CarpetExpression(String expression, CommandSource source, BlockPos origin)
    {
        this.origin = origin;
        this.source = source;
        this.expr = new Expression(expression);

        this.expr.addFunction("block", (lv) ->
        {
                if (lv.size() == 0)
                {
                    throw new ExpressionException("block requires at least one parameter");
                }
                if (lv.size() == 1)
                {
                    return new BlockValue(IRegistry.field_212618_g.get(new ResourceLocation(lv.get(0).getString())).getDefaultState(), origin);
                }
                BlockPos pos = locateBlockPos(lv);
                return new BlockValue(source.getWorld().getBlockState(pos), pos);
        });

        this.expr.addFunction("solid", (lv) -> booleanStateTest("solid", lv, (s, p) -> s.isSolid()));

        this.expr.addFunction("air", (lv) -> booleanStateTest("air", lv, (s, p) -> s.isAir()));

        this.expr.addFunction("liquid", (lv) -> booleanStateTest("liquid", lv, (s, p) -> !s.getFluidState().isEmpty()));

        this.expr.addNAryFunction("light", 3, (lv) ->
                new NumericValue(source.getWorld().getLight(locateBlockPos(lv))));

        this.expr.addNAryFunction("blockLight", 3, (lv) ->
                new NumericValue(source.getWorld().getLightFor(EnumLightType.BLOCK, locateBlockPos(lv))));

        this.expr.addNAryFunction("skyLight", 3, (lv) ->
                new NumericValue(source.getWorld().getLightFor(EnumLightType.SKY, locateBlockPos(lv))));

        this.expr.addNAryFunction("loaded", 3, (lv) ->
                source.getWorld().isBlockLoaded(locateBlockPos(lv)) ? Value.TRUE : Value.FALSE);

        this.expr.addNAryFunction("loadedEP", 3, (lv) ->
        {
            BlockPos pos = locateBlockPos(lv);
            return source.getWorld().isAreaLoaded(pos.getX() - 32, 0, pos.getZ() - 32,
                    pos.getX() + 32, 0, pos.getZ() + 32, true) ? Value.TRUE : Value.FALSE;
        });

        this.expr.addFunction("suffocates", (lv) ->
                booleanStateTest("suffocates", lv, (s, p) -> s.causesSuffocation()));

        this.expr.addNAryFunction("power", 3, (lv) ->
                new NumericValue(source.getWorld().getRedstonePowerFromNeighbors(locateBlockPos(lv))));

        this.expr.addFunction("ticksRandomly", (lv) ->
                booleanStateTest("ticksRandomly", lv, (s, p) -> s.needsRandomTick()));

        this.expr.addFunction("update", (lv) ->
                booleanStateTest("update", lv, (s, p) ->
                {
                    source.getWorld().neighborChanged(p, s.getBlock(), p);
                    return true;
                }));

        this.expr.addFunction("forcetick",(lv) ->
                booleanStateTest("forcetick", lv, (s, p) ->
                {
                    s.randomTick(source.getWorld(), p, source.getWorld().rand);
                    return true;
                }));

        this.expr.addFunction("tick", (lv) ->
                booleanStateTest("tick", lv, (s, p) ->
                {
                    if (s.needsRandomTick() || s.getFluidState().getTickRandomly())
                        s.randomTick(source.getWorld(), p, source.getWorld().rand);
                    return true;
                }));

        this.expr.addFunction("set", (lv) ->
        {
            if (lv.size() < 4 || lv.size() % 2 == 1)
                throw new CarpetExpressionException("set block should have at least 4 params and odd attributes");
            BlockPos pos = locateBlockPos(lv);
            if (!(lv.get(3) instanceof BlockValue))
                throw new CarpetExpressionException("fourth parameter of set should be a block");
            IBlockState bs = ((BlockValue) lv.get(3)).blockState;

            IBlockState targetBlockState = source.getWorld().getBlockState(pos);
            if (lv.size()==4) // no reqs for properties
                if (targetBlockState.getBlock() == bs.getBlock())
                    return Value.FALSE;

            StateContainer<Block, IBlockState> states = bs.getBlock().getStateContainer();

            for (int i = 4; i < lv.size(); i += 2)
            {
                String paramString = lv.get(i).getString();
                IProperty<?> property = states.getProperty(paramString);
                if (property == null)
                    throw new CarpetExpressionException("property " + paramString + " doesn't apply to " + lv.get(3).getString());

                String paramValue = lv.get(i + 1).getString();

                bs = setProperty(property, paramString, paramValue, bs);
            }
            source.getWorld().setBlockState(pos, bs, 2 | (CarpetSettings.getBool("fillUpdates") ? 0 : 1024));
            return new BlockValue(bs, pos);
        });

        this.expr.addFunction("blocksMovement", (lv) ->
                booleanStateTest("blocksMovement", lv, (s, p) ->
                        !s.allowsMovement(source.getWorld(), p, PathType.LAND)));

        this.expr.addFunction("sound", (lv) ->
                stateStringQuery("sound", lv, (s, p) ->
                        BlockInfo.soundName.get(s.getBlock().getSoundType())));

        this.expr.addFunction("material",(lv) ->
                stateStringQuery("material", lv, (s, p) ->
                        BlockInfo.materialName.get(s.getMaterial())));

        this.expr.addFunction("mapColour", (lv) ->
                stateStringQuery("mapColour", lv, (s, p) ->
                        BlockInfo.mapColourName.get(s.getMapColor(source.getWorld(), p))));

        this.expr.addBinaryFunction("property", (v1, v2) ->
        {
                if (!(v1 instanceof BlockValue))
                    throw new ExpressionException("First Argument of tag should be a block");
                IBlockState state = ((BlockValue) v1).blockState;
                String tag = v2.getString();
                StateContainer<Block, IBlockState> states = state.getBlock().getStateContainer();
                IProperty<?> property = states.getProperty(tag);
                if (property == null)
                    return Value.EMPTY;
                return new StringValue(state.get(property).toString());

        });

        this.expr.addUnaryFunction("print", (v) ->
        {
            Messenger.m(source, "gi " + v.getString());
            return v; // pass through for variables
        });

        this.expr.addNAryFunction("neighbours", 3, (lv)->
        {
            BlockPos center = locateBlockPos(lv);
            WorldServer world = source.getWorld();

            List<Value> neighbours = new ArrayList<>();
            neighbours.add(new BlockValue(world.getBlockState(center.up()), center.up()));
            neighbours.add(new BlockValue(world.getBlockState(center.down()), center.down()));
            neighbours.add(new BlockValue(world.getBlockState(center.north()), center.north()));
            neighbours.add(new BlockValue(world.getBlockState(center.south()), center.south()));
            neighbours.add(new BlockValue(world.getBlockState(center.east()), center.east()));
            neighbours.add(new BlockValue(world.getBlockState(center.west()), center.west()));
            return new ListValue(neighbours);
        });

        //conv (x,y,z,sx,sy,sz, (_x, _y, _z, _block, _a) -> expr, ?acc) ->
        this.expr.addLazyFunction("conv", -1, (lv)->
        {
            Value acc;
            if (lv.size() == 7)
                acc = new NumericValue(0);
            else if (lv.size() ==8)
                acc = lv.get(7).eval();
            else
                throw new CarpetExpressionException("conv accepts 7 or 8 parameters");
            LazyValue expr = lv.get(6);
            int cx;
            int cy;
            int cz;
            int sx;
            int sy;
            int sz;
            try
            {
                cx = ((NumericValue) lv.get(0).eval()).getNumber().intValue();
                cy = ((NumericValue) lv.get(1).eval()).getNumber().intValue();
                cz = ((NumericValue) lv.get(2).eval()).getNumber().intValue();
                sx = ((NumericValue) lv.get(3).eval()).getNumber().intValue();
                sy = ((NumericValue) lv.get(4).eval()).getNumber().intValue();
                sz = ((NumericValue) lv.get(5).eval()).getNumber().intValue();
            }
            catch (ClassCastException exc)
            {
                throw new CarpetExpressionException("Attempted to pass a non-number to conv");
            }
            //saving outer scope
            LazyValue _x = this.expr.getVariable("_x");
            LazyValue _y = this.expr.getVariable("_y");
            LazyValue _z = this.expr.getVariable("_z");
            LazyValue _a = this.expr.getVariable("_a");
            for (int x = cx-sx; x <= cx+sx; x++)
            {
                for (int z = cz-sz; z <= cz+sz; z++)
                {
                    for (int y = cy-sy; y <= cy+sy; y++)
                    {
                        Value kidYouNotWontChange = acc;
                        int ser = x;
                        int you = y;
                        int sly = z;
                        this.expr.setVariable("_x", () -> new NumericValue(ser).boundTo("_x"));
                        this.expr.setVariable("_y", () -> new NumericValue(you).boundTo("_y"));
                        this.expr.setVariable("_z", () -> new NumericValue(sly).boundTo("_z"));
                        this.expr.setVariable("_a", () -> kidYouNotWontChange.boundTo("_a"));
                        acc = expr.eval();
                    }
                }
            }
            //restoring outer scope
            this.expr.setVariable("_x", _x);
            this.expr.setVariable("_y", _y);
            this.expr.setVariable("_z", _z);
            this.expr.setVariable("_a", _a);
            Value honestWontChange = acc;
            return () -> honestWontChange;
        });

        //conv (x,y,z,(_x, _y, _z, _a) -> expr, ?acc) ->
        this.expr.addLazyFunction("convnb", -1, (lv)->
        {
            Value acc;
            if (lv.size() == 4)
                acc = new NumericValue(0);
            else if (lv.size() ==5)
                acc = lv.get(4).eval();
            else
                throw new CarpetExpressionException("convnb accepts 4 or 5 parameters");
            LazyValue expr = lv.get(3);
            int cx;
            int cy;
            int cz;
            try
            {
                cx = ((NumericValue) lv.get(0).eval()).getNumber().intValue();
                cy = ((NumericValue) lv.get(1).eval()).getNumber().intValue();
                cz = ((NumericValue) lv.get(2).eval()).getNumber().intValue();
            }
            catch (ClassCastException exc)
            {
                throw new CarpetExpressionException("Attempted to pass a non-number to conv");
            }
            BlockPos pos = new BlockPos(cx, cy, cz); // its deliberately offset wrt origin, only used to get nbs coords
            //saving outer scope
            LazyValue _x = this.expr.getVariable("_x");
            LazyValue _y = this.expr.getVariable("_y");
            LazyValue _z = this.expr.getVariable("_z");
            LazyValue _a = this.expr.getVariable("_a");
            for (BlockPos nb: Arrays.asList(pos.down(), pos.north(), pos.south(), pos.east(), pos.west(), pos.up()))
            {
                Value kidYouNotWontChange = acc;
                this.expr.setVariable("_x", () -> new NumericValue(nb.getX()).boundTo("_x"));
                this.expr.setVariable("_y", () -> new NumericValue(nb.getY()).boundTo("_y"));
                this.expr.setVariable("_z", () -> new NumericValue(nb.getZ()).boundTo("_z"));
                this.expr.setVariable("_a", () -> kidYouNotWontChange.boundTo("_a"));
                acc = expr.eval();
            }
            //restoring outer scope
            this.expr.setVariable("_x", _x);
            this.expr.setVariable("_y", _y);
            this.expr.setVariable("_z", _z);
            this.expr.setVariable("_a", _a);
            Value honestWontChange = acc;
            return () -> honestWontChange;
        });


    }

    public boolean test(BlockPos pos)
    {
        return test(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean test(int x, int y, int z)
    {
        try
        {
            return this.expr.
                    with("x", new BigDecimal(x - origin.getX())).
                    with("y", new BigDecimal(y - origin.getY())).
                    with("z", new BigDecimal(z - origin.getZ())).
                    eval().getBoolean();
        }
        catch (ExpressionException e)
        {
            throw new CarpetExpressionException(e.getMessage());
        }
    }

    public String eval(BlockPos pos)
    {
        return eval(pos.getX(), pos.getY(), pos.getZ());
    }

    public String eval(int x, int y, int z)
    {
        try
        {
            return this.expr.
                    with("x", new BigDecimal(x - origin.getX())).
                    with("y", new BigDecimal(y - origin.getY())).
                    with("z", new BigDecimal(z - origin.getZ())).
                    eval().getString();
        }
        catch (ExpressionException e)
        {
            throw new CarpetExpressionException(e.getMessage());
        }
    }

    public void setLogOutput(boolean to)
    {
        this.expr.setLogOutput(to ? (s) -> Messenger.m(source, "gi " + s) : null);
    }
}
