package carpet.helpers;

import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.server.SPacketCustomSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.registry.IRegistry;

public class XPcombine
{
    public static void searchForOtherXPNearbyCarpet(EntityXPOrb first)
    {
        for (EntityXPOrb entityxp : first.world.getEntitiesWithinAABB(EntityXPOrb.class, first.getBoundingBox().grow(0.5D, 0.0D, 0.5D)))
        {
            combineItems(first, entityxp);
        }
    }

    private static long tone = 0L;
    private static boolean combineItems(EntityXPOrb first, EntityXPOrb other)
    {
        if (
                first == other || first.world.isRemote
                || !first.isAlive() || !other.isAlive()
                || first.delayBeforeCanPickup == 32767 || other.delayBeforeCanPickup == 32767
                || first.xpOrbAge == -32768 || other.xpOrbAge == -32768
                || first.delayBeforeCombine != 0 || other.delayBeforeCombine != 0
        )
        {
            return false;
        }

        int size = getTextureByXP(first.getXpValue());
        first.setXpValue(first.getXpValue() + other.getXpValue());
        first.delayBeforeCanPickup = Math.max(first.delayBeforeCanPickup, other.delayBeforeCanPickup);
        first.xpOrbAge = Math.min(first.xpOrbAge, other.xpOrbAge);
        other.remove();

        EntityXPOrb newOrb;
        if (getTextureByXP(first.getXpValue()) != size)
        {
            newOrb = new EntityXPOrb(first.world, first.getXpValue(), first);

            first.world.spawnEntity(newOrb);
            first.remove();
        }
        else
        {
            first.delayBeforeCombine = 50;
            newOrb = first;
        }
        newOrb.motionX = first.motionX+first.world.rand.nextDouble()*1.0D-0.5D;
        newOrb.motionY = first.motionY+first.world.rand.nextDouble()*0.5D;
        newOrb.motionZ = first.motionZ+first.world.rand.nextDouble()*1.0D-0.5D;

        double pitch = Math.pow(2.0D, (tone%13)/12.0D )/2.0;
        tone+=2;
        if(tone%13 == 0 || tone%13 == 1 || tone%13 == 6) tone --;
        for (EntityPlayer p : newOrb.world.getPlayers(EntityPlayer.class, (p) -> p.getDistanceSq(newOrb) < 256.0D))
        {
            ((EntityPlayerMP)p).connection.sendPacket(new SPacketCustomSound(
                        IRegistry.SOUND_EVENT.getKey(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP),
                        SoundCategory.BLOCKS, newOrb.getPositionVector(), 1.0F, (float)pitch));
        }
        return true;
    }

    // COPY FROM CLIENT CODE
    private static int getTextureByXP(int xpValue)
    {
        if (xpValue >= 2477)
        {
            return 10;
        }
        else if (xpValue >= 1237)
        {
            return 9;
        }
        else if (xpValue >= 617)
        {
            return 8;
        }
        else if (xpValue >= 307)
        {
            return 7;
        }
        else if (xpValue >= 149)
        {
            return 6;
        }
        else if (xpValue >= 73)
        {
            return 5;
        }
        else if (xpValue >= 37)
        {
            return 4;
        }
        else if (xpValue >= 17)
        {
            return 3;
        }
        else if (xpValue >= 7)
        {
            return 2;
        }
        else
        {
            return xpValue >= 3 ? 1 : 0;
        }
    }
}
