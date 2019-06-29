package carpet.commands;

import carpet.settings.CarpetSettings;
import carpet.utils.Messenger;
import carpet.utils.PerimeterDiagnostics;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.EntitySummonArgument;
import net.minecraft.command.arguments.SuggestionProviders;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public class PerimeterInfoCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> command = literal("perimeterinfo").
                requires((player) -> CarpetSettings.commandPerimeterInfo).
                executes( (c) -> perimeterDiagnose(
                        c.getSource(),
                        new BlockPos(c.getSource().getPos()),
                        null)).
                then(argument("center position", BlockPosArgument.blockPos()).
                        executes( (c) -> perimeterDiagnose(
                                c.getSource(),
                                BlockPosArgument.getBlockPos(c, "center position"),
                                null)).
                        then(argument("mob",EntitySummonArgument.entitySummon()).
                                suggests(SuggestionProviders.SUMMONABLE_ENTITIES).
                                executes( (c) -> perimeterDiagnose(
                                        c.getSource(),
                                        BlockPosArgument.getBlockPos(c, "center position"),
                                        EntitySummonArgument.getEntityId(c, "mob").toString()
                                ))));
        dispatcher.register(command);
    }

    private static int perimeterDiagnose(CommandSource source, BlockPos pos, String mobId)
    {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        EntityLiving entityliving = null;
        if (mobId != null)
        {
            nbttagcompound.putString("id", mobId);
            entityliving = (EntityLiving) AnvilChunkLoader.readWorldEntityPos(nbttagcompound, source.getWorld(), pos.getX()+0.5, pos.getY()+2, pos.getZ()+0.5, true);
            if (entityliving == null)
            {
                Messenger.m(source, "r Failed to spawn test entity");
                return 0;
            }
        }
        PerimeterDiagnostics.Result res = PerimeterDiagnostics.countSpots(source.getWorld(), pos, entityliving);

        Messenger.m(source, "w Spawning spaces around ",Messenger.tp("c",pos), "w :");
        Messenger.m(source, "w   potential in-liquid: ","wb "+res.liquid);
        Messenger.m(source, "w   potential on-ground: ","wb "+res.ground);
        if (entityliving != null)
        {
            Messenger.m(source, "w   ", entityliving.getDisplayName() ,"w : ","wb "+res.specific);
            res.samples.forEach(bp -> Messenger.m(source, "w   ", Messenger.tp("c", bp)));
            entityliving.remove();
        }
        return 1;
    }
}
