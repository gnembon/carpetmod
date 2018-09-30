package carpet.helpers;

import carpet.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIFindEntityNearestPlayer;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class GhastHelper
{
    /*target selector to make sure no player with names is chosen
     */
    public static class GhastEntityAIFindEntityNearestPlayer extends EntityAIFindEntityNearestPlayer
    {
        public GhastEntityAIFindEntityNearestPlayer(EntityLiving entityLivingIn)
        {
            super(entityLivingIn);
        }

        @Override
        public boolean shouldExecute()
        {
            if (CarpetSettings.getBool("rideableGhasts") && this.entityLiving.hasCustomName())
            {
                return false;
            }
            return super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting()
        {
            if (CarpetSettings.getBool("rideableGhasts") && this.entityLiving.hasCustomName())
            {
                return false;
            }
            return super.shouldContinueExecuting();
        }

    }
    public static boolean is_yo_bro(EntityGhast ghast, EntityPlayer player)
    {
        return (ghast.hasCustomName() && player.getGameProfile().getName().equals(ghast.getCustomName().getString()));
    }
    public static boolean holds_yo_tear(EntityPlayer player)
    {
        return (
                (!player.getHeldItemMainhand().isEmpty() &&
                        player.getHeldItemMainhand().getItem() == Items.GHAST_TEAR)
                        ||
                (!player.getHeldItemOffhand().isEmpty() &&
                                player.getHeldItemOffhand().getItem() == Items.GHAST_TEAR)
        );
    }
    /*sets off fireball on demand
     */
    public static void set_off_fball(EntityGhast ghast, World world, EntityPlayer player)
    {
        world.playEvent((EntityPlayer)null, 1015, new BlockPos(ghast), 0);
        Vec3d vec3d = player.getLook(1.0F);
        world.playEvent((EntityPlayer)null, 1016, new BlockPos(ghast), 0);
        EntityLargeFireball entitylargefireball = new EntityLargeFireball(world, player, 30.0*vec3d.x, 30.0*vec3d.y, 30.0*vec3d.z);
        entitylargefireball.explosionPower = ghast.getFireballStrength();
        entitylargefireball.posX = ghast.posX + vec3d.x * 4.0D;
        entitylargefireball.posY = ghast.posY + (double)(ghast.height / 2.0F) +vec3d.y * 4.0D+ 0.5D;
        entitylargefireball.posZ = ghast.posZ + vec3d.z * 4.0D;
        world.spawnEntity(entitylargefireball);
    }

    /*rided ghast follows rider's tear clues
    */
    public static class AIFollowClues extends EntityAIBase
    {
        private final EntityGhast parentEntity;
        private EntityPlayer rider = null;
        public AIFollowClues(EntityGhast ghast)
        {
            this.parentEntity = ghast;
            this.setMutexBits(1);
        }
        public boolean shouldExecute()
        {
            if (!CarpetSettings.getBool("rideableGhasts"))
            {
                return false;
            }
            if (this.parentEntity.isBeingRidden())
            {
                Entity p = this.parentEntity.getControllingPassenger();
                if (p instanceof EntityPlayer)
                {
                    if (holds_yo_tear((EntityPlayer)p))
                    {
                        return true;
                    }
                }
            }
            return false;
            //return (this.parentEntity.isBeingRidden() && this.parentEntity.getPassengers().get(0) instanceof EntityPlayer);
        }
        public void startExecuting()
        {
            rider = (EntityPlayer)this.parentEntity.getControllingPassenger();
        }
        public void resetTask()
        {
            this.rider = null;
        }
        //private Vec3d look_left(Vec3d v)
        //{
            //return new Vec3d(v.zCoord+v.yCoord, -v.zCoord+v.xCoord, -v.yCoord-v.xCoord);
            //return new Vec3d(v.zCoord+v.yCoord, 0.0, -v.yCoord-v.xCoord);
        //}
        public void updateTask()
        {
            float strafe = rider.moveStrafing;
            float forward = rider.moveForward;
            if (forward <= 0.0F)
            {
                forward *= 0.5F;
            }
            Vec3d vec3d = Vec3d.ZERO;
            if (forward != 0.0f)
            {
                vec3d = rider.getLook(1.0F);
                if (forward < 0.0f)
                {
                    vec3d = vec3d.subtractReverse(Vec3d.ZERO);
                }
            }
            if (strafe != 0.0f)
            {
                //Vec3d strafe_vec = rider.getLook(1.0F).rotateYaw((float)Math.PI / 2F).rotatePitch(-rider.rotationPitch).scale(strafe);
                //Vec3d strafe_vec = this.look_left(rider.getLook(1.0F)).scale(strafe);

                //strafe_vec = new Vec3d(strafe_vec.xCoord, 0.0f, strafe_vec.zCoord);
                //vec3d = vec3d.add(strafe_vec);
                float c = MathHelper.cos(rider.rotationYaw* 0.017453292F);
                float s = MathHelper.sin(rider.rotationYaw* 0.017453292F);
                vec3d = new Vec3d(vec3d.x+c*strafe,vec3d.y,vec3d.z+s*strafe);
            }
            if (rider.isJumping)
            {
                vec3d = new Vec3d(vec3d.x,vec3d.y+1.0D,vec3d.z);
            }
            if (!(vec3d.equals(Vec3d.ZERO)))
            {
                this.parentEntity.getMoveHelper().setMoveTo(this.parentEntity.posX+vec3d.x, this.parentEntity.posY+vec3d.y,this.parentEntity.posZ+vec3d.z, 1.0D );
            }
            else
            {
                this.parentEntity.getMoveHelper().action = EntityMoveHelper.Action.WAIT;
            }
        }
    }

    /* homing abilities to find the player
    */
    public static class AIFindOwner extends EntityAIBase
    {
        private final EntityGhast parentEntity;
        private EntityPlayer owner = null;
        public AIFindOwner(EntityGhast ghast)
        {
            this.parentEntity = ghast;
            this.setMutexBits(1);
        }

        private EntityPlayer findOwner()
        {
            if (!this.parentEntity.isBeingRidden() && this.parentEntity.hasCustomName())
            {
                EntityPlayer player = ((WorldServer)this.parentEntity.getEntityWorld()).getServer().getPlayerList().getPlayerByUsername(this.parentEntity.getCustomName().getString());
                if (player != null && player.dimension == this.parentEntity.dimension && this.parentEntity.getDistanceSq(player) < 300.0D*300.0D)
                {
                    if (!(player.isPassenger() && player.getRidingEntity() instanceof EntityGhast))
                    {
                        if (this.parentEntity.getDistanceSq(player) > 10.0D*10.0D && holds_yo_tear(player))
                        {
                            return player;
                        }
                    }
                }
            }
            return null;
        }

        public boolean shouldExecute()
        {
            if (!CarpetSettings.getBool("rideableGhasts"))
            {
                return false;
            }
            if (owner != null)
            {
                owner = null;
                return false;
            }
            if (this.parentEntity.getRNG().nextInt(5) != 0)
            {
                return false;
            }

            owner = findOwner();
            if (owner == null)
            {
                return false;
            }
            return true;
        }
        public void startExecuting()
        {
            continueExecuting();
        }
        public void resetTask()
        {
            this.owner = null;
        }
        public boolean continueExecuting()
        {
            if (owner != null && owner.dimension == this.parentEntity.dimension)
                {
                    if (this.parentEntity.getDistanceSq(owner) > 50D && holds_yo_tear(owner))
                    {
                        Vec3d target = new Vec3d(this.owner.posX-this.parentEntity.posX, this.owner.posY-this.parentEntity.posY,this.owner.posZ-this.parentEntity.posZ).normalize();
                        this.parentEntity.getMoveHelper().setMoveTo((double)this.parentEntity.posX+target.x, (double)this.parentEntity.posY+target.y, (double)this.parentEntity.posZ+target.z, 1.0D);
                        return true;
                    }
                }
            return false;
        }
    }
}
