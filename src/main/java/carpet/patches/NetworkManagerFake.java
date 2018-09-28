package carpet.patches;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.EnumPacketDirection;

public class NetworkManagerFake extends NetworkManager
{
    public NetworkManagerFake(EnumPacketDirection p)
    {
        super(p);
    }

    @Override
    public void disableAutoRead()
    {
    }

    @Override
    public void handleDisconnection()
    {
    }
}
