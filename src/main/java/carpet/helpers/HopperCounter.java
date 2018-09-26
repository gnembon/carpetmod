package carpet.helpers;

import carpet.utils.Messenger;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HopperCounter
{
    public static final HashMap<String, HashMap<String,Long>> hopper_counter = new HashMap<>();
    public static final HashMap<String, Long> hopper_counter_start_tick = new HashMap<>();
    public static final HashMap<String, Long> hopper_counter_start_millis = new HashMap<>();

    static {
        for (EnumDyeColor color : EnumDyeColor.values())
        {
            String col_str = color.toString();
            hopper_counter.put(col_str, new HashMap<>());
            hopper_counter_start_tick.put(col_str, 0L);
            hopper_counter_start_millis.put(col_str, 0L);
        }
    }

    public static void count_hopper_items(World worldIn, EnumDyeColor color, ItemStack itemstack)
    {

        String item_name = new ItemStack(itemstack.getItem(), 1).getDisplayName().getString();
        int count = itemstack.getCount();
        String color_string = color.toString();
        if (hopper_counter_start_tick.get(color_string) == 0L)
        {
            hopper_counter_start_tick.put(color_string, (long)worldIn.getServer().getTickCounter());
            hopper_counter_start_millis.put(color_string, Util.milliTime());
        }

        long curr_count = hopper_counter.get(color_string).getOrDefault(item_name, 0L);
        hopper_counter.get(color_string).put(item_name, curr_count + count);


    }

    public static void reset_hopper_counter(World worldIn, String color)
    {
        if (color == null)
        {
            for (EnumDyeColor clr : EnumDyeColor.values())
            {

                hopper_counter.put(clr.toString(), new HashMap<String, Long>());
                hopper_counter_start_tick.put(clr.toString(), (long)worldIn.getServer().getTickCounter());
                hopper_counter_start_millis.put(clr.toString(), Util.milliTime());
            }
        }
        else
        {
            hopper_counter.put(color, new HashMap<String, Long>());
            hopper_counter_start_tick.put(color, (long) worldIn.getServer().getTickCounter());
            hopper_counter_start_millis.put(color, Util.milliTime());
        }
    }

    public static List<ITextComponent> query_hopper_all_stats(MinecraftServer server, boolean realtime)
    {
        List<ITextComponent> lst = new ArrayList<>();

        for (EnumDyeColor clr : EnumDyeColor.values())
        {
            List<ITextComponent> temp = query_hopper_stats_for_color(server, clr.toString(),realtime, false);
            if (temp.size() > 1)
            {
                lst.addAll(temp);
                lst.add(Messenger.s(null, ""));
            }
        }
        if (lst.size() == 0)
        {
            lst.add(Messenger.s(null, "No items have been counted yet."));
        }
        return lst;
    }

    public static List<ITextComponent> query_hopper_stats_for_color(MinecraftServer server, String color, boolean realtime, boolean brief)
    {
        List<ITextComponent> lst = new ArrayList<>();
        
        if(hopper_counter.get(color) == null) return lst;
        
        if (hopper_counter.get(color).isEmpty())
        {
            if (brief)
            {
                lst.add(Messenger.m(null, "g "+color+": -, -/h, - min "));
            }
            else
            {
                lst.add(Messenger.s(null, String.format("No items for %s yet", color)));
            }
            return lst;
        }
        long total = 0L;
        for (String item_name : hopper_counter.get(color).keySet() )
        {
            total += hopper_counter.get(color).get(item_name);
        }
        long total_ticks = 0L;
        if (realtime)
        {
            total_ticks = (Util.milliTime()-hopper_counter_start_millis.get(color))/50L+1L;
        }
        else
        {
            total_ticks = (long)server.getTickCounter() - hopper_counter_start_tick.get(color)+1L;
        }
        if (total == 0L)
        {
            if (brief)
            {
                lst.add(Messenger.m(null,
                        String.format("c %s: 0, 0/h, %.1f min ",color,total_ticks*1.0/(20*60))));
            }
            else
            {
                lst.add(Messenger.m(null,
                        String.format("w No items for %s yet (%.2f min.%s)",
                                color, total_ticks*1.0/(20*60), (realtime?" - real time":"")),
                        "nb  [X]", "^g reset", "!/counter "+color+" reset"));
            }
            return lst;
        }

        if (!brief)
        {
            lst.add(Messenger.m(null, String.format("w Items for %s (%.2f min.%s), total: %d, (%.1f/h):",
                            color, total_ticks*1.0/(20*60), (realtime?" - real time":""), total, total*1.0*(20*60*60)/total_ticks),
                    "nb [X]", "^g reset", "!/counter "+color+" reset"
                    ));
            for (String item_name : hopper_counter.get(color).keySet())
            {
                lst.add(Messenger.s(null, String.format(" - %s: %d, %.1f/h",
                        item_name,
                        hopper_counter.get(color).get(item_name),
                        hopper_counter.get(color).get(item_name) * 1.0 * (20 * 60 * 60) / total_ticks)));
            }
        }
        else
        {
            lst.add(Messenger.m(null,
                    String.format("c %s: %d, %d/h, %.1f min ",
                            color, total, total*(20*60*60)/total_ticks, total_ticks*1.0/(20*60))));
        }
        return lst;
    }
}
