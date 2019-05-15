package carpet.script;

import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;

public class CarpetContext extends Context
{
    public CommandSource s;
    public BlockPos origin;
    public CarpetContext(ScriptHost host, CommandSource source, BlockPos origin)
    {
        super(host);
        s = source;
        this.origin = origin;
    }

    @Override
    public Context recreate()
    {
        return new CarpetContext(this.host, this.s, this.origin);
    }

}
