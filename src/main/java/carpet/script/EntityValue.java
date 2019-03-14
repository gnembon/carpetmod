package carpet.script;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javafx.util.Pair;
import net.minecraft.command.arguments.NBTPathArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.INBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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
    public String getFromNBT(String path_string)
    {
        NBTTagCompound nbttagcompound = entity.writeWithoutTypeId(new NBTTagCompound());
        NBTPathArgument.NBTPath path;
        try
        {
            path = NBTPathArgument.nbtPath().parse(new StringReader(path_string));
        }
        catch (CommandSyntaxException e)
        {
            throw new Expression.InternalExpressionException("Incorrect path: "+path_string);
        }
        String res = null;
        try
        {
            INBTBase component = path.func_197143_a(nbttagcompound);
            if (component == null)
            {
                return null;
            }
            res = component.toFormattedComponent().getString();
        }
        catch (CommandSyntaxException ignored) { }
        return res;
    }
    public static Map<String, Pair<Class<? extends Entity>, Predicate<? super Entity>>> entityPredicates =
            new HashMap<String, Pair<Class<? extends Entity>, Predicate<? super Entity>>>()
    {{
        put("all", new Pair<>(Entity.class, EntitySelectors.IS_ALIVE));
        put("items", new Pair<>(EntityItem.class, EntitySelectors.IS_ALIVE));
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
        put("pos", (e, a) -> ListValue.of(new NumericValue(e.posX), new NumericValue(e.posY), new NumericValue(e.posZ)));
        put("x", (e, a) -> new NumericValue(e.posX));
        put("y", (e, a) -> new NumericValue(e.posY));
        put("z", (e, a) -> new NumericValue(e.posZ));
        put("motion", (e, a) -> ListValue.of(new NumericValue(e.motionX), new NumericValue(e.motionY), new NumericValue(e.motionZ)));
        put("motionx", (e, a) -> new NumericValue(e.motionX));
        put("motiony", (e, a) -> new NumericValue(e.motionY));
        put("motionz", (e, a) -> new NumericValue(e.motionZ));
        put("name", (e, a) -> new StringValue(e.getName().getString()));
        put("custom_name", (e, a) -> new StringValue(e.hasCustomName()?e.getCustomName().getString():""));
        put("type", (e, a) -> new StringValue(e.getType().func_212546_e().getString()));
        put("is_riding", (e, a) -> new NumericValue(e.isPassenger()));
        put("is_ridden", (e, a) -> new NumericValue(e.isBeingRidden()));
        put("passengers", (e, a) -> ListValue.wrap(e.getPassengers().stream().map(EntityValue::new).collect(Collectors.toList())));
        put("mount", (e, a) -> (e.getRidingEntity()!=null)?new EntityValue(e.getRidingEntity()):Value.NULL);
        put("tags", (e, a) -> ListValue.wrap(e.getTags().stream().map(StringValue::new).collect(Collectors.toList())));
        put("tag", (e, a) -> new NumericValue(e.getTags().contains(a.getString())));
        put("rotation_yaw", (e, a)-> new NumericValue(e.rotationYaw));
        put("rotation_pitch", (e, a)-> new NumericValue(e.rotationPitch));
        put("is_burning", (e, a) -> new NumericValue(e.getFire()>0));
        put("fire", (e, a) -> new NumericValue(e.getFire()));
        put("silent", (e, a)-> new NumericValue(e.isSilent()));
        put("gravity", (e, a) -> new NumericValue(!e.hasNoGravity()));
        put("immune_to_fire", (e, a) -> new NumericValue(e.isImmuneToFire()));
        put("UUID",(e, a) -> new StringValue(e.getCachedUniqueIdString()));
        put("invulnerable", (e, a) -> new NumericValue(e.isInvulnerable()));
        put("dimension", (e, a) -> new StringValue(e.dimension.getSuffix()));
        put("height", (e, a) -> new NumericValue(e.height));
        put("width", (e, a) -> new NumericValue(e.width));
        put("eye_height", (e, a) -> new NumericValue(e.getEyeHeight()));
        put("age", (e, a) -> new NumericValue(e.ticksExisted));
        put("item", (e, a) -> (e instanceof EntityItem)?new StringValue(((EntityItem) e).getItem().getDisplayName().getString()):Value.NULL);
        put("count", (e, a) -> (e instanceof EntityItem)?new NumericValue(((EntityItem) e).getItem().getCount()):Value.NULL);
        // EntityItem -> despawn timer via ssGetAge
        put("is_baby", (e, a) -> (e instanceof EntityLivingBase)?new NumericValue(((EntityLivingBase) e).isChild()):Value.NULL);
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
    }};

    public void set(String what, Value toWhat)
    {
        if (!(featureModifiers.containsKey(what)))
            throw new Expression.InternalExpressionException("unknown action on entity: "+what);
        featureModifiers.get(what).accept(entity, toWhat);
    }

    private static Map<String, BiConsumer<Entity, Value>> featureModifiers = new HashMap<String, BiConsumer<Entity, Value>>() {{
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
        });
        put("x", (e, v) -> e.posX = Expression.getNumericValue(v).getDouble());
        put("y", (e, v) -> e.posY = Expression.getNumericValue(v).getDouble());
        put("z", (e, v) -> e.posZ = Expression.getNumericValue(v).getDouble());
        put("pitch", (e, v) -> e.rotationPitch = (float)Expression.getNumericValue(v).getDouble());
        put("yaw", (e, v) -> e.rotationYaw = (float)Expression.getNumericValue(v).getDouble());
        //                        "look"
        //                        "turn"
        //                                "nod"

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
        });
        put("motionx", (e, v) -> e.motionX = Expression.getNumericValue(v).getDouble());
        put("motiony", (e, v) -> e.motionY = Expression.getNumericValue(v).getDouble());
        put("motionz", (e, v) -> e.motionZ = Expression.getNumericValue(v).getDouble());

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

        //                                        "fire"
        //                                                "extinguish"
        //                                                        "silent"
        //                                                                "gravity"
        //                                                                        "invulnerable"
        //                                                                                "dimension"
        //                                                                                        "item"
        //                                                                                                "count",
        //"age",
        //"effect_"name
        //"hold"
        //        "hold_offhand"
        //                "jump"
    }};
}
