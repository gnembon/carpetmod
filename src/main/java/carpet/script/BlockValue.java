package carpet.script;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.world.World;

import java.util.List;

public class BlockValue extends Value
{
    public static final BlockValue AIR = new BlockValue(Blocks.AIR.getDefaultState(), null, BlockPos.ORIGIN);
    public static final BlockValue NULL = new BlockValue(null, null, null);
    private IBlockState blockState;
    private BlockPos pos;
    private World world;

    public static BlockValue fromCoords(CarpetExpression.CarpetContext c, int x, int y, int z)
    {
        BlockPos pos = locateBlockPos(c, x,y,z);
        return new BlockValue(null, c.s.getWorld(), pos);
    }

    public static BlockValue fromString(String str)
    {
        try
        {
            ResourceLocation blockId = ResourceLocation.read(new StringReader(str));
            if (IRegistry.field_212618_g.func_212607_c(blockId))
            {

                Block block = IRegistry.field_212618_g.get(blockId);
                return new BlockValue(block.getDefaultState(), null, null);
            }
        }
        catch (CommandSyntaxException ignored)
        {
        }
        return NULL;
    }

    public static VectorLocator locateVec(CarpetExpression.CarpetContext c, List<LazyValue> params, int offset)
    {
        try
        {
            Value v1 = params.get(0 + offset).evalValue(c);
            if (v1 instanceof BlockValue)
            {
                return new VectorLocator(new Vec3d(((BlockValue) v1).getPos()).add(0.5,0.5,0.5), 1+offset);
            }
            return new VectorLocator( new Vec3d(
                    Expression.getNumericValue(v1).getDouble(),
                    Expression.getNumericValue(params.get(1 + offset).evalValue(c)).getDouble(),
                    Expression.getNumericValue(params.get(2 + offset).evalValue(c)).getDouble()),
                    offset+3
            );
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new Expression.InternalExpressionException("position should be defined either by three coordinates, or a block value");
        }
    }

    public static BlockPos locateBlockPos(CarpetExpression.CarpetContext c, int xpos, int ypos, int zpos)
    {
        return new BlockPos(c.origin.getX() + xpos, c.origin.getY() + ypos, c.origin.getZ() + zpos);
    }

    public static LocatorResult fromParams(CarpetExpression.CarpetContext c, List<LazyValue> params, int offset)
    {
        try
        {
            Value v1 = params.get(0 + offset).evalValue(c);
            if (v1 instanceof BlockValue)
            {
                return new LocatorResult(((BlockValue) v1), 1+offset);
            }
            int xpos = (int) ((NumericValue) v1).getLong();
            int ypos = (int) ((NumericValue) params.get(1 + offset).evalValue(c)).getLong();
            int zpos = (int) ((NumericValue) params.get(2 + offset).evalValue(c)).getLong();
            return new LocatorResult(
                    new BlockValue(
                            null,
                            c.s.getWorld(),
                            new BlockPos(c.origin.getX() + xpos, c.origin.getY() + ypos, c.origin.getZ() + zpos)
                    ),
                    3+offset
            );
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new Expression.InternalExpressionException("position should be defined either by three coordinates, or a block value");
        }
    }

    public IBlockState getBlockState()
    {
        if (blockState != null)
        {
            return blockState;
        }
        if (world != null && pos != null)
        {
            blockState = world.getBlockState(pos);
            return blockState;
        }
        throw new Expression.InternalExpressionException("Attemted to fetch blockstate without world or stored blockstate");
    }

    BlockValue(IBlockState state, World world, BlockPos position)
    {
        this.world = world;
        blockState = state;
        pos = position;
    }


    @Override
    public String getString()
    {
        return IRegistry.field_212618_g.getKey(getBlockState().getBlock()).getPath();
    }

    @Override
    public boolean getBoolean()
    {
        return this != NULL && !getBlockState().isAir();
    }

    @Override
    public Value clone()
    {
        return new BlockValue(blockState, world, pos);
    }

    public BlockPos getPos()
    {
        return pos;
    }


    public static class LocatorResult
    {
        BlockValue block;
        int offset;
        LocatorResult(BlockValue b, int o)
        {
            block = b;
            offset = o;
        }
    }

    public static class VectorLocator
    {
        Vec3d vec;
        int offset;
        VectorLocator(Vec3d v, int o)
        {
            vec = v;
            offset = o;
        }
    }
}
