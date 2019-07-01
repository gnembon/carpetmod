package carpet.script.value;

import carpet.script.CarpetContext;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.arguments.ItemInput;
import net.minecraft.command.arguments.ItemParser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class NBTSerializableValue extends Value
{
    private String nbtString = null;
    private NBTTagCompound nbtTag = null;
    private Supplier<NBTTagCompound> nbtSupplier = null;

    public NBTSerializableValue(ItemStack stack)
    {
        nbtSupplier = () -> stack.hasTag() ? stack.getTag(): new NBTTagCompound();
    }

    public NBTSerializableValue(String nbtString)
    {
        nbtSupplier = () ->
        {
            try
            {
                return (new JsonToNBT(new StringReader(nbtString))).readStruct();
            }
            catch (CommandSyntaxException e)
            {
                throw new InternalExpressionException("Incorrect nbt data: "+nbtString);
            }
        };
    }

    public NBTSerializableValue(NBTTagCompound tag)
    {
        nbtTag = tag;
    }

    public static InventoryLocator locateInventory(CarpetContext c, List<LazyValue> params, int offset)
    {
        try
        {
            IInventory inv = null;
            Value v1 = params.get(0 + offset).evalValue(c);
            if (v1 instanceof EntityValue)
            {
                Entity e = ((EntityValue) v1).getEntity();
                if (e instanceof EntityPlayer) inv = ((EntityPlayer) e).inventory;
                else if (e instanceof IInventory) inv = (IInventory) e;
                else if (e instanceof EntityVillager) inv = ((EntityVillager) e).getVillagerInventory();

                if (inv == null)
                    return null;

                return new InventoryLocator(e, e.getPosition(), inv, offset + 1);
            }
            else if (v1 instanceof BlockValue)
            {
                BlockPos pos = ((BlockValue) v1).getPos();
                if (pos == null)
                    throw new InternalExpressionException("Block to access inventory needs to be positioned in the world");
                inv = TileEntityHopper.getInventoryAtPosition(c.s.getWorld(), pos);
                if (inv == null)
                    return null;
                return new InventoryLocator(pos, pos, inv, offset + 1);
            }
            else if (v1 instanceof ListValue)
            {
                List<Value> args = ((ListValue) v1).getItems();
                BlockPos pos = new BlockPos(
                        NumericValue.asNumber(args.get(0)).getDouble(),
                        NumericValue.asNumber(args.get(1)).getDouble(),
                        NumericValue.asNumber(args.get(2)).getDouble());
                inv = TileEntityHopper.getInventoryAtPosition(c.s.getWorld(), pos);
                if (inv == null)
                    return null;
                return new InventoryLocator(pos, pos, inv, offset + 1);
            }
            BlockPos pos = new BlockPos(
                    NumericValue.asNumber(v1).getDouble(),
                    NumericValue.asNumber(params.get(1 + offset).evalValue(c)).getDouble(),
                    NumericValue.asNumber(params.get(2 + offset).evalValue(c)).getDouble());
            inv = TileEntityHopper.getInventoryAtPosition(c.s.getWorld(), pos);
            if (inv == null)
                return null;
            return new InventoryLocator(pos, pos, inv, offset + 3);
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new InternalExpressionException("Inventory should be defined either by three coordinates, a block value, or an entity");
        }
    }

    private static Map<String,ItemInput> itemCache = new HashMap<>();

    public static ItemInput parseItem(String itemString)
    {
        return parseItem(itemString, null);
    }

    public static ItemInput parseItem(String itemString, NBTTagCompound customTag)
    {
        try
        {
            ItemInput res = itemCache.get(itemString);
            if (res != null)
                if (customTag == null)
                    return res;
                else
                    return new ItemInput(res.getItem(), customTag);

            ItemParser parser = (new ItemParser(new StringReader(itemString), false)).parse();
            res = new ItemInput(parser.getItem(), parser.getNbt());
            itemCache.put(itemString, res);
            if (itemCache.size()>64000)
                itemCache.clear();
            if (customTag == null)
                return res;
            else
                return new ItemInput(res.getItem(), customTag);
        }
        catch (CommandSyntaxException e)
        {
            throw new InternalExpressionException("Incorrect item: "+itemString);
        }
    }

    public static int validateSlot(int slot, IInventory inv)
    {
        int invSize = inv.getSizeInventory();
        if (slot < 0)
            slot = invSize + slot;
        if (slot < 0 || slot >= invSize)
            return inv.getSizeInventory(); // outside of inventory
        return slot;
    }

    public NBTTagCompound getTag()
    {
        if (nbtTag == null)
            nbtTag = nbtSupplier.get();
        return nbtTag;
    }

    @Override
    public boolean equals(final Value o)
    {
        if (o instanceof NBTSerializableValue)
            return getTag().equals(((NBTSerializableValue) o).getTag());
        return super.equals(o);
    }

    @Override
    public String getString()
    {
        if (nbtString == null)
            nbtString = getTag().toString();
        return nbtString;
    }

    @Override
    public boolean getBoolean()
    {
        return true;
    }

    public static class InventoryLocator
    {
        public Object owner;
        public BlockPos position;
        public IInventory inventory;
        public int offset;
        InventoryLocator(Object owner, BlockPos pos, IInventory i, int o)
        {
            this.owner = owner;
            position = pos;
            inventory = i;
            offset = o;
        }

        public boolean hasEnough(Item item_1, int requested) {
            int int_1 = 0;

            for(int int_2 = 0; int_2 < inventory.getSizeInventory(); ++int_2) {
                ItemStack itemStack_1 = inventory.getStackInSlot(int_2);
                if (itemStack_1.getItem().equals(item_1)) {
                    int current = itemStack_1.getCount();
                    requested -= current;
                    if (requested <=0)
                        return true;
                }
            }
            return false;
        }

    }


}
