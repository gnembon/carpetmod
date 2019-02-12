package carpet.script;

import carpet.CarpetSettings;
import carpet.helpers.FeatureGenerator;
import carpet.script.Expression.ExpressionException;
import carpet.script.Expression.LazyValue;
import carpet.utils.BlockInfo;
import carpet.utils.Messenger;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.ParticleArgument;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.particles.IParticleData;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.world.EnumLightType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.Heightmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class CarpetExpression
{
    public static class CarpetExpressionException extends ExpressionException
    {
        CarpetExpressionException(String message)
        {
            super(message);
        }
    }

    private CommandSource source;
    private WorldServer world;
    private BlockPos origin;
    private Expression expr;

    public static class BlockValue extends StringValue
    {
        public static final BlockValue AIR = new BlockValue(Blocks.AIR.getDefaultState(), BlockPos.ORIGIN);
        public IBlockState blockState;
        public BlockPos pos;

        BlockValue(IBlockState arg, BlockPos position)
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

        public Value copy()
        {
            return new BlockValue(blockState, pos);
        }

    }
    private BlockValue blockValueFromCoords(int x, int y, int z)
    {
        BlockPos pos = locateBlockPos(x,y,z);
        return new BlockValue(world.getBlockState(pos), pos);
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

    BlockPos locateBlockPos(int xpos, int ypos, int zpos)
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
        this.world = source.getWorld();
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

        this.expr.addNAryFunction("seeSky", 3, (lv) ->
                new NumericValue(source.getWorld().canSeeSky(locateBlockPos(lv))));

        this.expr.addNAryFunction("topOpaque", -1, (lv) -> {
            int x;
            int z;
            if (lv.get(1) instanceof BlockValue)
            {
                BlockPos inpos = ((BlockValue)lv.get(1)).pos;
                x = inpos.getX();
                z = inpos.getZ();
            }
            else
            {
                x = Expression.getNumericalValue(lv.get(0)).intValue();
                z = Expression.getNumericalValue(lv.get(1)).intValue();
            }
            int y = source.getWorld().getChunk(x >> 4, z >> 4).getTopBlockY(Heightmap.Type.LIGHT_BLOCKING, x & 15, z & 15) + 1;
            BlockPos pos = new BlockPos(x,y,z);
            return new BlockValue(source.getWorld().getBlockState(pos), pos);
        });

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
                return Value.NULL;
            return new StringValue(state.get(property).toString());
        });

        //particle(x,y,z,"particle",count?10, duration,bool all)
        this.expr.addNAryFunction("particle", -1, (lv) ->
        {
            BlockPos pos = locateBlockPos(lv);
            String particleName = lv.get(3).getString();
            int count = 10;
            float speed = 0;
            EntityPlayerMP player = null;
            if (lv.size() > 4)
            {
                count = Expression.getNumericalValue(lv.get(4)).intValue();
                if (lv.size() > 5)
                {
                    speed = Expression.getNumericalValue(lv.get(5)).floatValue();
                    if (lv.size() > 6)
                    {
                        player = source.getServer().getPlayerList().getPlayerByUsername(lv.get(6).getString());
                    }
                }
            }
            IParticleData particle;
            try
            {
                particle = ParticleArgument.func_197189_a(new StringReader(particleName));
            }
            catch (CommandSyntaxException e)
            {
                return Value.NULL;
            }
            if (player == null)
            {
                for (EntityPlayerMP p : source.getServer().getPlayerList().getPlayers())
                {
                    world.spawnParticle(p, particle, true,pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, count,
                            0.5, 0.5, 0.5, speed);
                }
            }
            else
            {
                world.spawnParticle(player,
                    particle, true,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, count,
                    0.5, 0.5, 0.5, speed);
            }

            return Value.TRUE;
        });


        this.expr.addUnaryFunction("print", (v) ->
        {
            Messenger.m(source, "w " + v.getString());
            return v; // pass through for variables
        });

        this.expr.addUnaryFunction("run", (v) -> {
            BlockPos target = locateBlockPos(
                    Expression.getNumericalValue(this.expr.getVariable("x").eval()).intValue(),
                    Expression.getNumericalValue(this.expr.getVariable("y").eval()).intValue(),
                    Expression.getNumericalValue(this.expr.getVariable("z").eval()).intValue()
            );
            Vec3d posf = new Vec3d((double)target.getX()+0.5D,(double)target.getY(),(double)target.getZ()+0.5D);
            return new NumericValue(source.getServer().getCommandManager().handleCommand(
                    source.withPos(posf).withFeedbackDisabled(), v.getString()));
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
        this.expr.addLazyFunction("convsquare", -1, (lv)->
        {
            Value acc;
            if (lv.size() == 7)
                acc = new NumericValue(0);
            else if (lv.size() ==8)
                acc = lv.get(7).eval();
            else
                throw new CarpetExpressionException("convsquare accepts 7 or 8 parameters");
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
            LazyValue __ = this.expr.getVariable("_");
            for (int x = cx-sx; x <= cx+sx; x++)
            {
                for (int z = cz-sz; z <= cz+sz; z++)
                {
                    for (int y = cy-sy; y <= cy+sy; y++)
                    {
                        int xFinal = x;
                        int yFinal = y;
                        int zFinal = z;
                        this.expr.setVariable( "_", () -> blockValueFromCoords(xFinal,yFinal,zFinal).boundTo("_"));
                        this.expr.setVariable("_x", () -> new NumericValue(xFinal).boundTo("_x"));
                        this.expr.setVariable("_y", () -> new NumericValue(yFinal).boundTo("_y"));
                        this.expr.setVariable("_z", () -> new NumericValue(zFinal).boundTo("_z"));
                        this.expr.setVariable("_a", acc);
                        acc = expr.eval();
                    }
                }
            }
            //restoring outer scope
            this.expr.setVariable("_x", _x);
            this.expr.setVariable("_y", _y);
            this.expr.setVariable("_z", _z);
            this.expr.setVariable("_a", _a);
            this.expr.setVariable("_", __);
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
            LazyValue __ = this.expr.getVariable("_");
            for (BlockPos nb: Arrays.asList(pos.down(), pos.north(), pos.south(), pos.east(), pos.west(), pos.up()))
            {
                this.expr.setVariable( "_", () -> new BlockValue(world.getBlockState(nb), nb).boundTo("_"));
                this.expr.setVariable("_x", () -> new NumericValue(nb.getX()).boundTo("_x"));
                this.expr.setVariable("_y", () -> new NumericValue(nb.getY()).boundTo("_y"));
                this.expr.setVariable("_z", () -> new NumericValue(nb.getZ()).boundTo("_z"));
                this.expr.setVariable("_a", acc);
                acc = expr.eval();
            }
            //restoring outer scope
            this.expr.setVariable("_x", _x);
            this.expr.setVariable("_y", _y);
            this.expr.setVariable("_z", _z);
            this.expr.setVariable("_a", _a);
            this.expr.setVariable("_", __);
            Value honestWontChange = acc;
            return () -> honestWontChange;
        });

        //not ready yet
        this.expr.addNAryFunction("plop", 4, (lv) ->{
            BlockPos pos = locateBlockPos(lv);
            Boolean res = FeatureGenerator.spawn(lv.get(3).getString(), source.getWorld(), pos);
            if (res == null)
                return Value.NULL;
            return new NumericValue(res);
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
                    with("x", () -> new NumericValue(x - origin.getX()).boundTo("x")).
                    with("y", () -> new NumericValue(y - origin.getY()).boundTo("y")).
                    with("z", () -> new NumericValue(z - origin.getZ()).boundTo("z")).
                    with("block", () -> {
                        BlockPos pos = new BlockPos(x,y,z);
                        return new BlockValue(world.getBlockState(pos), pos).boundTo("block");
                    }).
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
                    with("x", () -> new NumericValue(x - origin.getX()).boundTo("x")).
                    with("y", () -> new NumericValue(y - origin.getY()).boundTo("y")).
                    with("z", () -> new NumericValue(z - origin.getZ()).boundTo("z")).
                    with("block", () -> {
                        BlockPos pos = new BlockPos(x,y,z);
                        return new BlockValue(world.getBlockState(pos), pos).boundTo("block");
                    }).
                    eval().getString();
        }
        catch (ExpressionException e)
        {
            throw new CarpetExpressionException(e.getMessage());
        }
    }

    public void copyStateFrom(CarpetExpression other)
    {
        this.expr.copyStateFrom(other.expr);
    }

    public void execute()
    {
        this.expr.eval().getString();
    }


    public void setLogOutput(boolean to)
    {
        this.expr.setLogOutput(to ? (s) -> Messenger.m(source, "gi " + s) : null);
    }
}
