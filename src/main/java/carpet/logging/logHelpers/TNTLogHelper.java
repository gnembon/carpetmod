package carpet.logging.logHelpers;

import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;

public class TNTLogHelper
{
    private double primedX, primedY, primedZ;
    private Vec3d primedAngle;
    public boolean initialized;
    /**
     * Runs when the TNT is primed. Expects the position and motion angle of the TNT.
     */
    public void onPrimed(double x, double y, double z, Vec3d velocity)
    {
        primedX = x;
        primedY = y;
        primedZ = z;
        primedAngle = velocity;
        initialized = true;
    }
    /**
     * Runs when the TNT explodes. Expects the position of the TNT.
     */
    public void onExploded(double x, double y, double z)
    {
        LoggerRegistry.getLogger("tnt").log( (option) -> {
            switch (option)
            {
                case "brief":
                    return new ITextComponent[]{Messenger.c(
                            "l P ",Messenger.dblt("l",primedX,primedY,primedZ),
                            "w  ",Messenger.dblt("l", primedAngle.x, primedAngle.y, primedAngle.z),
                            "r  E ",Messenger.dblt("r",x, y, z))};
                case "full":
                    return new ITextComponent[]{Messenger.c(
                            "l P ",Messenger.dblf("l",primedX,primedY,primedZ),
                            "w  ",Messenger.dblf("l", primedAngle.x, primedAngle.y, primedAngle.z),
                            "r  E ",Messenger.dblf("r",x, y, z))};
            }
            return null;
        });
    }

}
