package carpet.script;

import carpet.CarpetSettings;
import carpet.helpers.FeatureGenerator;
import carpet.script.Expression.ExpressionException;
import carpet.script.Expression.InternalExpressionException;
import carpet.utils.BlockInfo;
import carpet.utils.Messenger;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javafx.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.EntitySelector;
import net.minecraft.command.arguments.EntitySelectorParser;
import net.minecraft.command.arguments.ParticleArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketCustomSound;
import net.minecraft.particles.IParticleData;
import net.minecraft.pathfinding.PathType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.world.EnumLightType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.storage.SessionLockException;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class CarpetExpression
{
    private CommandSource source;
    private BlockPos origin;
    private Expression expr;
    private static long tickStart = 0L;

    private static boolean stopAll = false;

    /**
     * <h1>script stop/resume commands</h1>
     * <p>Section Content</p>
     * <p>Other Paragraph</p>
     * Stop All allows to stop execution of any scipt currently running that calls the tick() function which
     * allows the gameloop to regain control of the game and process user commands. Typing <code>script stop</code> would make sure
     * that all the scripts will stop their execution. Execution will be halted until <code>script resume</code> is called
     *
     * @param doStop .
     */
    public static void BreakExecutionOfAllScriptsWithCommands(boolean doStop)
    {
        stopAll = doStop;
    }

    static class CarpetContext extends Context
    {
        public CommandSource s;
        public BlockPos origin;
        CarpetContext(Expression expr, CommandSource source, BlockPos origin)
        {
            super(expr);
            s = source;
            this.origin = origin;
        }

        @Override
        public Context recreateFor(Expression e)
        {
            return new CarpetContext(e, this.s, this.origin);
        }

    }



    private LazyValue booleanStateTest(
            Context c,
            String name,
            List<LazyValue> params,
            BiFunction<IBlockState, BlockPos, Boolean> test
    )
    {
        CarpetContext cc = (CarpetContext) c;
        if (params.size() == 0)
        {
            throw new InternalExpressionException(name + " requires at least one parameter");
        }
        Value v0 = params.get(0).evalValue(c);
        if (v0 instanceof BlockValue)
            return (c_, t_) -> test.apply(((BlockValue) v0).getBlockState(), ((BlockValue) v0).getPos()) ? Value.TRUE : Value.FALSE;
        BlockValue block = BlockValue.fromParams(cc, params, 0).block;
        return (c_, t_) -> test.apply(block.getBlockState(), block.getPos()) ? Value.TRUE : Value.FALSE;
    }

    private LazyValue stateStringQuery(
            Context c,
            String name,
            List<LazyValue> params,
            BiFunction<IBlockState, BlockPos, String> test
    )
    {
        CarpetContext cc = (CarpetContext) c;
        if (params.size() == 0)
        {
            throw new InternalExpressionException(name + " requires at least one parameter");
        }

        Value v0 = params.get(0).evalValue(c);
        if (v0 instanceof BlockValue)
            return (c_, t_) -> new StringValue(test.apply( ((BlockValue) v0).getBlockState(), ((BlockValue) v0).getPos()));
        BlockValue block = BlockValue.fromParams(cc, params, 0).block;
        return (c_, t_) -> new StringValue(test.apply(block.getBlockState(), block.getPos()));
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
            throw new InternalExpressionException(value + " is not a valid value for property " + name);
        }
        return bs;
    }

    /**
     * <h1>Blocks manipulations</h1>
     * <p>Section Content</p>
     * <p>Other Paragraph</p>
     */

    public void BlockManipulation()
    {
        this.expr.addLazyFunction("block", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0)
            {
                throw new InternalExpressionException("block requires at least one parameter");
            }
            if (lv.size() == 1)
            {
                return (c_, t_) -> BlockValue.fromString(lv.get(0).evalValue(cc).getString());
                //return new BlockValue(IRegistry.field_212618_g.get(new ResourceLocation(lv.get(0).getString())).getDefaultState(), origin);
            }
            return (c_, t_) -> BlockValue.fromParams(cc, lv, 0).block;
        });

        this.expr.addLazyFunction("pos", 1, (c, t, lv) ->
        {
            Value arg = lv.get(0).evalValue(c);
            if (arg instanceof BlockValue)
            {
                BlockPos pos = ((BlockValue) arg).getPos();
                if (pos == null)
                    throw new InternalExpressionException("cannot fetch position of unlocalized block");
                return (c_, t_) -> new ListValue(Arrays.asList(new NumericValue(pos.getX()), new NumericValue(pos.getY()), new NumericValue(pos.getZ())));
            }
            else if (arg instanceof EntityValue)
            {
                Entity e = ((EntityValue) arg).getEntity();
                if (e == null)
                    throw new InternalExpressionException("null entity");
                return(c_, t_) -> new ListValue(Arrays.asList(new NumericValue(e.posX), new NumericValue(e.posY), new NumericValue(e.posZ)));
            }
            else
            {
                throw new InternalExpressionException("you can only get position of a block type");
            }
        });

        this.expr.addLazyFunction("solid", -1, (c, t, lv) ->
                booleanStateTest(c, "solid", lv, (s, p) -> s.isSolid()));

        this.expr.addLazyFunction("air", -1, (c, t, lv) ->
                booleanStateTest(c, "air", lv, (s, p) -> s.isAir()));

        this.expr.addLazyFunction("liquid", -1, (c, t, lv) ->
                booleanStateTest(c, "liquid", lv, (s, p) -> !s.getFluidState().isEmpty()));

        this.expr.addLazyFunction("light", -1, (c, t, lv) ->
                (c_, t_) -> new NumericValue(((CarpetContext)c).s.getWorld().getLight(BlockValue.fromParams((CarpetContext) c, lv, 0).block.getPos())));

        this.expr.addLazyFunction("blockLight", -1, (c, t, lv) ->
                (c_, t_) -> new NumericValue(((CarpetContext)c).s.getWorld().getLightFor(EnumLightType.BLOCK, BlockValue.fromParams((CarpetContext) c, lv, 0).block.getPos())));

        this.expr.addLazyFunction("skyLight", -1, (c, t, lv) ->
                (c_, t_) -> new NumericValue(((CarpetContext)c).s.getWorld().getLightFor(EnumLightType.SKY, BlockValue.fromParams((CarpetContext) c, lv, 0).block.getPos())));

        this.expr.addLazyFunction("seeSky", -1, (c, t, lv) ->
                (c_, t_) -> new NumericValue(((CarpetContext)c).s.getWorld().canSeeSky(BlockValue.fromParams((CarpetContext) c, lv, 0).block.getPos())));

        this.expr.addLazyFunction("top", -1, (c, t, lv) -> {
            String type = lv.get(0).evalValue(c).getString().toLowerCase(Locale.ROOT);
            Heightmap.Type htype;
            switch (type)
            {
                case "light": htype = Heightmap.Type.LIGHT_BLOCKING; break;
                case "motion": htype = Heightmap.Type.MOTION_BLOCKING; break;
                case "surface": htype = Heightmap.Type.WORLD_SURFACE; break;
                case "ocean floor": htype = Heightmap.Type.OCEAN_FLOOR; break;
                case "terrain": htype = Heightmap.Type.MOTION_BLOCKING_NO_LEAVES; break;
                default: htype = Heightmap.Type.LIGHT_BLOCKING;
            }
            int x;
            int z;
            Value v1 = lv.get(1).evalValue(c);
            if (v1 instanceof BlockValue)
            {
                BlockPos inpos = ((BlockValue)v1).getPos();
                x = inpos.getX();
                z = inpos.getZ();
            }
            else
            {
                x = (int)Expression.getNumericValue(lv.get(1).evalValue(c)).getLong();
                z = (int)Expression.getNumericValue(lv.get(2).evalValue(c)).getLong();
            }
            int y = ((CarpetContext)c).s.getWorld().getChunk(x >> 4, z >> 4).getTopBlockY(htype, x & 15, z & 15) + 1;
            return (c_, t_) -> new NumericValue(y);
            //BlockPos pos = new BlockPos(x,y,z);
            //return new BlockValue(source.getWorld().getBlockState(pos), pos);
        });

        this.expr.addLazyFunction("loaded", -1, (c, t, lv) ->
                (c_, t_) -> ((CarpetContext)c).s.getWorld().isBlockLoaded(BlockValue.fromParams((CarpetContext) c, lv, 0).block.getPos()) ? Value.TRUE : Value.FALSE);

        this.expr.addLazyFunction("loadedEP", -1, (c, t, lv) ->
        {
            BlockPos pos = BlockValue.fromParams((CarpetContext)c, lv, 0).block.getPos();
            return (c_, t_) -> ((CarpetContext)c).s.getWorld().isAreaLoaded(pos.getX() - 32, 0, pos.getZ() - 32,
                    pos.getX() + 32, 0, pos.getZ() + 32, true) ? Value.TRUE : Value.FALSE;
        });

        this.expr.addLazyFunction("suffocates", -1, (c, t, lv) ->
                booleanStateTest(c, "suffocates", lv, (s, p) -> s.causesSuffocation()));

        this.expr.addLazyFunction("power", -1, (c, t, lv) ->
                (c_, t_) -> new NumericValue(((CarpetContext)c).s.getWorld().getRedstonePowerFromNeighbors(BlockValue.fromParams((CarpetContext) c, lv, 0).block.getPos())));

        this.expr.addLazyFunction("ticksRandomly", -1, (c, t, lv) ->
                booleanStateTest(c, "ticksRandomly", lv, (s, p) -> s.needsRandomTick()));

        this.expr.addLazyFunction("update", -1, (c, t, lv) ->
                booleanStateTest(c, "update", lv, (s, p) ->
                {
                    ((CarpetContext) c).s.getWorld().neighborChanged(p, s.getBlock(), p);
                    return true;
                }));

        this.expr.addLazyFunction("blocktick", -1, (c, t, lv) ->
                booleanStateTest(c, "blocktick", lv, (s, p) ->
                {
                    World w = ((CarpetContext)c).s.getWorld();
                    s.randomTick(w, p, w.rand);
                    return true;
                }));

        this.expr.addLazyFunction("randomtick", -1, (c, t, lv) ->
                booleanStateTest(c, "randomtick", lv, (s, p) ->
                {
                    World w = ((CarpetContext)c).s.getWorld();
                    if (s.needsRandomTick() || s.getFluidState().getTickRandomly())
                        s.randomTick(w, p, w.rand);
                    return true;
                }));


        this.expr.addLazyFunction("set", -1, (c, t, lv) ->
        {
            if (lv.size() == 3)
            {
                Value entityValue = lv.get(0).evalValue(c);
                if (!(entityValue instanceof EntityValue))
                {
                    throw new InternalExpressionException("Expecting entity with set call with 3 arguments");
                }
                String what = lv.get(1).evalValue(c).getString();
                Value toWhat = lv.get(2).evalValue(c);
                ((EntityValue) entityValue).set(what, toWhat);
                return LazyValue.TRUE;
            }
            CarpetContext cc = (CarpetContext)c;
            World world = cc.s.getWorld();
            if (lv.size() < 2 || lv.size() % 2 == 1)
                throw new InternalExpressionException("set block should have at least 2 params and odd attributes, or 3 for entity");
            BlockValue.LocatorResult locator = BlockValue.fromParams(cc, lv, 0);
            Value v3 = lv.get(locator.offset).evalValue(cc);
            BlockValue bv = ((v3 instanceof BlockValue)) ? (BlockValue) v3 : BlockValue.fromString(v3.getString());
            if (bv == BlockValue.NULL)
                throw new InternalExpressionException("block to set to should be a valid block");
            IBlockState bs = bv.getBlockState();

            IBlockState targetBlockState = world.getBlockState(locator.block.getPos());
            if (lv.size()==1+locator.offset) // no reqs for properties
                if (targetBlockState.getBlock() == bs.getBlock())
                    return (c_, t_) -> Value.FALSE;

            StateContainer<Block, IBlockState> states = bs.getBlock().getStateContainer();

            for (int i = 1+locator.offset; i < lv.size(); i += 2)
            {
                String paramString = lv.get(i).evalValue(c).getString();
                IProperty<?> property = states.getProperty(paramString);
                if (property == null)
                    throw new InternalExpressionException("property " + paramString + " doesn't apply to " + v3.getString());

                String paramValue = lv.get(i + 1).evalValue(c).getString();

                bs = setProperty(property, paramString, paramValue, bs);
            }
            cc.s.getWorld().setBlockState(locator.block.getPos(), bs, 2 | (CarpetSettings.getBool("fillUpdates") ? 0 : 1024));
            final IBlockState finalBS = bs;
            return (c_, t_) -> new BlockValue(finalBS, world, locator.block.getPos());
        });

        this.expr.addLazyFunction("blocksMovement", -1, (c, t, lv) ->
                booleanStateTest(c, "blocksMovement", lv, (s, p) ->
                        !s.allowsMovement(((CarpetContext) c).s.getWorld(), p, PathType.LAND)));

        this.expr.addLazyFunction("sound", -1, (c, t, lv) ->
                stateStringQuery(c, "sound", lv, (s, p) ->
                        BlockInfo.soundName.get(s.getBlock().getSoundType())));

        this.expr.addLazyFunction("material",-1, (c, t, lv) ->
                stateStringQuery(c, "material", lv, (s, p) ->
                        BlockInfo.materialName.get(s.getMaterial())));

        this.expr.addLazyFunction("mapColour", -1,  (c, t, lv) ->
                stateStringQuery(c, "mapColour", lv, (s, p) ->
                        BlockInfo.mapColourName.get(s.getMapColor(((CarpetContext)c).s.getWorld(), p))));

        this.expr.addLazyFunction("property", -1, (c, t, lv) ->
        {
            BlockValue.LocatorResult locator = BlockValue.fromParams((CarpetContext) c, lv, 0);
            IBlockState state = locator.block.getBlockState();
            String tag = lv.get(locator.offset).evalValue(c).getString();
            StateContainer<Block, IBlockState> states = state.getBlock().getStateContainer();
            IProperty<?> property = states.getProperty(tag);
            if (property == null)
                return LazyValue.NULL;
            return (_c, _t ) -> new StringValue(state.get(property).toString());
        });
    }

    /**
     * <h1>Entity manipulations</h1>
     * <p>Section Content</p>
     * <p>Other Paragraph</p>
     *
     *
     *
     * map(entities_area('all',x,y,z,30,30,30),run('kill '+query(_,'id'))) // doesn't kill the player
     */

    public void EntityManipulation()
    {
        this.expr.addLazyFunction("players", -1, (c, t, lv) -> {
            if (lv.size() ==0)
            {
                return (_c, _t) ->
                {
                    Entity callingEntity = ((CarpetContext)_c).s.getEntity();
                    if (callingEntity instanceof EntityPlayer)
                    {
                        return new EntityValue(callingEntity);
                    }
                    Vec3d pos = ((CarpetContext)_c).s.getPos();
                    EntityPlayer closestPlayer = ((CarpetContext)_c).s.getWorld().getClosestPlayer(pos.x, pos.y, pos.z, -1.0, EntitySelectors.IS_ALIVE);
                    if (closestPlayer != null)
                    {
                        return new EntityValue(closestPlayer);
                    }
                    return Value.NULL;
                };
            }
            String playerName = lv.get(0).evalValue(c).getString();
            if ("*".equalsIgnoreCase(playerName))
            {
                return (_c, _t) -> ListValue.wrap(((CarpetContext)_c).s.getServer().getPlayerList().getPlayers().stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            EntityPlayerMP player = ((CarpetContext)c).s.getServer().getPlayerList().getPlayerByUsername(playerName);
            if (player != null)
                return (_c, _t) -> new EntityValue(player);
            return LazyValue.NULL;
        });

        this.expr.addLazyFunction("entity_id", 1, (c, t, lv) ->
        {
            String who = lv.get(0).evalValue(c).getString();
            Entity e = ((CarpetContext)c).s.getWorld().getEntityFromUuid(UUID.fromString(who));
            if (e==null)
            {
                return LazyValue.NULL;
            }
            return (cc, tt) -> new EntityValue(e);
        });

        this.expr.addLazyFunction("entities_list", 1, (c, t, lv) -> {
            String who = lv.get(0).evalValue(c).getString();
            Pair<Class<? extends Entity>, Predicate<? super Entity>> pair = EntityValue.entityPredicates.get(who);
            if (pair == null)
            {
                throw new InternalExpressionException("Unknown entity selection criterion: "+who);
            }
            List<Entity> entityList = ((CarpetContext)c).s.getWorld().getEntities(pair.getKey(), pair.getValue());
            return (_c, _t ) -> ListValue.wrap(entityList.stream().map(EntityValue::new).collect(Collectors.toList()));
        });

        this.expr.addLazyFunction("entities_area", 7, (c, t, lv) -> {
            BlockPos center = new BlockPos(
                    Expression.getNumericValue(lv.get(1).evalValue(c)).getDouble(),
                    Expression.getNumericValue(lv.get(2).evalValue(c)).getDouble(),
                    Expression.getNumericValue(lv.get(3).evalValue(c)).getDouble()
            );
            AxisAlignedBB area = new AxisAlignedBB(center).grow(
                    Expression.getNumericValue(lv.get(4).evalValue(c)).getDouble(),
                    Expression.getNumericValue(lv.get(5).evalValue(c)).getDouble(),
                    Expression.getNumericValue(lv.get(6).evalValue(c)).getDouble()
            );
            String who = lv.get(0).evalValue(c).getString();
            Pair<Class<? extends Entity>, Predicate<? super Entity>> pair = EntityValue.entityPredicates.get(who);
            if (pair == null)
            {
                throw new InternalExpressionException("Unknown entity selection criterion: "+who);
            }
            List<Entity> entityList = ((CarpetContext)c).s.getWorld().getEntitiesWithinAABB(pair.getKey(), area, pair.getValue());
            return (_c, _t ) -> ListValue.wrap(entityList.stream().map(EntityValue::new).collect(Collectors.toList()));
        });

        this.expr.addLazyFunction("entities_selector", -1, (c, t, lv) -> {
            String selector = lv.get(0).evalValue(c).getString();

            try
            {
                EntitySelector entityselector = new EntitySelectorParser(new StringReader(selector), true).parse();
                Collection<? extends Entity > entities = entityselector.select(((CarpetContext)c).s);
                List<Value> retlist = new ArrayList<>();
                for (Entity e: entities)
                {
                    retlist.add(new EntityValue(e));
                }
                return (c_, t_) -> ListValue.wrap(retlist);
            }
            catch (CommandSyntaxException e)
            {
                throw new InternalExpressionException("Cannot select entities from "+selector);
            }
        });


        this.expr.addLazyFunction("query", -1, (c, t, lv) -> {
            if (lv.size()<2)
            {
                throw new InternalExpressionException("query_entity takes entity as a first argument, and queried feature as a second");
            }
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof EntityValue))
                throw new InternalExpressionException("First argument to query_entity should be an entity");
            String what = lv.get(1).evalValue(c).getString();
            if (lv.size()==2)
                return (_c, _t) -> ((EntityValue) v).get(what, null);
            if (lv.size()==3)
                return (_c, _t) -> ((EntityValue) v).get(what, lv.get(2).evalValue(c));
            return (_c, _t) -> ((EntityValue) v).get(what, ListValue.wrap(lv.subList(2, lv.size()).stream().map((vv) -> vv.evalValue(c)).collect(Collectors.toList())));

        });

        // or update
        this.expr.addLazyFunction("modify", -1, (c, t, lv) -> {
            if (lv.size()<2)
            {
                throw new InternalExpressionException("modify_entity takes entity as a first argument, and queried feature as a second");
            }
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof EntityValue))
                throw new InternalExpressionException("First argument to get should be an entity");
            String what = lv.get(1).evalValue(c).getString();
            if (lv.size()==2)
                ((EntityValue) v).set(what, null);
            else if (lv.size()==3)
                ((EntityValue) v).set(what, lv.get(2).evalValue(c));
            else
                ((EntityValue) v).set(what, ListValue.wrap(lv.subList(2, lv.size()).stream().map((vv) -> vv.evalValue(c)).collect(Collectors.toList())));
            return lv.get(0);
        });
    }

    /**
     * <h1>Iterating over larger areas of blocks</h1>
     * <p>Section Content</p>
     * <p>Other Paragraph</p>
     */

    public void IteratingOverAreasOfBlocks()
    {
        this.expr.addLazyFunction("scan", 7, (c, t, lv) ->
        {
            int cx = (int)Expression.getNumericValue(lv.get(0).evalValue(c)).getLong();
            int cy = (int)Expression.getNumericValue(lv.get(1).evalValue(c)).getLong();
            int cz = (int)Expression.getNumericValue(lv.get(2).evalValue(c)).getLong();
            int xrange = (int)Expression.getNumericValue(lv.get(3).evalValue(c)).getLong();
            int yrange = (int)Expression.getNumericValue(lv.get(4).evalValue(c)).getLong();
            int zrange = (int)Expression.getNumericValue(lv.get(5).evalValue(c)).getLong();
            LazyValue expr = lv.get(6);

            //saving outer scope
            LazyValue _x = c.getVariable("_x");
            LazyValue _y = c.getVariable("_y");
            LazyValue _z = c.getVariable("_z");
            LazyValue __ = c.getVariable("_");
            int sCount = 0;
            for (int y=cy-yrange; y <= cy+yrange; y++)
            {
                int yFinal = y;
                c.setVariable("_y", (c_, t_) -> new NumericValue(yFinal).bindTo("_y"));
                for (int x=cx-xrange; x <= cx+xrange; x++)
                {
                    int xFinal = x;
                    c.setVariable("_x", (c_, t_) -> new NumericValue(xFinal).bindTo("_x"));
                    for (int z=cz-zrange; z <= cz+zrange; z++)
                    {
                        int zFinal = z;
                        c.setVariable( "_", (cc_, t_c) -> BlockValue.fromCoords(((CarpetContext)c), xFinal,yFinal,zFinal).bindTo("_"));
                        c.setVariable("_z", (c_, t_) -> new NumericValue(zFinal).bindTo("_z"));
                        if (expr.evalValue(c).getBoolean())
                        {
                            sCount += 1;
                        }
                    }
                }
            }
            //restoring outer scope
            c.setVariable("_x", _x);
            c.setVariable("_y", _y);
            c.setVariable("_z", _z);
            c.setVariable("_", __);
            int finalSCount = sCount;
            return (c_, t_) -> new NumericValue(finalSCount);
        });

        this.expr.addLazyFunction("volume", 7, (c, t, lv) ->
        {
            int xi = (int)Expression.getNumericValue(lv.get(0).evalValue(c)).getLong();
            int yi = (int)Expression.getNumericValue(lv.get(1).evalValue(c)).getLong();
            int zi = (int)Expression.getNumericValue(lv.get(2).evalValue(c)).getLong();
            int xj = (int)Expression.getNumericValue(lv.get(3).evalValue(c)).getLong();
            int yj = (int)Expression.getNumericValue(lv.get(4).evalValue(c)).getLong();
            int zj = (int)Expression.getNumericValue(lv.get(5).evalValue(c)).getLong();
            int minx = min(xi, xj);
            int miny = min(yi, yj);
            int minz = min(zi, zj);
            int maxx = max(xi, xj);
            int maxy = max(yi, yj);
            int maxz = max(zi, zj);
            LazyValue expr = lv.get(6);

            //saving outer scope
            LazyValue _x = c.getVariable("_x");
            LazyValue _y = c.getVariable("_y");
            LazyValue _z = c.getVariable("_z");
            LazyValue __ = c.getVariable("_");
            int sCount = 0;
            for (int y=miny; y <= maxy; y++)
            {
                int yFinal = y;
                c.setVariable("_y", (c_, t_) -> new NumericValue(yFinal).bindTo("_y"));
                for (int x=minx; x <= maxx; x++)
                {
                    int xFinal = x;
                    c.setVariable("_x", (c_, t_) -> new NumericValue(xFinal).bindTo("_x"));
                    for (int z=minz; z <= maxz; z++)
                    {
                        int zFinal = z;
                        c.setVariable( "_", (cc_, t_c) -> BlockValue.fromCoords(((CarpetContext)c), xFinal,yFinal,zFinal).bindTo("_"));
                        c.setVariable("_z", (c_, t_) -> new NumericValue(zFinal).bindTo("_z"));
                        if (expr.evalValue(c).getBoolean())
                        {
                            sCount += 1;
                        }
                    }
                }
            }
            //restoring outer scope
            c.setVariable("_x", _x);
            c.setVariable("_y", _y);
            c.setVariable("_z", _z);
            c.setVariable("_", __);
            int finalSCount = sCount;
            return (c_, t_) -> new NumericValue(finalSCount);
        });

        this.expr.addLazyFunction("neighbours", -1, (c, t, lv)->
        {

            BlockPos center = BlockValue.fromParams((CarpetContext) c, lv,0).block.getPos();
            World world = ((CarpetContext) c).s.getWorld();

            List<Value> neighbours = new ArrayList<>();
            neighbours.add(new BlockValue(null, world, center.up()));
            neighbours.add(new BlockValue(null, world, center.down()));
            neighbours.add(new BlockValue(null, world, center.north()));
            neighbours.add(new BlockValue(null, world, center.south()));
            neighbours.add(new BlockValue(null, world, center.east()));
            neighbours.add(new BlockValue(null, world, center.west()));
            return (c_, t_) -> ListValue.wrap(neighbours);
        });

        this.expr.addLazyFunction("rect", -1, (c, t, lv)->
        {
            if (lv.size() != 3 && lv.size() != 6 && lv.size() != 9)
            {
                throw new InternalExpressionException("rectangular region should be specified with 3, 6, or 9 coordinates");
            }
            int cx;
            int cy;
            int cz;
            int sminx;
            int sminy;
            int sminz;
            int smaxx;
            int smaxy;
            int smaxz;
            try
            {
                cx = (int)((NumericValue) lv.get(0).evalValue(c)).getLong();
                cy = (int)((NumericValue) lv.get(1).evalValue(c)).getLong();
                cz = (int)((NumericValue) lv.get(2).evalValue(c)).getLong();
                if (lv.size()==3) // only done this way because of stupid Java lambda final reqs
                {
                    sminx = 1;
                    sminy = 1;
                    sminz = 1;
                    smaxx = 1;
                    smaxy = 1;
                    smaxz = 1;
                }
                else if (lv.size()==6)
                {
                    sminx = (int) ((NumericValue) lv.get(3).evalValue(c)).getLong();
                    sminy = (int) ((NumericValue) lv.get(4).evalValue(c)).getLong();
                    sminz = (int) ((NumericValue) lv.get(5).evalValue(c)).getLong();
                    smaxx = sminx;
                    smaxy = sminy;
                    smaxz = sminz;
                }
                else // size == 9
                {
                    sminx = (int) ((NumericValue) lv.get(3).evalValue(c)).getLong();
                    sminy = (int) ((NumericValue) lv.get(4).evalValue(c)).getLong();
                    sminz = (int) ((NumericValue) lv.get(5).evalValue(c)).getLong();
                    smaxx = (int)((NumericValue) lv.get(6).evalValue(c)).getLong();
                    smaxy = (int)((NumericValue) lv.get(7).evalValue(c)).getLong();
                    smaxz = (int)((NumericValue) lv.get(8).evalValue(c)).getLong();
                }
            }
            catch (ClassCastException exc)
            {
                throw new InternalExpressionException("Attempted to pass a non-number to rect");
            }
            CarpetContext cc = (CarpetContext)c;
            return (c_, t_) -> new LazyListValue()
            {
                int minx = cx-sminx;
                int miny = cy-sminy;
                int minz = cz-sminz;
                int maxx = cx+smaxx;
                int maxy = cy+smaxy;
                int maxz = cz+smaxz;
                int x;
                int y;
                int z;
                {
                    x = minx;
                    y = miny;
                    z = minz;
                }
                @Override
                public boolean hasNext()
                {
                    return y <= maxy;
                }

                @Override
                public Value next()
                {
                    Value r = BlockValue.fromCoords(cc, x,y,z);
                    //possibly reroll context
                    x++;
                    if (x > maxx)
                    {
                        x = minx;
                        z++;
                        if (z > maxz)
                        {
                            z = minz;
                            y++;
                            // hasNext should fail if we went over
                        }
                    }

                    return r;
                }

                @Override
                public void fatality()
                {
                    // possibly return original x, y, z
                }
            };
        });

        this.expr.addLazyFunction("diamond", -1, (c, t, lv)->
        {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() != 3 && lv.size() != 4 && lv.size() != 5)
            {
                throw new InternalExpressionException("diamond region should be specified with 3 to 5 coordinates");
            }

            int cx;
            int cy;
            int cz;
            int width;
            int height;
            try
            {
                cx = (int)((NumericValue) lv.get(0).evalValue(c)).getLong();
                cy = (int)((NumericValue) lv.get(1).evalValue(c)).getLong();
                cz = (int)((NumericValue) lv.get(2).evalValue(c)).getLong();
                if (lv.size()==3)
                {
                    return (_c, _t ) -> new ListValue(Arrays.asList(
                            BlockValue.fromCoords(cc, cx, cy-1, cz),
                            BlockValue.fromCoords(cc, cx, cy, cz),
                            BlockValue.fromCoords(cc, cx-1, cy, cz),
                            BlockValue.fromCoords(cc, cx, cy, cz-1),
                            BlockValue.fromCoords(cc, cx+1, cy, cz),
                            BlockValue.fromCoords(cc, cx, cy, cz+1),
                            BlockValue.fromCoords(cc, cx, cy+1, cz)
                    ));
                }
                else if (lv.size()==4)
                {
                    width = (int) ((NumericValue) lv.get(3).evalValue(c)).getLong();
                    height = 0;
                }
                else // size == 5
                {
                    width = (int) ((NumericValue) lv.get(3).evalValue(c)).getLong();
                    height = (int) ((NumericValue) lv.get(4).evalValue(c)).getLong();
                }
            }
            catch (ClassCastException exc)
            {
                throw new InternalExpressionException("Attempted to pass a non-number to diamond");
            }
            if (height == 0)
            {
                return (c_, t_) -> new LazyListValue()
                {
                    int curradius = 0;
                    int curpos = 0;
                    {

                    }
                    @Override
                    public boolean hasNext()
                    {
                        return curradius <= width;
                    }

                    @Override
                    public Value next()
                    {
                        if (curradius == 0)
                        {
                            curradius = 1;
                            return BlockValue.fromCoords(cc, cx, cy, cz);
                        }
                        // x = 3-|i-6|
                        // z = |( (i-3)%12-6|-3
                        Value block = BlockValue.fromCoords(cc, cx+(curradius-abs(curpos-2*curradius)), cy, cz-curradius+abs( abs(curpos-curradius)%(4*curradius) -2*curradius ));
                        curpos++;
                        if (curpos>=curradius*4)
                        {
                            curradius++;
                            curpos = 0;
                        }
                        return block;

                    }
                };
            }
            else
            {
                return (c_, t_) -> new LazyListValue()
                {
                    int curradius = 0;
                    int curpos = 0;
                    int curheight = -height;
                    @Override
                    public boolean hasNext()
                    {
                        return curheight <= height;
                    }

                    @Override
                    public Value next()
                    {
                        if (curheight == -height || curheight == height)
                        {
                            return BlockValue.fromCoords(cc, cx, cy+curheight++, cz);
                        }
                        if (curradius == 0)
                        {
                            curradius++;
                            return BlockValue.fromCoords(cc, cx, cy+curheight, cz);
                        }
                        // x = 3-|i-6|
                        // z = |( (i-3)%12-6|-3

                        Value block = BlockValue.fromCoords(cc, cx+(curradius-abs(curpos-2*curradius)), cy+curheight, cz-curradius+abs( abs(curpos-curradius)%(4*curradius) -2*curradius ));
                        curpos++;
                        if (curpos>=curradius*4)
                        {
                            curradius++;
                            curpos = 0;
                            if (curradius>width -abs(width*curheight/height))
                            {
                                curheight++;
                                curradius = 0;
                                curpos = 0;
                            }
                        }
                        return block;
                    }
                };

            }
        });
    }

    //TODO sounds
    /**
     * <h1>Auxiliary aspects</h1>
     * <p>Section Content</p>
     * <p>Other Paragraph</p>
     */

    public void AuxiliaryAspects()
    {
        this.expr.addLazyFunction("sound", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockValue.VectorLocator locator = BlockValue.locateVec(cc, lv, 0);
            ResourceLocation soundName = new ResourceLocation(lv.get(locator.offset).evalValue(c).getString());
            if (!(IRegistry.field_212633_v.func_212607_c(soundName)))
                throw new InternalExpressionException("No such sound: "+soundName.getPath());
            float volume = 1.0F;
            float pitch = 1.0F;
            if (lv.size() > 1+locator.offset)
            {
                volume = (float)Expression.getNumericValue(lv.get(1+locator.offset).evalValue(c)).getDouble();
                if (lv.size() > 2+locator.offset)
                {
                    pitch = (float)Expression.getNumericValue(lv.get(2+locator.offset).evalValue(c)).getDouble();
                }
            }
            Vec3d vec = locator.vec;
            double d0 = Math.pow(volume > 1.0F ? (double)(volume * 16.0F) : 16.0D, 2.0D);
            int count = 0;
            for (EntityPlayerMP player : cc.s.getWorld().getPlayers(EntityPlayerMP.class, (p) -> p.getDistanceSq(vec) < d0))
            {
                count++;
                player.connection.sendPacket(new SPacketCustomSound(soundName, SoundCategory.PLAYERS, vec, volume, pitch));
            }
            int totalPlayed = count;
            return (_c, _t) -> new NumericValue(totalPlayed);
        });
        //particle(x,y,z,"particle",count?10, duration,bool all)
        this.expr.addLazyFunction("particle", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            MinecraftServer ms = cc.s.getServer();
            WorldServer world = cc.s.getWorld();
            BlockValue.VectorLocator locator = BlockValue.locateVec(cc, lv, 0);
            String particleName = lv.get(locator.offset).evalValue(c).getString();
            int count = 10;
            double speed = 0;
            float spread = 0.5f;
            EntityPlayerMP player = null;
            if (lv.size() > 1+locator.offset)
            {
                count = (int)Expression.getNumericValue(lv.get(1+locator.offset).evalValue(c)).getLong();
                if (lv.size() > 2+locator.offset)
                {
                    spread = (float)Expression.getNumericValue(lv.get(2+locator.offset).evalValue(c)).getDouble();
                    if (lv.size() > 3+locator.offset)
                    {
                        speed = Expression.getNumericValue(lv.get(3 + locator.offset).evalValue(c)).getDouble();
                        if (lv.size() > 4 + locator.offset) // should accept entity as well as long as it is player
                        {
                            player = ms.getPlayerList().getPlayerByUsername(lv.get(4 + locator.offset).evalValue(c).getString());
                        }
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
                throw new InternalExpressionException("No such particle: "+particleName);
            }
            Vec3d vec = locator.vec;
            if (player == null)
            {
                for (EntityPlayerMP p : (ms.getPlayerList().getPlayers()))
                {
                    world.spawnParticle(p, particle, true, vec.x, vec.y, vec.z, count,
                            spread, spread, spread, speed);
                }
            }
            else
            {
                world.spawnParticle(player,
                        particle, true, vec.x, vec.y, vec.z, count,
                        spread, spread, spread, speed);
            }

            return (c_, t_) -> Value.TRUE;
        });

        //"overriden" native call that prints to stderr
        this.expr.addLazyFunction("print", 1, (c, t, lv) ->
        {
            Messenger.m(((CarpetContext)c).s, "w " + lv.get(0).evalValue(c).getString());
            return lv.get(0); // pass through for variables
        });


        this.expr.addLazyFunction("run", 1, (c, t, lv) -> {
            BlockPos target = BlockValue.locateBlockPos((CarpetContext) c,
                    (int)Expression.getNumericValue(c.getVariable("x").evalValue(c)).getLong(),
                    (int)Expression.getNumericValue(c.getVariable("y").evalValue(c)).getLong(),
                    (int)Expression.getNumericValue(c.getVariable("z").evalValue(c)).getLong()
            );
            Vec3d posf = new Vec3d((double)target.getX()+0.5D,(double)target.getY(),(double)target.getZ()+0.5D);
            CommandSource s = ((CarpetContext)c).s;
            return (c_, t_) -> new NumericValue(s.getServer().getCommandManager().handleCommand(
                    s.withPos(posf).withFeedbackDisabled(), lv.get(0).evalValue(c).getString()));
        });

        this.expr.addLazyFunction("save", 0, (c, t, lv) -> {
            CommandSource s = ((CarpetContext)c).s;
            s.getServer().getPlayerList().saveAllPlayerData();
            boolean saving = s.getWorld().disableLevelSaving;
            s.getWorld().disableLevelSaving = false;
            try
            {
                s.getWorld().saveAllChunks(true,null);
            }
            catch (SessionLockException ignored) { }
            s.getWorld().getChunkProvider().tick(() -> true);
            s.getWorld().getChunkProvider().flushToDisk();
            s.getWorld().disableLevelSaving = saving;
            CarpetSettings.LOG.warn("Saved chunks");
            return (cc, tt) -> Value.TRUE;
        });

        this.expr.addLazyFunction("ticktime", 0, (c, t, lv) -> (cc, tt) -> new NumericValue(((CarpetContext)cc).s.getServer().getTickCounter()));
        this.expr.addLazyFunction("ticktime2", 0, (c, t, lv) -> (cc, tt) -> new NumericValue(((CarpetContext)cc).s.getServer().getTickCounter()));

        this.expr.addLazyFunction("ticktime3", 0, (c, t, lv) ->
        {
            Value time = new NumericValue(((CarpetContext) c).s.getServer().getTickCounter());
            return (cc, tt) -> time;
        });

        this.expr.addLazyFunction("gametick", -1, (c, t, lv) -> {
            CommandSource s = ((CarpetContext)c).s;
            s.getServer().tick( () -> System.nanoTime()-tickStart<50000000L);
            s.getServer().dontPanic(); // optional not to freak out the watchdog
            if (lv.size()>0)
            {
                long ms_total = Expression.getNumericValue(lv.get(0).evalValue(c)).getLong();
                long end_expected = tickStart+ms_total*1000000L;
                long wait = end_expected-System.nanoTime();
                if (wait > 0L)
                {
                    try
                    {
                        Thread.sleep(wait/1000000L);
                    }
                    catch (InterruptedException ignored)
                    {
                    }
                }
            }
            tickStart = System.nanoTime(); // for the next tick
            Thread.yield();
            if(stopAll)
                throw new Expression.ExitStatement(Value.NULL);
            return (cc, tt) -> Value.TRUE;
        });

        //not ready yet
        this.expr.addLazyFunction("plop", 4, (c, t, lv) ->{
            BlockValue.LocatorResult locator = BlockValue.fromParams((CarpetContext)c, lv, 0);
            Boolean res = FeatureGenerator.spawn(lv.get(locator.offset).evalValue(c).getString(), ((CarpetContext)c).s.getWorld(), locator.block.getPos());
            if (res == null)
                return (c_, t_) -> Value.NULL;
            return (c_, t_) -> new NumericValue(res);
        });
    }

    /**
     * <h1>Minecraft API</h1>
     * <p>Here is the gist of the Minecraft related functions. Otherwise the CarpetScript could live without Minecraft.</p>
     * @param expression expression
     * @param source source
     * @param origin origin
     */
    static
    {
        Expression.globalVariables.put("_x", (c, t) -> new NumericValue(0).bindTo("_x"));
        Expression.globalVariables.put("_y", (c, t) -> new NumericValue(0).bindTo("_y"));
        Expression.globalVariables.put("_z", (c, t) -> new NumericValue(0).bindTo("_z"));
    }
    public CarpetExpression(String expression, CommandSource source, BlockPos origin)
    {
        this.origin = origin;
        this.source = source;
        this.expr = new Expression(expression);

        BlockManipulation();
        EntityManipulation();
        IteratingOverAreasOfBlocks();
        AuxiliaryAspects();
    }

    /**
     * <h1>fill and scan commands</h1>
     * <p>Section Content</p>
     * <p>Other Paragraph</p>
     * @param x .
     * @param y .
     * @param z .
     * @return .
     */
    public boolean fillAndScanCommand(int x, int y, int z)
    {
        try
        {
            Context context = new CarpetContext(this.expr, source, origin).
                    with("x", (c, t) -> new NumericValue(x - origin.getX())).
                    with("y", (c, t) -> new NumericValue(y - origin.getY())).
                    with("z", (c, t) -> new NumericValue(z - origin.getZ()));
            Entity e = source.getEntity();
            if (e==null)
            {
                context.with("p", LazyValue.NULL );
            }
            else
            {
                context.with("p", (cc, tt) -> new EntityValue(e));
            }
            return this.expr.eval(context).getBoolean();
        }
        catch (ExpressionException e)
        {
            throw new ExpressionInspector.CarpetExpressionException(e.getMessage());
        }
        catch (ArithmeticException ae)
        {
            throw new ExpressionInspector.CarpetExpressionException("math doesn't compute... "+ae.getMessage());
        }
    }

    /**
     * <h1>script run command</h1>
     * <p>Section Content</p>
     * <p>Other Paragraph</p>
     * @param pos .
     * @return .
     */
    public String scriptRunCommand(BlockPos pos)
    {
        try
        {
            Context context = new CarpetContext(this.expr, source, origin).
                    with("x", (c, t) -> new NumericValue(pos.getX() - origin.getX()).bindTo("x")).
                    with("y", (c, t) -> new NumericValue(pos.getY() - origin.getY()).bindTo("y")).
                    with("z", (c, t) -> new NumericValue(pos.getZ() - origin.getZ()).bindTo("z"));
            Entity e = source.getEntity();
            if (e==null)
            {
                context.with("p", (cc, tt) -> Value.NULL.reboundedTo("p") );
            }
            else
            {
                context.with("p", (cc, tt) -> new EntityValue(e).bindTo("p"));
            }
            return this.expr.eval(context).getString();
        }
        catch (ExpressionException e)
        {
            throw new ExpressionInspector.CarpetExpressionException(e.getMessage());
        }
        catch (ArithmeticException ae)
        {
            throw new ExpressionInspector.CarpetExpressionException("math doesn't compute... "+ae.getMessage());
        }
    }

    /**
     * <h1> script invoke / invokepoint / invokearea commands</h1>
     * <p>Section Content</p>
     * <p>Other Paragraph</p>
     * @param source .
     * @param call .
     * @param argv .
     * @return .
     */
    public static String invokeGlobalFunctionCommand(CommandSource source, String call, List<String> argv)
    {
        Expression.UserDefinedFunction acf = Expression.globalFunctions.get(call);
        if (acf == null)
            return "UNDEFINED";
        List<String> args = acf.getArguments();
        if (argv.size() != args.size())
        {
            return "Fail: stored function "+call+" takes "+args.size()+" arguments, not "+argv.size()+
                  ": "+String.join(", ", args);
        }
        try
        {
            Vec3d pos = source.getPos();
            Expression expr = new Expression(call+"("+String.join(" , ",argv)+" ) ").withName(call);
            Context context = new CarpetContext(expr, source, BlockPos.ORIGIN).
                    with("x", (c, t) -> new NumericValue(Math.round(pos.x))).
                    with("y", (c, t) -> new NumericValue(Math.round(pos.y))).
                    with("z", (c, t) -> new NumericValue(Math.round(pos.z)));
            Entity e = source.getEntity();
            if (e==null)
            {
                context.with("p", LazyValue.NULL );
            }
            else
            {
                context.with("p", (cc, tt) -> new EntityValue(e));
            }
            return expr.eval(context).getString();
        }
        catch (ExpressionException e)
        {
             return e.getMessage();
        }
        catch (ArithmeticException ae)
        {
            throw new ExpressionInspector.CarpetExpressionException("math doesn't compute... "+ae.getMessage());
        }

    }

    void setLogOutput(boolean to)
    {
        this.expr.setLogOutput(to ? (s) -> Messenger.m(source, "gi " + s) : null);
    }
    static void setChatErrorSnooper(CommandSource source)
    {
        ExpressionException.errorSnooper = (expr, token, message) ->
        {
            try
            {
                source.asPlayer();
            }
            catch (CommandSyntaxException e)
            {
                return null;
            }
            String[] lines = expr.getCodeString().split("\n");

            String shebang = message;

            if (lines.length > 1)
            {
                shebang += " at line "+(token.lineno+1)+", pos "+(token.linepos+1);
            }
            else
            {
                shebang += " at pos "+(token.pos+1);
            }
            if (expr.getName() != null)
            {
                shebang += " in "+expr.getName()+"";
            }
            Messenger.m(source, "r "+shebang);

            if (lines.length > 1 && token.lineno > 0)
            {
                Messenger.m(source, "l "+lines[token.lineno-1]);
            }
            Messenger.m(source, "l "+lines[token.lineno].substring(0, token.linepos), "r  HERE>> ", "l "+
                    lines[token.lineno].substring(token.linepos));

            if (lines.length > 1 && token.lineno < lines.length-1)
            {
                Messenger.m(source, "l "+lines[token.lineno+1]);
            }
            return new ArrayList<>();
        };
    }
    static void resetErrorSnooper()
    {
        ExpressionException.errorSnooper=null;
    }
}
