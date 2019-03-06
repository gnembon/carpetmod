package ocd.mcoptimizations.shapecast;

import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

public class CollectionShapecaster implements IShapecaster
{
    private final Collection<VoxelShape> shapes;

    public CollectionShapecaster(final Collection<VoxelShape> shapes)
    {
        this.shapes = shapes;
    }

    @Override
    public double shapecast(final AxisAlignedBB shape, final Axis axis, double maxDist, @Nullable final Predicate<VoxelShape> filter)
    {
        if (Math.abs(maxDist) < 1E-7)
            return 0.;

        for (VoxelShape shape_ : shapes) {
            if (filter == null || filter.test(shape_)) {
                maxDist = shape_.func_212430_a(axis, shape, maxDist);

                if (Math.abs(maxDist) < 1E-7)
                    return 0.;
            }
        }

        return maxDist;
    }

    @Override
    public boolean isEmpty()
    {
        for (VoxelShape shape : this.shapes)
        {
            if (!shape.isEmpty())
                return false;
        }

        return true;
    }

    @Override
    public IShapecaster createShapecaster(final AxisAlignedBB box, @Nullable final Predicate<VoxelShape> filter)
    {
        final ArrayList<VoxelShape> shapes = new ArrayList<>(this.shapes.size());

        for (VoxelShape shape_ : this.shapes)
        {
            if ((filter == null || filter.test(shape_)) && shape_.intersects(box))
                shapes.add(shape_);
        }

        return create(shapes);
    }

    public static IShapecaster create(final Collection<VoxelShape> shapes)
    {
        if (shapes.isEmpty())
            return EmptyShapecaster.INSTANCE;

        return new CollectionShapecaster(shapes);
    }
}