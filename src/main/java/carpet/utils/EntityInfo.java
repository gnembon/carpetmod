package carpet.utils;

import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class EntityInfo
{
    private static String makeTime(long ticks)
    {
        long secs = ticks/20;
        if (secs < 60)
        {
            return String.format("%d\"", secs);
        }
        if (secs < 60*60)
        {
            return String.format("%d'%d\"", secs/60, secs%60);
        }

        return String.format("%dh%d'%d\"", secs/60/60, (secs % (60*60))/60,(secs % (60*60))%60 );
    }

    private static String display_item(ItemStack item)
    {
        if (item == null)
        {
            return null;
        }
        if (item.isEmpty()) // func_190926_b()
        {
            return null;
        } // func_190916_E()
        String stackname = item.getCount()>1?String.format("%dx%s",item.getCount(), item.getDisplayName().getString()):item.getDisplayName().getString();
        if (item.isDamaged())
        {
            stackname += String.format(" %d/%d", item.getMaxDamage()-item.getDamage(), item.getMaxDamage());
        }
        if (item.isEnchanted())
        {
            stackname += " ( ";
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(item);
            for (Enchantment e: enchants.keySet())
            {
                int level = enchants.get(e);
                String enstring = e.getName();
                stackname += enstring;
                stackname += " ";
            }
            stackname += ")";
        }
        return stackname;
    }

    public static String entity_short_string(Entity e)
    {
        if (e == null)
        {
            return "None";
        }
        return String.format("%s at [%.1f, %.1f, %.1f]",e.getDisplayName().getString(), e.posX, e.posY, e.posZ);
    }

    private static double get_speed(double internal)
    {
        return 43.1*internal;
    }

    private static double get_horse_speed_percent(double internal)
    {
        double min = 0.45*0.25;
        double max = (0.45+0.9)*0.25;
        return 100*(internal-min)/(max-min);
    }

    private static double get_horse_jump(double x)
    {
         return -0.1817584952 * x*x*x + 3.689713992 * x*x + 2.128599134 * x - 0.343930367;
    }

    private static double get_horse_jump_percent(double internal)
    {
        double min = 0.4;
        double max = 1.0;
        return 100*(internal-min)/(max-min);
    }

    public static List<ITextComponent> entityInfo(Entity e, World source_world)
    {
        List<ITextComponent> lst = new ArrayList<>();
        World world = e.getEntityWorld();
        lst.add(Messenger.c("w "+entity_short_string(e)));
        if (e.isPassenger()) { lst.add(Messenger.c("w  - Rides: ", "wb "+e.getRidingEntity().getDisplayName().getString())); }
        if (e.isBeingRidden())
        {
            List<Entity> passengers = e.getPassengers();
            if (passengers.size() == 1)
            {
                lst.add(Messenger.c("w  - Is being ridden by: ", "wb "+passengers.get(0).getDisplayName().getString()));
            }
            else
            {
                lst.add(Messenger.c("w  - Is being ridden by:"));
                for (Entity ei: passengers)
                {
                    lst.add(Messenger.c("wb    * "+ ei.getDisplayName().getString()));
                }
            }
        }
        lst.add(Messenger.c(String.format("w  - Height: %.2f, Width: %.2f, Eye height: %.2f",e.height, e.width, e.getEyeHeight())));
        lst.add(Messenger.c("w  - Age: ", "wb "+makeTime(e.ticksExisted)));
        if (source_world.dimension.getType() != e.dimension)
        {
            lst.add(Messenger.c("w  - Dimension: ", "wb "+e.dimension.toString()));
        }
        if (e.getFire() > 0) { lst.add(Messenger.c("w  - Fire for ","wb "+e.getFire(),"w  ticks")); }
        if (e.isImmuneToFire() ) { lst.add(Messenger.c("w  - Immune to fire")); }
        if (e.timeUntilPortal > 0) { lst.add(Messenger.c("w  - Portal cooldown for ","wb "+e.timeUntilPortal," ticks")); }
        if (e.isInvulnerable()) { lst.add(Messenger.c("w  - Invulnerable")); }
        if (e.isImmuneToExplosions()) { lst.add(Messenger.c("w  - Immune to explosions")); }

        if (e instanceof EntityItem)
        {
            EntityItem ei = (EntityItem)e;
            ItemStack stack = ei.getItem();// getEntityItem();
            String stackname = stack.getCount()>1?String.format("%dx%s",stack.getCount(), stack.getDisplayName().getString()):stack.getDisplayName().getString();
            lst.add(Messenger.c("w  - Content: ", "wb "+stackname));
            lst.add(Messenger.c("w  - Despawn Timer: ", "wb "+makeTime(ei.getAge())));
        }
        if (e instanceof EntityXPOrb)
        {
            EntityXPOrb exp = (EntityXPOrb)e;
            lst.add(Messenger.c("w  - Despawn Timer: ", "wb "+makeTime(exp.xpOrbAge)));
            lst.add(Messenger.c("w  - Xp Value: ", "wb "+exp.getXpValue()));
        }
        if (e instanceof EntityItemFrame)
        {
            EntityItemFrame eif = (EntityItemFrame)e;
            lst.add(Messenger.c("w  - Content: ", "wb "+eif.getDisplayedItem().getDisplayName()));
            lst.add(Messenger.c("w  - Rotation: ", "wb "+eif.getRotation()));
        }
        if (e instanceof EntityPainting)
        {
            EntityPainting ep = (EntityPainting)e;
            lst.add(Messenger.c("w  - Art: ", "wb "+IRegistry.field_212620_i.getKey(ep.art).toString()));
        }

        if (e instanceof EntityLivingBase)
        {
            EntityLivingBase elb = (EntityLivingBase)e;
            lst.add(Messenger.c("w  - Despawn timer: ", "wb "+makeTime(elb.getIdleTime())));

            lst.add(Messenger.c(String.format("w  - Health: %.2f/%.2f", elb.getHealth(), elb.getMaxHealth())));
            if (elb.getAttribute(SharedMonsterAttributes.ARMOR).getValue() > 0.0)
            {
                lst.add(Messenger.c(String.format("w  - Armour: %.1f",elb.getAttribute(SharedMonsterAttributes.ARMOR).getValue())));
            }
            if (elb.getAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getValue() > 0.0)
            {
                lst.add(Messenger.c(String.format("w  - Toughness: %.1f",elb.getAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getValue())));
            }
            //lst.add(String.format(" - Base speed: %.1fb/s",get_speed(elb.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue())));



            Collection<PotionEffect> potions = elb.getActivePotionEffects();
            if (!potions.isEmpty())
            {
                lst.add(Messenger.c("w  - Potion effects:"));
                for (PotionEffect pe : potions)
                {
                    lst.add(Messenger.c(String.format("w    * %s%s %s",
                            pe.getEffectName().substring(7),
                            (pe.getAmplifier()>1)?String.format("x%d",pe.getAmplifier()):"",
                            makeTime(pe.getDuration()))));
                }
            }
            ItemStack mainhand = elb.getHeldItemMainhand();
            if (!(mainhand.isEmpty()))
            {
                lst.add(Messenger.c("w  - Main hand: ", "wb "+display_item(mainhand)));
            }
            ItemStack offhand = elb.getHeldItemOffhand();
            if (!(offhand.isEmpty()))
            {
                lst.add(Messenger.c("w  - Off hand: ", "wb "+display_item(offhand)));
            }
            String armour = "";
            for (ItemStack armourpiece: elb.getArmorInventoryList())
            {
                if (!(armourpiece.isEmpty()))
                {
                    armour += String.format("\n   * %s", display_item(armourpiece));
                }
            }
            if (!("".equals(armour)))
            {
                lst.add(Messenger.c("w  - Armour:"+ armour));
            }
            if (e instanceof EntityLiving)
            {
                EntityLiving el = (EntityLiving)elb;
                lst.add(Messenger.c(String.format("w  - Follow range: %.1f",el.getAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getValue())));

                lst.add(Messenger.c(String.format("w  - Movement speed factor: %.2f",el.getMoveHelper().getSpeed())));


                EntityLivingBase target_elb = el.getAttackTarget();
                if (target_elb != null)
                {
                    lst.add(Messenger.c("w  - Attack target: ", "wb "+entity_short_string(target_elb)));
                }
                if (el.canPickUpLoot())
                {
                    lst.add(Messenger.c("w  - Can pick up loot"));
                }
                if (el.isNoDespawnRequired())
                {
                    lst.add((Messenger.c("w  - Won't despawn")));
                }

                if (e instanceof EntityWither)
                {
                    EntityWither ew = (EntityWither)e;
                    Entity etarget = world.getEntityByID(ew.getWatchedTargetId(0));
                    lst.add(Messenger.c("w  - Head 1 target: ", "wb "+entity_short_string(etarget) ));
                    etarget = world.getEntityByID(ew.getWatchedTargetId(1));
                    lst.add(Messenger.c("w  - Head 2 target: ", "wb "+entity_short_string(etarget) ));
                    etarget = world.getEntityByID(ew.getWatchedTargetId(2));
                    lst.add(Messenger.c("w  - Head 3 target: ", "wb "+entity_short_string(etarget) ));
                }


            }
        }
                /*


                if (e instanceof EntityCreature)
                {
                    EntityCreature ec = (EntityCreature) e;
                    if (ec.hasHome())
                    {
                        BlockPos pos = ec.getHomePosition();
                        lst.add(String.format(" - Home position: %d blocks around [%d, %d, %d]", (int)ec.getMaximumHomeDistance(), pos.getX(),pos.getY(),pos.getZ()));
                    }
                    if (e instanceof EntityAgeable)
                    {
                        EntityAgeable eage = (EntityAgeable) e;
                        if (eage.getGrowingAge() < 0)
                        {
                            lst.add(String.format(" - Time till adulthood: %s", makeTime(-eage.getGrowingAge())));
                        }
                        if (eage.getGrowingAge() > 0)
                        {
                            lst.add(String.format(" - Mating cooldown: %s", makeTime(eage.getGrowingAge())));
                        }
                        if (e instanceof EntityVillager)
                        {
                            EntityVillager ev = (EntityVillager) e;

                            InventoryBasic vinv = ev.getVillagerInventory();
                            String inventory_content = "";
                            for (int i = 0; i < vinv.getSizeInventory(); ++i)
                            {
                                ItemStack vstack = vinv.getStackInSlot(i);
                                if (!vstack.isEmpty())
                                {
                                    inventory_content += String.format("\n   * %d: %s", i, display_item(vstack));
                                }
                            }
                            if (!("".equals(inventory_content)))
                            {
                                lst.add(String.format(" - Inventory:%s", inventory_content));
                            }
                            if (ev.getWealth()>0)
                            {
                                lst.add(String.format(" - Wealth: %d emeralds", ev.getWealth()));
                            }
                        }
                        if (e instanceof AbstractHorse)
                        {
                            AbstractHorse ah = (AbstractHorse) e;
                            lst.add(String.format(" - Horse Speed: %.2f b/s (%.1f%%%%)",
                                get_speed(elb.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getValue()),
                                get_horse_speed_percent(elb.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getValue())
                                ));
                            lst.add(String.format(" - Horse Jump: %.2f b/s (%.1f%%%%)",
                                get_horse_jump(ah.getHorseJumpStrength()),
                                get_horse_jump_percent(ah.getHorseJumpStrength())
                                ));
                        }
                    }
                    if (e instanceof EntityMob)
                    {
                        lst.add(String.format(" - Base attack: %.1f",elb.getAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getValue()));
                        if (e instanceof EntityZombieVillager)
                        {
                            EntityZombieVillager ezv = (EntityZombieVillager) e;
                            if (ezv.getConversionTime() > 0)
                            {
                                lst.add(String.format(" - Convert to villager in: %s",makeTime(ezv.getConversionTime())));
                            }
                        }
                    }
                }
                if (e instanceof EntitySlime)
                {
                    lst.add(String.format(" - Base attack: %.1f",((EntitySlime)e).getPublicAttackStrength()));
                }
            }
        }
        */

        return lst;
    }

    static void issue_entity_info(EntityPlayer player)
    {
        try
        {
            player.getServer().getCommandManager().handleCommand (player.getCommandSource(), "info entity @e[r=5,c=5,type=!player]"); // TODO fix command call
        }
        catch (Throwable ignored)
        {
        }
    }
}
