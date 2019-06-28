package carpet.commands;

import carpet.settings.CarpetSettings;
import carpet.utils.DistanceCalculator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.Vec3Argument;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public class DistanceCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> command = literal("distance").
                requires((player) -> CarpetSettings.commandDistance).
                then(literal("from").
                        executes( (c) -> DistanceCalculator.setStart(c.getSource(), c.getSource().getPos())).
                        then(argument("from", Vec3Argument.vec3()).
                                executes( (c) -> DistanceCalculator.setStart(
                                        c.getSource(),
                                        Vec3Argument.getVec3(c, "from"))).
                                then(literal("to").
                                        executes((c) -> DistanceCalculator.distance(
                                                c.getSource(),
                                                Vec3Argument.getVec3(c, "from"),
                                                c.getSource().getPos())).
                                        then(argument("to", Vec3Argument.vec3()).
                                                executes( (c) -> DistanceCalculator.distance(
                                                        c.getSource(),
                                                        Vec3Argument.getVec3(c, "from"),
                                                        Vec3Argument.getVec3(c, "to")
                                                )))))).
                then(literal("to").
                        executes( (c) -> DistanceCalculator.setEnd(c.getSource(), c.getSource().getPos()) ).
                        then(argument("to", Vec3Argument.vec3()).
                                executes( (c) -> DistanceCalculator.setEnd(c.getSource(), Vec3Argument.getVec3(c, "to")))));
        dispatcher.register(command);
    }
}
