package ocd.mcoptimizations.shapecast;

import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Predicate;

public abstract class LazyShapecaster implements IShapecaster
{
    protected final @Nullable
    Entity entity;
    protected final Set<Entity> ignoredEntities;
    protected final AxisAlignedBB box;
    protected final @Nullable Predicate<VoxelShape> filter;

    protected LazyShapecaster(@Nullable final Entity entity, final Set<Entity> ignoredEntities, final AxisAlignedBB box, @Nullable final Predicate<VoxelShape> filter)
    {
        this.entity = entity;
        this.ignoredEntities = ignoredEntities;
        this.box = box;
        this.filter = filter;
    }

    protected abstract double shapecast(@Nullable Entity entity, Set<Entity> ignoredEntities, AxisAlignedBB shape, Axis axis, double maxDist, @Nullable Predicate<VoxelShape> filter);

    protected abstract boolean isEmpty(@Nullable Entity entity, Set<Entity> ignoredEntities, AxisAlignedBB box, @Nullable Predicate<VoxelShape> filter);

    @Override
    public double shapecast(final AxisAlignedBB shape, final Axis axis, final double maxDist, @Nullable final Predicate<VoxelShape> filter)
    {
        if (Math.abs(maxDist) < 1E-7)
            return 0.;

        return this.shapecast(this.entity, this.ignoredEntities, shape, axis, maxDist, this.filter == null ? filter : filter == null ? this.filter : this.filter.and(filter));
    }

    @Override
    public boolean isEmpty()
    {
        return this.isEmpty(this.entity, this.ignoredEntities, this.box, this.filter);
    }

    @Override
    public IShapecaster createShapecaster(final AxisAlignedBB box, @Nullable final Predicate<VoxelShape> filter)
    {
        return new LazyShapecaster(this.entity, this.ignoredEntities, this.box.intersect(box), this.filter == null ? filter : filter == null ? this.filter : this.filter.and(filter))
        {
            @Override
            protected double shapecast(@Nullable final Entity entity, final Set<Entity> ignoredEntities, final AxisAlignedBB shape, final Axis axis, final double maxDist, @Nullable final Predicate<VoxelShape> filter)
            {
                return LazyShapecaster.this.shapecast(entity, ignoredEntities, shape, axis, maxDist, filter);
            }

            @Override
            protected boolean isEmpty(@Nullable final Entity entity, final Set<Entity> ignoredEntities, final AxisAlignedBB box, @Nullable final Predicate<VoxelShape> filter)
            {
                return LazyShapecaster.this.isEmpty(entity, ignoredEntities, box, filter);
            }
        };
    }
}