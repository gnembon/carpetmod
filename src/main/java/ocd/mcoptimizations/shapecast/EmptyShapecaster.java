package ocd.mcoptimizations.shapecast;

import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class EmptyShapecaster implements IShapecaster
{
    public static final IShapecaster INSTANCE = new EmptyShapecaster();

    @Override
    public double shapecast(final AxisAlignedBB shape, final Axis axis, final double maxDist, @Nullable final Predicate<VoxelShape> filter)
    {
        return Math.abs(maxDist) < 1E-7 ? 0. : maxDist;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public IShapecaster createShapecaster(final AxisAlignedBB box, @Nullable final Predicate<VoxelShape> filter)
    {
        return this;
    }
}