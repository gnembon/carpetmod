package carpet.utils;

import carpet.helpers.HopperCounter;
import carpet.helpers.TickSpeed;
import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.PacketCounter;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketPlayerListHeaderFooter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HUDController
{
    public static Map<EntityPlayer, List<ITextComponent>> player_huds = new HashMap<>();

    public static void addMessage(EntityPlayer player, ITextComponent hudMessage)
    {
        if (!player_huds.containsKey(player))
        {
            player_huds.put(player, new ArrayList<>());
        }
        else
        {
            player_huds.get(player).add(new TextComponentString("\n"));
        }
        player_huds.get(player).add(hudMessage);
    }
    public static void clear_player(EntityPlayer player)
    {
        SPacketPlayerListHeaderFooter packet = new SPacketPlayerListHeaderFooter();
        packet.header = new TextComponentString("");
        packet.footer = new TextComponentString("");
        ((EntityPlayerMP)player).connection.sendPacket(packet);
    }


    public static void update_hud(MinecraftServer server)
    {
        if(server.getTickCounter() % 20 != 0)
            return;

        player_huds.clear();

        if (LoggerRegistry.__tps)
            LoggerRegistry.getLogger("tps").log(()-> send_tps_display(server));

        if (LoggerRegistry.__mobcaps)
            LoggerRegistry.getLogger("mobcaps").log((option, player) -> {
                int dim = player.dimension.getId();
                switch (option)
                {
                    case "overworld":
                        dim = 0;
                        break;
                    case "nether":
                        dim = -1;
                        break;
                    case "end":
                        dim = 1;
                        break;
                }
                return send_mobcap_display(dim);
            });

        if(LoggerRegistry.__counter)
            LoggerRegistry.getLogger("counter").log((option)->send_counter_info(server, option));

        if (LoggerRegistry.__packets)
            LoggerRegistry.getLogger("packets").log(()-> packetCounter());

        for (EntityPlayer player: player_huds.keySet())
        {
            SPacketPlayerListHeaderFooter packet = new SPacketPlayerListHeaderFooter();
            packet.header = new TextComponentString("");
            packet.footer = Messenger.c(player_huds.get(player).toArray(new Object[0]));
            ((EntityPlayerMP)player).connection.sendPacket(packet);
        }
    }
    private static ITextComponent [] send_tps_display(MinecraftServer server)
    {
        double MSPT = MathHelper.average(server.tickTimeArray) * 1.0E-6D;
        double TPS = 1000.0D / Math.max((TickSpeed.time_warp_start_time != 0)?0.0:TickSpeed.mspt, MSPT);
        String color = Messenger.heatmap_color(MSPT,TickSpeed.mspt);
        return new ITextComponent[]{Messenger.c(
                "g TPS: ", String.format(Locale.US, "%s %.1f",color, TPS),
                "g  MSPT: ", String.format(Locale.US,"%s %.1f", color, MSPT))};
    }

    private static ITextComponent [] send_mobcap_display(int dim)
    {
        List<ITextComponent> components = new ArrayList<>();
        for (EnumCreatureType type:EnumCreatureType.values())
        {
            Tuple<Integer,Integer> counts = SpawnReporter.mobcaps.get(dim).getOrDefault(type, new Tuple<>(0,0));
            int actual = counts.getA(); int limit = counts.getB();
            components.add(Messenger.c(
                    (actual+limit == 0)?"g -":Messenger.heatmap_color(actual,limit)+" "+actual,
                    Messenger.creatureTypeColor(type)+" /"+((actual+limit == 0)?"-":limit)
                    ));
            components.add(Messenger.c("w  "));
        }
        components.remove(components.size()-1);
        return new ITextComponent[]{Messenger.c(components.toArray(new Object[0]))};
    }

    private static ITextComponent [] send_counter_info(MinecraftServer server, String color)
    {
        List <ITextComponent> res = HopperCounter.query_hopper_stats_for_color(server, color, false, true);
        return new ITextComponent[]{ Messenger.c(res.toArray(new Object[0]))};
    }
    private static ITextComponent [] packetCounter()
    {
        ITextComponent [] ret =  new ITextComponent[]{
                Messenger.c("w I/" + PacketCounter.totalIn + " O/" + PacketCounter.totalOut),
        };
        PacketCounter.reset();
        return ret;
    }
}
