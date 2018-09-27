package carpet.commands;

import carpet.CarpetSettings;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

public class SpawnCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> literalargumentbuilder = Commands.literal("spawn").
                requires((player) -> CarpetSettings.getBool("commandSpawn"));
        literalargumentbuilder.then(Commands.literal("list"));

        dispatcher.register(literalargumentbuilder);
    }
}
