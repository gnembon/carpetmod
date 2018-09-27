package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.util.math.BlockPos;

public class SpawnCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> literalargumentbuilder = Commands.literal("spawn").
                requires((player) -> CarpetSettings.getBool("commandSpawn"));

        literalargumentbuilder.
                then(Commands.literal("list").
                        then(Commands.argument("pos", BlockPosArgument.blockPos()).
                                executes( (c) -> list(c.getSource(), BlockPosArgument.getBlockPos(c, "pos")))));

        dispatcher.register(literalargumentbuilder);
    }

    private static int list(CommandSource source, BlockPos pos)
    {
        Messenger.m(source,"w listing spawns at position", Messenger.tp("wb",pos ));
        return 1;
    }
}
