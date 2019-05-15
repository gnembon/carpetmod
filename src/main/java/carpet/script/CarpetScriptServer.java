package carpet.script;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.script.bundled.CameraPathModule;
import carpet.script.bundled.FileModule;
import carpet.script.bundled.ModuleInterface;
import carpet.utils.Messenger;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.lang.Math.max;
import static net.minecraft.command.Commands.literal;

public class CarpetScriptServer
{
    //make static for now, but will change that later:
    public ScriptHost globalHost;
    public Map<String, ScriptHost> modules;
    long tickStart;
    public boolean stopAll;

    public static List<ModuleInterface> bundledModuleData = new ArrayList<ModuleInterface>(){{
        add(new CameraPathModule());
    }};

    public CarpetScriptServer()
    {
        globalHost = createMinecraftScriptHost();
        modules = new HashMap<>();
        tickStart = 0L;
        stopAll = false;
        resetErrorSnooper();
    }

    ModuleInterface getModule(String name)
    {
        for (ModuleInterface moduleData : bundledModuleData)
        {
            if (moduleData.getName().equalsIgnoreCase(name))
            {
                return moduleData;
            }
        }

        File folder = CarpetServer.minecraft_server.getActiveAnvilConverter().getFile(
                CarpetServer.minecraft_server.getFolderName(), "scripts");
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null)
            return null;
        for (File script : listOfFiles)
        {
            if (script.getName().equalsIgnoreCase(name+".sc"))
            {
                return new FileModule(script);
            }
        }
        return null;
    }

    public List<String> listModules()
    {
        List<String> moduleNames = new ArrayList<>();
        for (ModuleInterface mi: bundledModuleData)
        {
            moduleNames.add(mi.getName());
        }
        File folder = CarpetServer.minecraft_server.getActiveAnvilConverter().getFile(
                CarpetServer.minecraft_server.getFolderName(), "scripts");
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null)
            return moduleNames;
        for (File script : listOfFiles)
        {
            if (script.getName().endsWith(".sc"))
            {
                moduleNames.add(script.getName().replaceFirst("\\.sc",""));
            }
        }
        return moduleNames;
    }


    private static ScriptHost createMinecraftScriptHost()
    {
        ScriptHost host = new ScriptHost();
        host.globalVariables.put("_x", (c, t) -> Value.ZERO);
        host.globalVariables.put("_y", (c, t) -> Value.ZERO);
        host.globalVariables.put("_z", (c, t) -> Value.ZERO);
        return host;
    }

    public int addScriptHost(String name)
    {
        ScriptHost newHost = createMinecraftScriptHost();
        // parse code and convert to expression
        // fill functions etc.
        // possibly add a command

        modules.put(name, newHost);
        addCommand(name, Arrays.asList("a","b"));
        return 1;
    }



    public void addCommand(String hostName, List<String> calls)
    {
        LiteralArgumentBuilder<CommandSource> command = literal(hostName).
                requires((player) -> CarpetServer.scriptServer.modules.containsKey(hostName)).
                executes( (c) -> { Messenger.m(c.getSource(), "w Running ", "rb "+hostName); return 1; });
        for (String call : calls)
        {
            command.then(literal(call).executes( (c) -> {
                Messenger.m(c.getSource(), "w Running ", "lb "+call, "w  from ","rb "+hostName); return 1;
            }));
        }
        CarpetServer.minecraft_server.getCommandManager().getDispatcher().register(command);
        CarpetSettings.notifyPlayersCommandsChanged();
    }

    public void setChatErrorSnooper(CommandSource source)
    {
        Expression.ExpressionException.errorSnooper = (expr, token, message) ->
        {
            try
            {
                source.asPlayer();
            }
            catch (CommandSyntaxException e)
            {
                return null;
            }
            String[] lines = expr.getCodeString().split("\n");

            String shebang = message;

            if (lines.length > 1)
            {
                shebang += " at line "+(token.lineno+1)+", pos "+(token.linepos+1);
            }
            else
            {
                shebang += " at pos "+(token.pos+1);
            }
            if (expr.getName() != null)
            {
                shebang += " in "+expr.getName()+"";
            }
            Messenger.m(source, "r "+shebang);

            if (lines.length > 1 && token.lineno > 0)
            {
                Messenger.m(source, "l "+lines[token.lineno-1]);
            }
            Messenger.m(source, "l "+lines[token.lineno].substring(0, token.linepos), "r  HERE>> ", "l "+
                    lines[token.lineno].substring(token.linepos));

            if (lines.length > 1 && token.lineno < lines.length-1)
            {
                Messenger.m(source, "l "+lines[token.lineno+1]);
            }
            return new ArrayList<>();
        };
    }
    public void resetErrorSnooper()
    {
        Expression.ExpressionException.errorSnooper=null;
    }

    public String invokeGlobalFunctionCommand(CommandSource source, String call, List<Integer> coords, String arg)
    {
        //will set a custom host when we have the other bits.
        ScriptHost host = globalHost;
        if (stopAll)
            return "SCRIPTING PAUSED";
        Expression.UserDefinedFunction acf = host.globalFunctions.get(call);
        if (acf == null)
            return "UNDEFINED";
        List<LazyValue> argv = new ArrayList<>();
        for (Integer i: coords)
            argv.add( (c, t) -> new NumericValue(i));
        String sign = "";
        for (Tokenizer.Token tok : Tokenizer.simplepass(arg))
        {
            switch (tok.type)
            {
                case VARIABLE:
                    if (host.globalVariables.containsKey(tok.surface.toLowerCase(Locale.ROOT)))
                    {
                        argv.add(host.globalVariables.get(tok.surface.toLowerCase(Locale.ROOT)));
                        break;
                    }
                case STRINGPARAM:
                    argv.add((c, t) -> new StringValue(tok.surface));
                    sign = "";
                    break;

                case LITERAL:
                    try
                    {
                        String finalSign = sign;
                        argv.add((c, t) ->new NumericValue(finalSign+tok.surface));
                        sign = "";
                    }
                    catch (NumberFormatException exception)
                    {
                        return "Fail: "+sign+tok.surface+" seems like a number but it is not a number. Use quotes to ensure its a string";
                    }
                    break;
                case HEX_LITERAL:
                    try
                    {
                        String finalSign = sign;
                        argv.add((c, t) -> new NumericValue(new BigInteger(finalSign+tok.surface.substring(2), 16).doubleValue()));
                        sign = "";
                    }
                    catch (NumberFormatException exception)
                    {
                        return "Fail: "+sign+tok.surface+" seems like a number but it is not a number. Use quotes to ensure its a string";
                    }
                    break;
                case OPERATOR:
                case UNARY_OPERATOR:
                    if ((tok.surface.equals("-") || tok.surface.equals("-u")) && sign.isEmpty())
                    {
                        sign = "-";
                    }
                    else
                    {
                        return "Fail: operators, like " + tok.surface + " are not allowed in invoke";
                    }
                    break;
                case FUNCTION:
                    return "Fail: passing functions like "+tok.surface+"() to invoke is not allowed";
                case OPEN_PAREN:
                case COMMA:
                case CLOSE_PAREN:
                    return "Fail: "+tok.surface+" is not allowed in invoke";
            }
        }
        List<String> args = acf.getArguments();
        if (argv.size() != args.size())
        {
            String error = "Fail: stored function "+call+" takes "+args.size()+" arguments, not "+argv.size()+ ":\n";
            for (int i = 0; i < max(argv.size(), args.size()); i++)
            {
                error += (i<args.size()?args.get(i):"??")+" => "+(i<argv.size()?argv.get(i).evalValue(null).getString():"??")+"\n";
            }
            return error;
        }
        try
        {
            // TODO: this is just for now - invoke would be able to invoke other hosts scripts
            Context context = new CarpetExpression.CarpetContext(host, source, BlockPos.ORIGIN);
            return Expression.evalValue(
                    () -> acf.lazyEval(context, Context.VOID, acf.expression, acf.token, argv),
                    context,
                    Context.VOID
            ).getString();
        }
        catch (Expression.ExpressionException e)
        {
            return e.getMessage();
        }
    }
}
