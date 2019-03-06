package ocd.mcoptimizations.voxel;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.AxisRotation;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.*;

public class VoxelShapeAABB extends VoxelShape
{
    private static final VoxelShapePart shapePart;

    static
    {
        shapePart = new VoxelShapePartBitSet(1, 1, 1);
        shapePart.setFilled(0, 0,0, true, true);
    }

    protected final AxisAlignedBB box;

    public VoxelShapeAABB(final AxisAlignedBB box)
    {
        super(shapePart);
        this.box = box;
    }

    @Override
    public double getStart(final Axis axis) { return this.box.getMin(axis); }

    @Override
    public double getEnd(final Axis axis) { return this.box.getMax(axis); }

    @Override
    public AxisAlignedBB getBoundingBox() { return this.box; }

    @Override
    protected double getValueUnchecked(final Axis axis, final int regionIndex)
    {
        if (regionIndex == 0)
            return this.getStart(axis);
        else if (regionIndex == 1)
            return this.getEnd(axis);
        else
            throw new IndexOutOfBoundsException("AABB voxel shape has no region with index " + regionIndex);
    }

    @Override
    protected DoubleList getValues(Axis axis) {
        return DoubleArrayList.wrap(new double[] { this.getStart(axis), this.getEnd(axis) });
    }

    public static boolean isEmpty(final AxisAlignedBB box)
    {
        return box.minX + 1E-7 > box.maxX || box.minY + 1E-7 > box.maxY || box.minZ + 1E-7 > box.maxZ;
    }

    @Override
    public boolean isEmpty() { return isEmpty(this.box); }

    @Override
    public VoxelShape offset(final double xOffset, final double yOffset, final double zOffset)
    {
        return new VoxelShapeAABB(this.box.offset(xOffset, yOffset, zOffset));
    }

    @Override
    protected int getClosestIndex(final Axis axis, final double position)
    {
        return position < this.getStart(axis) ? -1 : position < this.getEnd(axis) ? 0 : 1;
    }

    @Override
    protected boolean contains(double x, double y, double z)
    {
        return this.box.contains(x, y, z);
    }

    @Override
    protected double func_212431_a(AxisRotation axisRotation, final AxisAlignedBB shape, final double maxDist)
    {
        if (Math.abs(maxDist) < 1E-7)
            return 0.;

        if (this.isEmpty())
            return maxDist;

        double d;

        axisRotation = axisRotation.reverse();
        final Axis axis = axisRotation.rotate(Axis.X);

        if (maxDist > 0.)
        {
            d = this.getStart(axis) - shape.getMax(axis);

            if (d < -1E-7 || maxDist < d)
                return maxDist;
            else if (d < 1E-7)
                d = 0.;
        }
        else
        {
            d = this.getEnd(axis) - shape.getMin(axis);

            if (d > 1E-7 || maxDist > d)
                return maxDist;
            else if (d > -1E-7)
                d = 0.;
        }

        final Axis axis2 = axisRotation.rotate(Axis.Y);
        final Axis axis3 = axisRotation.rotate(Axis.Z);

        return this.getStart(axis2) + 1E-7 < shape.getMax(axis2) &&
                shape.getMin(axis2) + 1E-7 < this.getEnd(axis2) &&
                this.getStart(axis3) + 1E-7 < shape.getMax(axis3) &&
                shape.getMin(axis3) + 1E-7 < this.getEnd(axis3) ?
                d : maxDist;
    }

    @Override
    public boolean compare(final VoxelShape shape, final IBooleanFunction desc)
    {
        return shape.compare(this.box, desc.swapArgs());
    }

    @Override
    public boolean compare(final AxisAlignedBB shape, final IBooleanFunction desc)
    {
        if (desc.apply(false, false))
            throw new IllegalArgumentException();

        if (this.isEmpty())
            return desc.apply(false, !isEmpty(shape));

        if (isEmpty(shape))
            return desc.apply(!this.isEmpty(), false);

        return (desc.apply(true, true) && this.intersects(shape)) ||
                (desc.apply(false, true) && (shape.minX + 1E-7 < this.box.minX || shape.maxX > this.box.maxX + 1E-7 || shape.minY + 1E-7 < this.box.minY || shape.maxY > this.box.maxY + 1E-7 || shape.minZ + 1E-7 < this.box.minZ || shape.maxZ > this.box.maxZ + 1E-7)) ||
                (desc.apply(true, false) && (this.box.minX + 1E-7 < shape.minX || this.box.maxX > shape.maxX + 1E-7 || this.box.minY + 1E-7 < shape.minY || this.box.maxY > shape.maxY + 1E-7 || this.box.minZ + 1E-7 < shape.minZ || this.box.maxZ > shape.maxZ + 1E-7));
    }

    @Override
    public boolean intersects(final VoxelShape shape)
    {
        return shape.intersects(this.box);
    }

    @Override
    public boolean intersects(final AxisAlignedBB shape)
    {
        return this.box.minX + 1E-7 < shape.maxX && this.box.maxX > shape.minX + 1E-7 && this.box.minY + 1E-7 < shape.maxY && this.box.maxY > shape.minY + 1E-7 && this.box.minZ + 1E-7 < shape.maxZ && this.box.maxZ > shape.minZ + 1E-7;
    }

    @Override
    public VoxelShape persistent()
    {
        return VoxelShapes.create(this.box);
    }

    public static class Cached extends VoxelShapeAABB
    {
        private final DoubleList xPoints;
        private final DoubleList yPoints;
        private final DoubleList zPoints;

        public Cached(final AxisAlignedBB box)
        {
            super(box);

            this.xPoints = DoubleArrayList.wrap(new double[] {box.minX, box.maxX});
            this.yPoints = DoubleArrayList.wrap(new double[] {box.minY, box.maxY});
            this.zPoints = DoubleArrayList.wrap(new double[] {box.minZ, box.maxZ});
        }

        @Override
        protected DoubleList getValues(final Axis axis)
        {
            switch(axis)
            {
                case X:
                    return this.xPoints;
                case Y:
                    return this.yPoints;
                case Z:
                    return this.zPoints;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public VoxelShape persistent()
        {
            return this;
        }
    }
}
