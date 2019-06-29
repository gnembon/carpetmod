package carpet.commands;

import carpet.settings.CarpetSettings;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.GameType;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public class CameraModeCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> camera = literal("c").
                requires((player) -> CarpetSettings.commandCameramode).
                executes((c) -> cameraMode(c.getSource(), c.getSource().asPlayer())).
                then(argument("player", EntityArgument.player()).
                        executes( (c) -> cameraMode(c.getSource(), EntityArgument.getPlayer(c, "player"))));

        LiteralArgumentBuilder<CommandSource> survival = literal("s").
                requires((player) -> CarpetSettings.commandCameramode).
                executes((c) -> survivalMode(
                        c.getSource(),
                        c.getSource().asPlayer())).
                then(argument("player", EntityArgument.player()).
                        executes( (c) -> survivalMode(c.getSource(), EntityArgument.getPlayer(c, "player"))));

        dispatcher.register(camera);
        dispatcher.register(survival);
    }
    private static boolean iCanHasPermissions(CommandSource source, EntityPlayer player)
    {
        try
        {
            return source.hasPermissionLevel(2) || source.asPlayer() == player;
        }
        catch (CommandSyntaxException e)
        {
            return true; // shoudn't happen because server has all permissions anyways
        }
    }
    private static int cameraMode(CommandSource source, EntityPlayer player)
    {
        if (!(iCanHasPermissions(source, player))) return 0;
        player.setGameType(GameType.SPECTATOR);
        player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 999999, 0, false, false));
        player.addPotionEffect(new PotionEffect(MobEffects.CONDUIT_POWER, 999999, 0, false, false));
        return 1;
    }
    private static int survivalMode(CommandSource source, EntityPlayer player)
    {
        if (!(iCanHasPermissions(source, player))) return 0;
        player.setGameType(GameType.SURVIVAL);
        player.removePotionEffect(MobEffects.NIGHT_VISION);
        player.removePotionEffect(MobEffects.CONDUIT_POWER);
        return 1;
    }

}
