package carpet.utils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DistanceCalculator
{
    public static final HashMap<EntityPlayer, BlockPos> dist_pos = new HashMap<>();

    public static List<ITextComponent> print_distance_two_points(BlockPos pos1, BlockPos pos2)
    {
        int dx = MathHelper.abs(pos1.getX()-pos2.getX());
        int dy = MathHelper.abs(pos1.getY()-pos2.getY());
        int dz = MathHelper.abs(pos1.getZ()-pos2.getZ());
        int manhattan = dx+dy+dz;
        double spherical = MathHelper.sqrt(dx*dx + dy*dy + dz*dz);
        double cylindrical = MathHelper.sqrt(dx*dx + dz*dz);
        List<ITextComponent> res = new ArrayList<>();
        res.add(Messenger.c("w Distance between ",
                Messenger.tp("b",pos1),"w and ",
                Messenger.tp("b",pos2),"w :"));
        res.add(Messenger.c("w  - Manhattan: ", String.format("wb %d", manhattan)));
        res.add(Messenger.c("w  - Spherical: ", String.format("wb %.2f", spherical)));
        res.add(Messenger.c("w  - Cylindrical: ", String.format("wb %.2f", cylindrical)));
        return res;
    }

    public static void report_distance(EntityPlayer player, BlockPos pos)
    {
        if ( !(dist_pos.containsKey(player) ) )
        {
            dist_pos.put(player, pos);
            Messenger.m(player,"gi Set initial point to: ", Messenger.tp("gi",pos));
            return;
        }
        Messenger.send(player, print_distance_two_points( dist_pos.get(player), pos));
        dist_pos.remove(player);
    }
}
