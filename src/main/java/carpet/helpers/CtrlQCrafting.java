package carpet.helpers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.List;

public class CtrlQCrafting {

    public static ItemStack dropAllCrafting(EntityPlayer playerIn, int index, List<Slot> invSlots)
    {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = invSlots.get(index);

        if (slot != null && slot.getHasStack())
        {
            ItemStack itemStack1 = slot.getStack();
            itemStack = itemStack1.copy();

            if (index == 0)
            {
                playerIn.dropItem(itemStack, true);
                itemStack1.setCount(0);
                slot.onSlotChange(itemStack1, itemStack);
            }

            if (itemStack.getCount() == itemStack1.getCount())
            {
                return ItemStack.EMPTY;
            }

            ItemStack itemStack2 = slot.onTake(playerIn, itemStack1);

            if (index == 0)
            {
                playerIn.dropItem(itemStack2, false);
            }

        }
        return itemStack;
    }
}
