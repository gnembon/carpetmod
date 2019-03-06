package ocd.mcoptimizations.shapecast;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public interface IShapecaster
{
    double shapecast(AxisAlignedBB shape, EnumFacing.Axis axis, double maxDist, @Nullable Predicate<VoxelShape> filter);

    boolean isEmpty();

    default double shapecast(AxisAlignedBB shape, EnumFacing.Axis axis, double maxDist)
    {
        return this.shapecast(shape, axis, maxDist, null);
    }

    IShapecaster createShapecaster(final AxisAlignedBB box, @Nullable final Predicate<VoxelShape> filter);

    static IShapecaster combine(final IShapecaster shapecaster1, final IShapecaster shapecaster2)
    {
        if (shapecaster1 == EmptyShapecaster.INSTANCE)
            return shapecaster2;

        if (shapecaster2 == EmptyShapecaster.INSTANCE)
            return shapecaster1;

        return new IShapecaster()
        {
            @Override
            public double shapecast(AxisAlignedBB shape, Axis axis, double maxDist, @Nullable Predicate<VoxelShape> filter)
            {
                return shapecaster2.shapecast(shape, axis, shapecaster1.shapecast(shape, axis, maxDist, filter), filter);
            }

            @Override
            public boolean isEmpty()
            {
                return shapecaster1.isEmpty() && shapecaster2.isEmpty();
            }

            @Override
            public double shapecast(final AxisAlignedBB shape, final Axis axis, final double maxDist)
            {
                return shapecaster2.shapecast(shape, axis, shapecaster1.shapecast(shape, axis, maxDist));
            }

            @Override
            public IShapecaster createShapecaster(final AxisAlignedBB box, @Nullable final Predicate<VoxelShape> filter)
            {
                return combine(shapecaster1.createShapecaster(box, filter), shapecaster2.createShapecaster(box, filter));
            }
        };
    }
}