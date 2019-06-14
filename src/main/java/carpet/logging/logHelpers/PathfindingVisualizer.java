package carpet.logging.logHelpers;

import carpet.logging.LoggerRegistry;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.arguments.ParticleArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.particles.IParticleData;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

public class PathfindingVisualizer
{
    private static IParticleData failedPath;
    private static IParticleData successfulPath;
    private static IParticleData lvl1;
    private static IParticleData lvl2;
    private static IParticleData lvl3;

    static
    {
        failedPath = parseParticle("angry_villager");
        successfulPath = parseParticle("happy_villager");
        lvl1 = parseParticle("dust 1 1 0 1");
        lvl2 = parseParticle("dust 1 0.5 0 1");
        lvl3 = parseParticle("dust 1 0 0 1");
    }

    private static void drawParticleLine(EntityPlayerMP player, Vec3d from, Vec3d to, float ratio, boolean successful)
    {
        IParticleData accent = successful ? successfulPath : failedPath;
        IParticleData color = (ratio < 2)? lvl1 : ((ratio < 4)?lvl2:lvl3);

        ((WorldServer)player.world).spawnParticle(
                player,
                accent,
                true,
                from.x, from.y, from.z, 5,
                0.5, 0.5, 0.5, 0.0);

        double lineLengthSq = from.squareDistanceTo(to);
        if (lineLengthSq == 0) return;

        Vec3d incvec = to.subtract(from).normalize();//    multiply(50/sqrt(lineLengthSq));
        int pcount = 0;
        for (Vec3d delta = new Vec3d(0.0,0.0,0.0);
             delta.lengthSquared()<lineLengthSq;
             delta = delta.add(incvec.scale(player.world.rand.nextFloat())))
        {
            ((WorldServer)player.world).spawnParticle(
                    player,
                    color,
                    true,
                    delta.x+from.x, delta.y+from.y, delta.z+from.z, 1,
                    0.0, 0.0, 0.0, 0.0);
        }
    }

    private static IParticleData parseParticle(String name)
    {
        try
        {
            return ParticleArgument.parseParticle(new StringReader(name));
        }
        catch (CommandSyntaxException e)
        {
            throw new RuntimeException("No such particle: "+name);
        }
    }


    public static void slowPath(Entity entity, Vec3d target, float miliseconds, boolean successful)
    {
        if (!LoggerRegistry.__pathfinding) return;
        LoggerRegistry.getLogger("pathfinding").log( (option, player)->
        {
            if (!(player instanceof EntityPlayerMP))
                return null;
            int minDuration = Integer.parseInt(option);
            if (miliseconds < minDuration)
                return null;
            if (player.getDistanceSq(entity) > 1000 && player.getDistanceSq(target) > 1000)
                return null;
            if (minDuration < 1)
                minDuration = 1;
            drawParticleLine((EntityPlayerMP) player, entity.getPositionVector(), target, miliseconds/minDuration, successful );
            return null;
        });
    }
}
