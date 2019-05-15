package carpet.logging;

import carpet.utils.HUDController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;

public class HUDLogger extends Logger
{
    public HUDLogger(String logName, String def, String[] options)
    {
        super(logName, def, options);
    }

    @Override
    public void removePlayer(String playerName)
    {
        EntityPlayer player = playerFromName(playerName);
        if (player != null) HUDController.clear_player(player);
        super.removePlayer(playerName);
    }

    @Override
    public void sendPlayerMessage(EntityPlayer player, ITextComponent... messages)
    {
        for (ITextComponent m:messages) HUDController.addMessage(player, m);
    }


}
