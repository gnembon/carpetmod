package carpet.script;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javafx.util.Pair;
import net.minecraft.command.arguments.NBTPathArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.INBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.util.text.TextComponentString;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
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
    public String getSelectedItem()
    {
        if (entity instanceof EntityPlayer)
        {
            ItemStack itemstack = ((EntityPlayer)entity).inventory.getCurrentItem();

            if (!itemstack.isEmpty())
            {
                return IRegistry.field_212630_s.getKey(itemstack.getItem()).toString();
            }
        }
        Iterator<ItemStack> handstuff = entity.getHeldEquipment().iterator();
        if (handstuff.hasNext())
            return IRegistry.field_212630_s.getKey(handstuff.next().getItem()).toString();
        return null;
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
    public Value get(String what)
    {
        if (!(featureAccessors.containsKey(what)))
            throw new Expression.InternalExpressionException("unknown feature of entity: "+what);
        return featureAccessors.get(what).apply(entity);
    }
    private static Map<String, Function<Entity, Value>> featureAccessors = new HashMap<String, Function<Entity, Value>>() {{
        put("health", (e) -> (e instanceof EntityLivingBase)?new NumericValue(((EntityLivingBase) e).getHealth()):Value.NULL);
        put("pos", (e) -> new ListValue(Arrays.asList(new NumericValue(e.posX), new NumericValue(e.posY), new NumericValue(e.posZ))));
        put("x", (e) -> new NumericValue(e.posX));
        put("y", (e) -> new NumericValue(e.posY));
        put("z", (e) -> new NumericValue(e.posZ));
        put("motion", (e) -> new ListValue(Arrays.asList(new NumericValue(e.motionX), new NumericValue(e.motionY), new NumericValue(e.motionZ))));
        put("motionx", (e) -> new NumericValue(e.motionX));
        put("motiony", (e) -> new NumericValue(e.motionY));
        put("motionz", (e) -> new NumericValue(e.motionZ));
        put("name", (e) -> new StringValue(e.getName().getString()));
        put("custom_name", (e) -> new StringValue(e.hasCustomName()?e.getCustomName().getString():""));
        put("type", (e) -> new StringValue(e.getType().func_212546_e().getString()));
        put("is_riding", (e) -> new NumericValue(e.isPassenger()));
        put("is_ridden", (e) -> new NumericValue(e.isBeingRidden()));
        put("passengers", (e) -> new ListValue(e.getPassengers().stream().map(EntityValue::new).collect(Collectors.toList())));
        put("mount", (e) -> (e.getRidingEntity()!=null)?new EntityValue(e.getRidingEntity()):Value.NULL);
        put("tags", (e) -> new ListValue(e.getTags().stream().map(StringValue::new).collect(Collectors.toList())));
        //"has_tag"
        put("rotation_yaw", (e)-> new NumericValue(e.rotationYaw));
        put("rotation_pitch", (e)-> new NumericValue(e.rotationPitch));
        put("is_burning", (e) -> new NumericValue(e.getFire()>0));
        put("fire", (e) -> new NumericValue(e.getFire()));
        put("silent", (e)-> new NumericValue(e.isSilent()));
        put("gravity", (e) -> new NumericValue(!e.hasNoGravity()));
        put("immune_to_fire", (e) -> new NumericValue(e.isImmuneToFire()));
        put("UUID",(e) -> new StringValue(e.getCachedUniqueIdString()));
        put("invulnerable", (e) -> new NumericValue(e.isInvulnerable()));
        put("dimension", (e) -> new StringValue(e.dimension.getSuffix()));
        put("height", (e) -> new NumericValue(e.height));
        put("width", (e) -> new NumericValue(e.width));
        put("eye_height", (e) -> new NumericValue(e.getEyeHeight()));
        put("age", (e) -> new NumericValue(e.ticksExisted));
        put("item", (e) -> (e instanceof EntityItem)?new StringValue(((EntityItem) e).getItem().getDisplayName().getString()):Value.NULL);
        put("count", (e) -> (e instanceof EntityItem)?new NumericValue(((EntityItem) e).getItem().getCount()):Value.NULL);
        // EntityItem -> despawn timer via ssGetAge
        put("is_baby", (e) -> (e instanceof EntityLivingBase)?new NumericValue(((EntityLivingBase) e).isChild()):Value.NULL);
        put("effects", (e) ->
        {
            if (!(e instanceof EntityLivingBase))
            {
                return Value.NULL;
            }
            List<Value> effects = new ArrayList<>();
            for (PotionEffect p: ((EntityLivingBase) e).getActivePotionEffects())
            {
                List<Value> effect = new ArrayList<>(3);
                effect.add(new StringValue(p.getEffectName().replaceFirst("^effect\\.minecraft\\.","")));//getPotion().getDisplayName().getString()));
                effect.add(new NumericValue(p.getAmplifier()));
                effect.add(new NumericValue(p.getDuration()));
                effects.add(ListValue.wrap(effect));
            }
            return ListValue.wrap(effects);
        });
        for (ResourceLocation potionRes : IRegistry.field_212631_t.getKeys())
        {
            Potion potion = IRegistry.field_212631_t.func_212608_b(potionRes);

            put("has_"+potionRes.getPath(), (e) -> {
                if (!(e instanceof EntityLivingBase))
                    return Value.NULL;
                return new NumericValue(((EntityLivingBase) e).isPotionActive(potion));
            });
            put("effect_"+potionRes.getPath(), (e) -> {
                if (!(e instanceof EntityLivingBase))
                    return Value.NULL;

                if ( !((EntityLivingBase) e).isPotionActive(potion ) )
                    return Value.NULL;
                PotionEffect pe = ((EntityLivingBase) e).getActivePotionEffect(potion);
                List<Value> effect = new ArrayList<>(3);
                effect.add(new StringValue(pe.getEffectName().replaceFirst("^effect\\.minecraft\\.","")));//getPotion().getDisplayName().getString()));
                effect.add(new NumericValue(pe.getAmplifier()));
                effect.add(new NumericValue(pe.getDuration()));
                return ListValue.wrap(effect);
            });
        }

        //                                "effect_"+effects
        //                                        "undead"
        //                                                "holds"
        //                                                "holds_offhand"
    }};

    public void set(String what, Value toWhat)
    {
        if (!(featureModifiers.containsKey(what)))
            throw new Expression.InternalExpressionException("unknown action on entity: "+what);
        featureModifiers.get(what).accept(entity, toWhat);
    }

    private static Map<String, BiConsumer<Entity, Value>> featureModifiers = new HashMap<String, BiConsumer<Entity, Value>>() {{
        put("health", (e, v) -> { if (e instanceof EntityLivingBase) ((EntityLivingBase) e).setHealth((float)Expression.getNumericValue(v).getDouble()); });
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
        //"dismount"
        //"mount"
        //"drop_passengers"
        //"mount_passengers"
        //"tag"
        //"clear_tag"
        //"kill"
        //        "rotation_pitch"
        //                "rotation_yaw"
        //                        "look"
        //                        "turn"
        //                                "nod"
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
