package carpet.script;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.EntitySelector;
import net.minecraft.command.arguments.EntitySelectorParser;
import net.minecraft.command.arguments.NBTPathArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityTeleport;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.util.text.TextComponentString;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EntityValue extends Value
{
    private Entity entity;

    public EntityValue(Entity e)
    {
        entity = e;
    }

    private static Map<String, EntitySelector> selectorCache = new HashMap<>();
    public static Collection<? extends Entity > getEntitiesFromSelector(CommandSource source, String selector)
    {
        try
        {
            EntitySelector entitySelector = selectorCache.get(selector);
            if (entitySelector != null)
            {
                return entitySelector.select(source);
            }
            entitySelector = new EntitySelectorParser(new StringReader(selector), true).parse();
            selectorCache.put(selector, entitySelector);
            return entitySelector.select(source);
        }
        catch (CommandSyntaxException e)
        {
            throw new Expression.InternalExpressionException("Cannot select entities from "+selector);
        }
    }

    public Entity getEntity()
    {
        return entity;
    }

    @Override
    public String getString()
    {
        return entity.getDisplayName().getString();
    }

    @Override
    public boolean getBoolean()
    {
        return true;
    }

    @Override
    public boolean equals(Value v)
    {
        if (v instanceof EntityValue)
        {
            return entity.getEntityId()==((EntityValue) v).entity.getEntityId();
        }
        return super.equals(v);
    }

    @Override
    public Value in(Value v)
    {
        String what = v.getString();
        return this.get(what, null);
    }

    public static Pair<Class<? extends Entity>, Predicate<? super Entity>> getPredicate(String who)
    {
        Pair<Class<? extends Entity>, Predicate<? super Entity>> res = entityPredicates.get(who);
        if (res != null) return res;
        return res; //TODO add more here like search by tags, or type
        //if (who.startsWith('tag:'))
    }
    private static Map<String, Pair<Class<? extends Entity>, Predicate<? super Entity>>> entityPredicates =
            new HashMap<String, Pair<Class<? extends Entity>, Predicate<? super Entity>>>()
    {{
        put("*", Pair.of(Entity.class, EntitySelectors.IS_ALIVE));
        put("living", Pair.of(EntityLivingBase.class, EntitySelectors.IS_ALIVE));
        put("items", Pair.of(EntityItem.class, EntitySelectors.IS_ALIVE));
        put("players", Pair.of(EntityPlayer.class, EntitySelectors.IS_ALIVE));
        put("!players", Pair.of(Entity.class, (e) -> !(e instanceof EntityPlayer) ));
    }};
    public Value get(String what, Value arg)
    {
        if (!(featureAccessors.containsKey(what)))
            throw new Expression.InternalExpressionException("unknown feature of entity: "+what);
        return featureAccessors.get(what).apply(entity, arg);
    }
    private static Map<String, EntityEquipmentSlot> inventorySlots = new HashMap<String, EntityEquipmentSlot>(){{
        put("main", EntityEquipmentSlot.MAINHAND);
        put("offhand", EntityEquipmentSlot.OFFHAND);
        put("head", EntityEquipmentSlot.HEAD);
        put("chest", EntityEquipmentSlot.CHEST);
        put("legs", EntityEquipmentSlot.LEGS);
        put("feet", EntityEquipmentSlot.FEET);
    }};
    private static Map<String, BiFunction<Entity, Value, Value>> featureAccessors = new HashMap<String, BiFunction<Entity, Value, Value>>() {{
        put("removed", (entity, arg) -> new NumericValue(entity.removed));
        put("uuid",(e, a) -> new StringValue(e.getCachedUniqueIdString()));
        put("id",(e, a) -> new NumericValue(e.getEntityId()));
        put("pos", (e, a) -> ListValue.of(new NumericValue(e.posX), new NumericValue(e.posY), new NumericValue(e.posZ)));
        put("x", (e, a) -> new NumericValue(e.posX));
        put("y", (e, a) -> new NumericValue(e.posY));
        put("z", (e, a) -> new NumericValue(e.posZ));
        put("motion", (e, a) -> ListValue.of(new NumericValue(e.motionX), new NumericValue(e.motionY), new NumericValue(e.motionZ)));
        put("motion_x", (e, a) -> new NumericValue(e.motionX));
        put("motion_y", (e, a) -> new NumericValue(e.motionY));
        put("motion_z", (e, a) -> new NumericValue(e.motionZ));
        put("name", (e, a) -> new StringValue(e.getDisplayName().getString()));
        put("custom_name", (e, a) -> e.hasCustomName()?new StringValue(e.getCustomName().getString()):Value.NULL);
        put("type", (e, a) -> new StringValue(e.getType().getTranslationKey().replaceFirst("entity\\.minecraft\\.","")));
        put("is_riding", (e, a) -> new NumericValue(e.isPassenger()));
        put("is_ridden", (e, a) -> new NumericValue(e.isBeingRidden()));
        put("passengers", (e, a) -> ListValue.wrap(e.getPassengers().stream().map(EntityValue::new).collect(Collectors.toList())));
        put("mount", (e, a) -> (e.getRidingEntity()!=null)?new EntityValue(e.getRidingEntity()):Value.NULL);
        put("tags", (e, a) -> ListValue.wrap(e.getTags().stream().map(StringValue::new).collect(Collectors.toList())));
        put("has_tag", (e, a) -> new NumericValue(e.getTags().contains(a.getString())));
        put("yaw", (e, a)-> new NumericValue(e.rotationYaw));
        put("pitch", (e, a)-> new NumericValue(e.rotationPitch));
        put("is_burning", (e, a) -> new NumericValue(e.getFire()>0));
        put("fire", (e, a) -> new NumericValue(e.getFire()));
        put("silent", (e, a)-> new NumericValue(e.isSilent()));
        put("gravity", (e, a) -> new NumericValue(!e.hasNoGravity()));
        put("immune_to_fire", (e, a) -> new NumericValue(e.isImmuneToFire()));

        put("invulnerable", (e, a) -> new NumericValue(e.isInvulnerable()));
        put("dimension", (e, a) -> new StringValue(e.dimension.toString().replaceFirst("minecraft:","")));
        put("height", (e, a) -> new NumericValue(e.height));
        put("width", (e, a) -> new NumericValue(e.width));
        put("eye_height", (e, a) -> new NumericValue(e.getEyeHeight()));
        put("age", (e, a) -> new NumericValue(e.ticksExisted));
        put("item", (e, a) -> (e instanceof EntityItem)?new StringValue(((EntityItem) e).getItem().getDisplayName().getString()):Value.NULL);
        put("count", (e, a) -> (e instanceof EntityItem)?new NumericValue(((EntityItem) e).getItem().getCount()):Value.NULL);
        // EntityItem -> despawn timer via ssGetAge
        put("is_baby", (e, a) -> (e instanceof EntityLivingBase)?new NumericValue(((EntityLivingBase) e).isChild()):Value.NULL);
        put("target", (e, a) -> {
            if (e instanceof EntityLiving)
            {
                EntityLivingBase target = ((EntityLiving) e).getAttackTarget();
                if (target != null)
                {
                    return new EntityValue(target);
                }
            }
            return Value.NULL;
        });
        put("home", (e, a) -> {
            if (e instanceof EntityCreature)
            {
                return ((EntityCreature) e).hasHome()?new BlockValue(null, e.getEntityWorld(), ((EntityCreature) e).getHomePosition()):Value.FALSE;
            }
            return Value.NULL;
        });
        put("sneaking", (e, a) -> {
            if (e instanceof EntityPlayer)
            {
                return e.isSneaking()?Value.TRUE:Value.FALSE;
            }
            return Value.NULL;
        });
        put("sprinting", (e, a) -> {
            if (e instanceof EntityPlayer)
            {
                return e.isSprinting()?Value.TRUE:Value.FALSE;
            }
            return Value.NULL;
        });
        put("swimming", (e, a) -> {
            if (e instanceof EntityPlayer)
            {
                return e.isSwimming()?Value.TRUE:Value.FALSE;
            }
            return Value.NULL;
        });
        put("gamemode", (e, a) -> {
            if (e instanceof  EntityPlayerMP)
            {
                return new StringValue(((EntityPlayerMP) e).interactionManager.getGameType().getName());
            }
            return Value.NULL;
        });
        put("gamemode_id", (e, a) -> {
            if (e instanceof  EntityPlayerMP)
            {
                return new NumericValue(((EntityPlayerMP) e).interactionManager.getGameType().getID());
            }
            return Value.NULL;
        });
        //spectating_entity
        // isGlowing
        put("effect", (e, a) ->
        {
            if (!(e instanceof EntityLivingBase))
            {
                return Value.NULL;
            }
            if (a == null)
            {
                List<Value> effects = new ArrayList<>();
                for (PotionEffect p : ((EntityLivingBase) e).getActivePotionEffects())
                {
                    effects.add(ListValue.of(
                        new StringValue(p.getEffectName().replaceFirst("^effect\\.minecraft\\.", "")),
                        new NumericValue(p.getAmplifier()),
                        new NumericValue(p.getDuration())
                    ));
                }
                return ListValue.wrap(effects);
            }
            String effectName = a.getString();
            Potion potion = IRegistry.field_212631_t.func_212608_b(new ResourceLocation(effectName));
            if (potion == null)
                throw new Expression.InternalExpressionException("No such an effect: "+effectName);
            if (!((EntityLivingBase) e).isPotionActive(potion))
                return Value.NULL;
            PotionEffect pe = ((EntityLivingBase) e).getActivePotionEffect(potion);
            return ListValue.of( new NumericValue(pe.getAmplifier()), new NumericValue(pe.getDuration()) );
        });
        put("health", (e, a) ->
        {
            if (e instanceof EntityLivingBase)
            {
                return new NumericValue(((EntityLivingBase) e).getHealth());
            }
            //if (e instanceof EntityItem)
            //{
            //    e.h consider making item health public
            //}
            return Value.NULL;
        });
        put("holds", (e, a) -> {
            EntityEquipmentSlot where = EntityEquipmentSlot.MAINHAND;
            if (a != null)
                where = inventorySlots.get(a.getString());
            if (where == null)
                throw new Expression.InternalExpressionException("Unknown inventory slot: "+a.getString());
            if (e instanceof EntityLivingBase)
            {
                ItemStack itemstack = ((EntityLivingBase)e).getItemStackFromSlot(where);
                if (!itemstack.isEmpty())
                {
                    return ListValue.of(
                            new StringValue(IRegistry.field_212630_s.getKey(itemstack.getItem()).getPath()),
                            new NumericValue(itemstack.getCount()),
                            new StringValue(itemstack.write(new NBTTagCompound()).getString())
                    );
                }
            }
            return Value.NULL;

        });

        put("nbt",(e, a) -> {
            NBTTagCompound nbttagcompound = e.writeWithoutTypeId(new NBTTagCompound());
            if (a==null)
                return new StringValue(nbttagcompound.getString());
            NBTPathArgument.NBTPath path;
            try
            {
                path = NBTPathArgument.nbtPath().parse(new StringReader(a.getString()));
            }
            catch (CommandSyntaxException exc)
            {
                throw new Expression.InternalExpressionException("Incorrect path: "+a.getString());
            }
            String res = null;
            try
            {
                res = path.func_197143_a(nbttagcompound).toFormattedComponent().getString();
            }
            catch (CommandSyntaxException ignored) { }
            return new StringValue(res);
        });
    }};
    private static <Req extends Entity> Req assertEntityArgType(Class<Req> klass, Value arg)
    {
        if (!(arg instanceof EntityValue))
        {
            return null;
        }
        Entity e = ((EntityValue) arg).getEntity();
        if (!(klass.isAssignableFrom(e.getClass())))
        {
            return null;
        }
        return (Req)e;
    }

    public void set(String what, Value toWhat)
    {
        if (!(featureModifiers.containsKey(what)))
            throw new Expression.InternalExpressionException("unknown action on entity: " + what);
        featureModifiers.get(what).accept(entity, toWhat);
    }

    private static Map<String, BiConsumer<Entity, Value>> featureModifiers = new HashMap<String, BiConsumer<Entity, Value>>() {{
        put("remove", (entity, value) -> entity.remove());
        put("health", (e, v) -> { if (e instanceof EntityLivingBase) ((EntityLivingBase) e).setHealth((float)Expression.getNumericValue(v).getDouble()); });
        put("kill", (e, v) -> e.onKillCommand());
        put("pos", (e, v) ->
        {
            if (!(v instanceof ListValue))
            {
                throw new Expression.InternalExpressionException("expected a list of 3 parameters as second argument");
            }
            List<Value> coords = ((ListValue) v).getItems();
            e.posX = Expression.getNumericValue(coords.get(0)).getDouble();
            e.posY = Expression.getNumericValue(coords.get(1)).getDouble();
            e.posZ = Expression.getNumericValue(coords.get(2)).getDouble();
            e.setPosition(e.posX, e.posY, e.posZ);
            if (e instanceof EntityPlayerMP)
                ((EntityPlayerMP)e).connection.sendPacket(new SPacketEntityTeleport(e));
        });
        put("x", (e, v) ->
        {
            e.posX = Expression.getNumericValue(v).getDouble();
            e.setPosition(e.posX, e.posY, e.posZ);
            if (e instanceof EntityPlayerMP)
                ((EntityPlayerMP)e).connection.sendPacket(new SPacketEntityTeleport(e));
        });
        put("y", (e, v) ->
        {
            e.posY = Expression.getNumericValue(v).getDouble();
            e.setPosition(e.posX, e.posY, e.posZ);
            if (e instanceof EntityPlayerMP)
                ((EntityPlayerMP)e).connection.sendPacket(new SPacketEntityTeleport(e));
        });
        put("z", (e, v) ->
        {
            e.posZ = Expression.getNumericValue(v).getDouble();
            e.setPosition(e.posX, e.posY, e.posZ);
            if (e instanceof EntityPlayerMP)
                ((EntityPlayerMP)e).connection.sendPacket(new SPacketEntityTeleport(e));
        });
        put("pitch", (e, v) ->
        {
            e.rotationPitch = (float) Expression.getNumericValue(v).getDouble();
            e.prevRotationPitch = e.rotationPitch;
            if (e instanceof EntityPlayerMP)
                ((EntityPlayerMP)e).connection.sendPacket(new SPacketEntityTeleport(e));
        });
        put("yaw", (e, v) ->
        {
            e.rotationYaw = (float) Expression.getNumericValue(v).getDouble();
            e.prevRotationYaw = e.rotationYaw;
            if (e instanceof EntityPlayerMP)
                ((EntityPlayerMP)e).connection.sendPacket(new SPacketEntityTeleport(e));
        });
        //"look"
        //"turn"
        //"nod"

        put("move", (e, v) ->
        {
            if (!(v instanceof ListValue))
            {
                throw new Expression.InternalExpressionException("expected a list of 3 parameters as second argument");
            }
            List<Value> coords = ((ListValue) v).getItems();
            e.posX += Expression.getNumericValue(coords.get(0)).getDouble();
            e.posY += Expression.getNumericValue(coords.get(1)).getDouble();
            e.posZ += Expression.getNumericValue(coords.get(2)).getDouble();
            e.setPosition(e.posX, e.posY, e.posZ);
            if (e instanceof EntityPlayerMP)
                ((EntityPlayerMP)e).connection.sendPacket(new SPacketEntityTeleport(e));
        });

        put("motion", (e, v) ->
        {
            if (!(v instanceof ListValue))
            {
                throw new Expression.InternalExpressionException("expected a list of 3 parameters as second argument");
            }
            List<Value> coords = ((ListValue) v).getItems();
            e.motionX = Expression.getNumericValue(coords.get(0)).getDouble();
            e.motionY = Expression.getNumericValue(coords.get(1)).getDouble();
            e.motionZ = Expression.getNumericValue(coords.get(2)).getDouble();
            if (e instanceof EntityPlayerMP)
                ((EntityPlayerMP)e).connection.sendPacket(new SPacketEntityVelocity(e));
        });
        put("motion_x", (e, v) ->
        {
            e.motionX = Expression.getNumericValue(v).getDouble();
            if (e instanceof EntityPlayerMP)
                ((EntityPlayerMP)e).connection.sendPacket(new SPacketEntityVelocity(e));
        });
        put("motion_y", (e, v) ->
        {
            e.motionY = Expression.getNumericValue(v).getDouble();
            if (e instanceof EntityPlayerMP)
                ((EntityPlayerMP)e).connection.sendPacket(new SPacketEntityVelocity(e));
        });
        put("motion_z", (e, v) ->
        {
            e.motionZ = Expression.getNumericValue(v).getDouble();
            if (e instanceof EntityPlayerMP)
                ((EntityPlayerMP)e).connection.sendPacket(new SPacketEntityVelocity(e));
        });

        put("accelerate", (e, v) ->
        {
            if (!(v instanceof ListValue))
            {
                throw new Expression.InternalExpressionException("expected a list of 3 parameters as second argument");
            }
            List<Value> coords = ((ListValue) v).getItems();
            e.addVelocity(
                    Expression.getNumericValue(coords.get(0)).getDouble(),
                    Expression.getNumericValue(coords.get(1)).getDouble(),
                    Expression.getNumericValue(coords.get(2)).getDouble()
            );
            if (e instanceof EntityPlayerMP)
                ((EntityPlayerMP)e).connection.sendPacket(new SPacketEntityVelocity(e));

        });
        put("custom_name", (e, v) -> {
            String name = v.getString();
            if (name.isEmpty())
                e.setCustomName(null);
            e.setCustomName(new TextComponentString(v.getString()));
        });
        put("dismount", (e, v) -> e.stopRiding() );
        put("mount", (e, v) -> {
            if (v instanceof EntityValue)
            {
                e.startRiding(((EntityValue) v).getEntity(),true);
            }
        });
        put("drop_passengers", (e, v) -> e.removePassengers());
        put("mount_passengers", (e, v) -> {
            if (v==null)
                throw new Expression.InternalExpressionException("mount_passengers needs entities to ride");
            if (v instanceof EntityValue)
                ((EntityValue) v).getEntity().startRiding(e);
            else if (v instanceof ListValue)
                for (Value element : ((ListValue) v).getItems())
                    if (element instanceof EntityValue)
                        ((EntityValue) element).getEntity().startRiding(e);
        });
        put("tag", (e, v) -> {
            if (v==null)
                throw new Expression.InternalExpressionException("tag requires parameters");
            if (v instanceof ListValue)
                for (Value element : ((ListValue) v).getItems()) e.addTag(element.getString());
            else
                e.addTag(v.getString());
        });
        put("clear_tag", (e, v) -> {
            if (v==null)
                throw new Expression.InternalExpressionException("clear_tag requires parameters");
            if (v instanceof ListValue)
                for (Value element : ((ListValue) v).getItems()) e.removeTag(element.getString());
            else
                e.removeTag(v.getString());
        });
        //put("target", (e, v) -> {
        //    // attacks indefinitely - might need to do it through tasks
        //    if (e instanceof EntityLiving)
        //    {
        //        EntityLivingBase elb = assertEntityArgType(EntityLivingBase.class, v);
        //        ((EntityLiving) e).setAttackTarget(elb);
        //    }
        //});
        put("talk", (e, v) -> {
            // attacks indefinitely
            if (e instanceof EntityLiving)
            {
                ((EntityLiving) e).playAmbientSound();
            }
        });
        put("home", (e, v) -> {
            if (!(e instanceof EntityCreature))
                return;
            EntityCreature ec = (EntityCreature)e;
            if (v == null)
                throw new Expression.InternalExpressionException("home requires at least one position argument, and optional distance, or null to cancel");
            if (v instanceof NullValue)
            {
                ec.detachHome();
                ec.getAI(false).removeTask(ec.temporaryTasks.get("home"));
                ec.temporaryTasks.remove("home");
                return;
            }

            BlockPos pos;
            int distance = 16;

            if (v instanceof BlockValue)
            {
                pos = ((BlockValue) v).getPos();
                if (pos == null) throw new Expression.InternalExpressionException("block is not positioned in the world");
            }
            else if (v instanceof ListValue)
            {
                List<Value> lv = ((ListValue) v).getItems();
                if (lv.get(0) instanceof BlockValue)
                {
                    pos = ((BlockValue) lv.get(0)).getPos();
                    if (lv.size()>1)
                    {
                        distance = (int)Expression.getNumericValue(lv.get(1)).getLong();
                    }
                }
                else if (lv.size()>=3)
                {
                    pos = new BlockPos(Expression.getNumericValue(lv.get(0)).getLong(),
                            Expression.getNumericValue(lv.get(1)).getLong(),
                            Expression.getNumericValue(lv.get(2)).getLong());
                    if (lv.size()>3)
                    {
                        distance = (int)Expression.getNumericValue(lv.get(4)).getLong();
                    }
                }
                else throw new Expression.InternalExpressionException("home requires at least one position argument, and optional distance");

            }
            else throw new Expression.InternalExpressionException("home requires at least one position argument, and optional distance");

            ec.setHomePosAndDistance(pos, distance);
            if (!ec.temporaryTasks.containsKey("home"))
            {
                EntityAIBase task = new EntityAIMoveTowardsRestriction(ec, 1.0D);
                ec.temporaryTasks.put("home", task);
                ec.getAI(false).addTask(10, task);
            }
        });

        // gamemode
        // spectate
        // "fire"
        // "extinguish"
        // "silent"
        // "gravity"
        // "invulnerable"
        // "dimension"
        // "item"
        // "count",
        // "age",
        // "effect_"name
        // "hold"
        // "hold_offhand"
        // "jump"
        // "nbt" <-big one, for now use run('data merge entity ...
    }};
}
