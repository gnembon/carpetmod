package carpet;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import net.minecraft.client.settings.CreativeSettings;
import net.minecraft.entity.player.EntityPlayerMP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//import carpet.carpetclient.CarpetClientRuleChanger;
//import carpet.utils.TickingArea;
import net.minecraft.server.MinecraftServer;

// /s /c commands tp players back where they started
// discuss xcom accurateBlockPlacement -> flexible or alternate block placement
// world.chunk.storage.Anvil Chunk Loader check again code on diff.
// check why 'test' world doesn't respond to commands while warping
// death counter
//unify command meter with other carpet commmands

public class CarpetSettings
{
    public static boolean locked = false;
    public static final String carpetVersion = "v18_10_11";

    public static final Logger LOG = LogManager.getLogger();
    private static final Map<String, CarpetSettingEntry> settings_store;
    public static final CarpetSettingEntry FalseEntry = CarpetSettingEntry.create("void","all","Error").choices("None","");

    public static final String[] default_tags = {"tnt","fix","survival","creative", "experimental","optimizations","feature","commands"}; //tab completion only
    
    static {
        settings_store = new HashMap<>();
        set_defaults();
    }

    //those don't have to mimic defaults - defaults will be '
    //static store
    public static int n_pushLimit = 12;
    public static boolean b_hopperCounters = false;
    public static int n_mobSpawningAlgorithm = 113;

    /*
    public static boolean extendedConnectivity = false;
    public static int pistonGhostBlocksFix = 0;
    public static boolean quasiConnectivity = true;

    public static boolean fastRedstoneDust = false;
    public static float tntRandomRange = -1;

    public static int railPowerLimit = 8;
    public static int waterFlow = 0;
    public static boolean wirelessRedstone;
    public static boolean optimizedTileEntities = false;
    public static boolean mergeTNT = false;
    public static boolean unloadedEntityFix = false;
    public static float hardcodeTNTangle = -1;
    public static boolean worldGenBug = false;
    public static boolean antiCheat = false;
    public static boolean optimizedTNT = false;
    public static boolean huskSpawningInTemples = false;
    public static boolean shulkerSpawningInEndCities = false;
    public static boolean redstoneMultimeter = false;
    public static boolean movableTileEntities = false;
    public static boolean fastMovingEntityOptimization = false;
    public static boolean blockCollisionsOptimization = false;
    public static boolean explosionNoBlockDamage = false;
    public static boolean movingBlockLightOptimization = false;
    public static boolean noteBlockImitation = false;
    public static boolean displayMobAI = false;
    public static boolean newLight = false;
    public static boolean doubleRetraction = false;
    public static boolean netherRNG = false;
    public static boolean endRNG = false;

    public static long setSeed = 0;
    */

    private static CarpetSettingEntry rule(String s1, String s2, String s3) { return CarpetSettingEntry.create(s1,s2,s3);}
    
    private static void set_defaults()
    {
        CarpetSettingEntry[] RuleList = new CarpetSettingEntry[] {
    rule("watchdogCrashFix", "fix", "Fixes server crashing supposedly on falling behind 60s in ONE tick, yeah bs.").
                                   extraInfo("Fixed 1.12 watchdog crash in 1.13 pre-releases, reintroduced with 1.13, GG."),
  //!rule("extendedConnectivity",  "experimental", "Quasi Connectivity doesn't require block updates.")
  //                              .extraInfo("All redstone components will send extra updates downwards",
  //                                         "Affects hoppers, droppers and dispensers"),
  //!rule("portalSuffocationFix",  "fix", "Nether portals correctly place entities going through")
  //                              .extraInfo("Entities shouldn't suffocate in obsidian"),
  //!rule("superSecretSetting",    "experimental","Gbhs sgnf sadsgras fhskdpri!"),
  /////rule("portalTeleportationFix", "fix", "Nether portals won't teleport you on occasion to 8x coordinates")
  //                              .extraInfo("It also prevents from taking random fire damage when going through portals"),
  //???rule("llamaOverfeedingFix",   "fix", "Prevents llamas from taking player food while breeding"),
  rule("invisibilityFix",       "fix", "Guardians honor players' invisibility effect"),
  rule("portalCreativeDelay",   "creative",  "Portals won't let a creative player go through instantly"),
  //                              .extraInfo("Holding obsidian in either hand won't let you through at all"),
  // !? rule("potionsDespawnFix",     "fix", "Allows mobs with potion effects to despawn outside of player range")
  //                              .extraInfo("Specifically effective to let witches drinking their own stuffs despawn"),
  rule("ctrlQCraftingFix",      "fix survival", "Dropping entire stacks works also from on the crafting UI result slot"),
  rule("persistentParrots",     "survival feature", "Parrots don't get of your shoulders until you receive damage"),
  //???rule("breedingMountingDisabled", "fix", "Prevents players from mounting animals when holding breeding food"),
  //!rule("growingUpWallJump",     "fix", "Mobs growing up won't glitch into walls or go through fences"),
  //!rule("reloadSuffocationFix",  "fix experimental", "Won't let mobs glitch into blocks when reloaded.")
  //                              .extraInfo("Can cause slight differences in mobs behaviour"),
  rule("xpNoCooldown",          "creative", "Players absorb XP instantly, without delay"),
  rule("combineXPOrbs",         "creative", "XP orbs combine with other into bigger orbs"),
  //!rule("stackableEmptyShulkerBoxes", "survival", "Empty shulker boxes can stack to 64 when dropped on the ground")
  //                              .extraInfo("To move them around between inventories, use shift click to move entire stacks"),
  //!rule("rideableGhasts",        "survival feature", "Named ghasts won't attack players and allow to be ridden and controlled")
  //                              .extraInfo("Hold a ghast tear to bring a tamed ghast close to you",
  //                                         "Use fire charges when riding to shoot fireballs",
  //                                         "Requires flying to be enabled on the server"),
  //!rule("explosionNoBlockDamage", "tnt", "Explosions won't destroy blocks"),
  //!rule("tntPrimerMomentumRemoved", "tnt", "Removes random TNT momentum when primed"),
  //!rule("fastRedstoneDust",      "experimental optimizations", "Lag optimizations for redstone Dust. By Theosib"),
  //<with modified protocol> rule("accurateBlockPlacement", "creative", "Allows to place blocks in different orientations. Requires Carpet Client")
  //                              .extraInfo("Also prevents rotations upon placement of dispensers and furnaces","when placed into a world by commands"),
  /////rule("optimizedTNT",          "tnt", "TNT causes less lag when exploding in the same spot and in liquids"),
  /////rule("huskSpawningInTemples", "experimental feature", "Only husks spawn in desert temples"),
  /////rule("shulkerSpawningInEndCities", "feature experimental", "Shulkers will respawn in end cities"),
  /////rule("watchdogFix",           "fix", "Fixes server crashing under heavy load and low tps")
  //                              .extraInfo("Won't prevent crashes if the server doesn't respond in max-tick-time ticks"),
  //!rule("wirelessRedstone",      "creative", "Repeater pointing from and to wool blocks transfer signals wirelessly")
  //                              .extraInfo("Temporary feature - repeaters need an update when reloaded",
  //                                         "By Narcoleptic Frog"),
  //???rule("optimizedTileEntities", "experimental", "Reduces the lag caused by tile entities.")
  //                              .extraInfo("By PallaPalla"),
  /////rule("mergeTNT",              "tnt", "Merges stationary primed TNT entities"),
  //???<no block data anymore> rule("repeaterPoweredTerracota", "experimental creative", "Repeater delays depends on stained hardened clay aka terracotta on which they are placed")
  //                              .extraInfo("1 to 15 gt per delay added (1-15 block data), 0 (white) adds 100gt per tick"),
  //!rule("unloadedEntityFix",     "experimental creative", "Entities pushed or moved into unloaded chunks no longer disappear"),
  rule("TNTDoNotUpdate",        "tnt", "TNT doesn't update when placed against a power source"),
  rule("antiCheatSpeed",        "creative surival", "Prevents players from rubberbanding when moving too fast"),
  rule("quasiConnectivity",     "creative", "Pistons, droppers and dispensers react if block above them is powered")
                                .defaultTrue(),
  rule("flippinCactus",         "creative survival", "Players can flip and rotate blocks when holding cactus")
                                .extraInfo("Doesn't cause block updates when rotated/flipped",
                                           "Applies to pistons, observers, droppers, repeaters, stairs, glazed terracotta etc..."),
  rule("hopperCounters",        "commands creative survival","hoppers pointing to wool will count items passing through them")
                                .extraInfo("Enables /counter command, and actions while placing red and green carpets on wool blocks",
                                           "Use /counter <color?> reset to reset the counter, and /counter <color?> to query",
                                           "In survival, place green carpet on same color wool to query, red to reset the counters",
                                           "Counters are global and shared between players, 16 channels available",
                                           "Items counted are destroyed, count up to one stack per tick per hopper")
                                .isACommand().boolAccelerate().defaultFalse(),
  //! rename rule("renewableElderGuardians", "experimental feature", "Guardians turn into Elder Guardian when struck by lightning"),
  //rule("optimizedDespawnRange", "optimizations", "Spawned mobs that would otherwise despawn immediately, won't be placed in world"), // use 1.14 spawning instead
  //???rule("redstoneMultimeter",    "creative survival", "Enables integration with redstone multimeter mod")
  //                              .extraInfo("Required clients with RSMM Mod by Narcoleptic Frog. Enables multiplayer experience with RSMM Mod"),
  //! will try rule("movableTileEntities",   "experimental", "Pistons can push tile entities, like hoppers, chests etc."),
  //!rule("displayMobAI",          "creative", "Uses nametags to display current mobs AI tasks"),
  //???rule("fastMovingEntityOptimization", "experimental", "Optimized movement calculation or very fast moving entities"),
  //???rule("blockCollisionsOptimization", "experimental", "Optimized entity-block collision calculations. By masa"),
  //!rule("desertShrubs",          "feature", "Saplings turn into dead shrubs in hot climates and no water access when it attempts to grow into a tree"),
  /////rule("nitwitCrafter",         "experimental", "Nitwit villagers will have 3 hidden crafting recipes they can craft")
  //                              .extraInfo("They require food for crafting and prefer a specific food type to craft faster.",
  //                                         "They have one crafting recipe to start out and unlock there higher recipes as they craft",
  //                                         "The nitwits will craft faster as they progress",
  //                                         "When a crafting table is nearby they will throw the product towards it",
  //                                         "They need a crafting table to craft tier 2 and higher recipes"),
  //???rule("boundingBoxFix",        "fix", "Structure bounding boxes (i.e. witch huts) will generate correctly")
  //                              .extraInfo("Fixes spawning issues due to incorrect bounding boxes"),
  /////rule("movingBlockLightOptimization", "optimizations", "Blocks inherit the original light opacity and light values while being pushed with a piston"),
  /////rule("noteBlockImitationOf1.13", "experimental", "Note blocks have update capabilities behaviour from 1.13"),
  /////rule("hopperDuplicationFix",  "fix", "Hopper duplication fix by Theosib. Fixed in 1.12.2").defaultTrue(),
  //??? check vanilla first rule("entityDuplicationFix",  "fix", "Chunk saving issues that causes entites and blocks to duplicate or dissapear")
  //                              .extraInfo("By Theosib"),
  /////rule("itemFrameDuplicationFix", "fix", "Fixes duplication of items when using item frames"),
  /////rule("craftingWindowDuplicationFix", "fix", "Fixes the recipe book duplication caused by clicking to fast while crafting"),
  rule("silverFishDropGravel",  "experimental", "Silverfish drop a gravel item when breaking out of a block"),
  /////rule("renewablePackedIce",    "experimental", "Multiple ice crushed by falling anvils make packed ice"),
  /////rule("renewableDragonEggs",   "experimental", "Dragon eggs when fed meet items spawn more eggs"),
  //!rule("summonNaturalLightning","creative", "summoning a lightning bolt has all the side effects of natural lightning"),
  rule("commandSpawn",          "commands", "Enables /spawn command for spawn tracking").isACommand(),
  rule("commandTick",           "commands", "Enables /tick command to control game speed").isACommand(),
  rule("commandLog",            "commands", "Enables /log command to monitor events in the game via chat and overlays").isACommand(),
  rule("commandDistance",       "commands", "Enables /distance command to measure in game distance between points").isACommand()
                                .extraInfo("Also enables brown carpet placement action if 'carpets' rule is turned on as well"),
  rule("commandInfo",           "commands", "Enables /info command for blocks and entities").isACommand()
                                .extraInfo("Also enables gray carpet placement action ")
                                .extraInfo("and yellow carpet placement action for entities if 'carpets' rule is turned on as well"),
  ////rule("commandUnload",         "commands", "Enables /unload command to control game speed").defaultTrue(),
  rule("commandCameramode",     "commands", "Enables /c and /s commands to quickly switch between camera and survival modes").isACommand()
                                .extraInfo("/c and /s commands are available to all players regardless of their permission levels"),
  rule("commandPerimeterInfo",  "commands", "Enables /perimeterinfo command").isACommand()
                                .extraInfo("... that scans the area around the block for potential spawnable spots"),
  rule("commandPlayer",         "commands", "Enables /player command to control/spawn players").isACommand(),
  ////rule("commandRNG",            "commands", "Enables /rng command to manipulate and query rng").defaultTrue(),
  ////rule("newLight",              "optimizations", "Uses alternative lighting engine by PhiPros. AKA NewLight mod"),
  //!rule("carpets",               "survival", "Placing carpets may issue carpet commands for non-op players"),
  rule("missingTools",          "survival", "Pistons, Glass and Sponge can be broken faster with their appropriate tools"),
  rule("mobSpawningAlgorithm","experimental","Using version appropriate spawning rules: ")
                                .extraInfo(" - 1.8 : fixed 4 mobs per pack for all mobs, 'subchunk' rule",
                                           " - 1.12 : fixed 1 to 4 pack size, ignoring entity collisions, subchunk rule",
                                           " - 1.13 : vanilla",
                                           " - 1.14 : mobs don't spawn outside of 128 sphere around players")
                                .choices("1.13","1.8 1.12 1.13 1.14")
                                .validate( (s) -> {
                                    String value = CarpetSettings.getString("mobSpawningAlgorithm");
                                    CarpetSettings.n_mobSpawningAlgorithm = 113;
                                    switch (value)
                                    {
                                        case "1.8":
                                            CarpetSettings.n_mobSpawningAlgorithm = 18;
                                            break;
                                        case "1.9":
                                        case "1.10":
                                        case "1.11":
                                        case "1.12":
                                            CarpetSettings.n_mobSpawningAlgorithm = 112;
                                            break;
                                        case "1.14":
                                            CarpetSettings.n_mobSpawningAlgorithm = 114;
                                            break;
                                    }
                                }),
  /////rule("pocketPushing",         "experimental", "Reintroduces piston warping/translocation bug"),
  rule("portalCaching",         "survival experimental", "Alternative, persistent cashing strategy for nether portals"),
  rule("calmNetherFires",       "experimental", "Permanent fires don't schedule random updates"),
  /////rule("observersDoNonUpdate",  "creative", "Observers don't pulse when placed"),
  //!rule("flyingMachineTransparent", "creative", "Transparent observers, TNT and redstone blocks. May cause lighting artifacts"),
  rule("fillUpdates",           "creative", "fill/clone/setblock and structure blocks cause block updates").defaultTrue(),
  rule("pushLimit",             "creative","Customizable piston push limit")
                                .choices("12","10 12 14 100").setNotStrict().numAccelerate(),
  //!rule("railPowerLimit",        "creative", "Customizable powered rail power range")
  //                              .choices("9","9 15 30").setNotStrict(),
  rule("fillLimit",             "creative","Customizable fill/clone volume limit")
                                .choices("32768","32768 250000 1000000").setNotStrict(),
  //!rule("maxEntityCollisions",   "optimizations", "Customizable maximal entity collision limits, 0 for no limits")
  //                              .choices("0","0 1 20").setNotStrict(),
  //???rule("pistonGhostBlocksFix",  "fix", "Fix for piston ghost blocks")
  //                              .extraInfo("true(serverOnly) option works with all clients, including vanilla",
  //                              "clientAndServer option requires compatible carpet clients and messes up flying machines")
  //                              .choices("false","false true clientAndServer"),
  //???rule("waterFlow",             "optimizations", "fixes water flowing issues")
  //                              .choices("vanilla","vanilla optimized correct"),
  /////rule("hardcodeTNTangle",      "tnt", "Sets the horizontal random angle on TNT for debugging of TNT contraptions")
  //                              .extraInfo("Set to -1 for default behaviour")
  //                              .choices("-1","-1")
  //                              .setFloat(),
  /////rule("tntRandomRange",        "tnt", "Sets the tnt random explosion range to a fixed value")
  //                              .extraInfo("Set to -1 for default behaviour")
  //                              .choices("-1","-1")
  //                              .setFloat(),
  //!rule("sleepingThreshold",     "experimental", "The percentage of required sleeping players to skip the night")
  //                              .extraInfo("Use values from 0 to 100, 100 for default (all players needed)")
  //                              .choices("100","0 10 50 100").setNotStrict(),
  //???rule("spongeRandom",          "experimental feature", "sponge responds to random ticks"),
  //!rule("customMOTD",            "creative","Sets a different motd message on client trying to connect to the server")
  //                              .extraInfo("use '_' to use the startup setting from server.properties")
  //                              .choices("_","_").setNotStrict(),
  /////rule("doubleRetraction",      "experimental", "1.8 double retraction from pistons.")
  //                              .extraInfo("Gives pistons the ability to double retract without side effects."),
  rule("rotatorBlock",          "experimental", "Cactus in dispensers rotates blocks.")
                                .extraInfo("Cactus in a dispenser gives the dispenser the ability to rotate the blocks that are in front of it anti-clockwise if possible."),
  /////rule("netherRNG",             "creative", "Turning nether RNG manipulation on or off.")
  //                              .extraInfo("Turning nether RNG manipulation on or off."),
  /////rule("endRNG",                "creative", "Turning end RNG manipulation on or off.")
  //                              .extraInfo("Turning end RNG manipulation on or off."),
  //!rule("viewDistance",          "creative", "Changes the view distance of the server.")
  //                              .extraInfo("Set to 0 to not override the value in server settings.")
  //                              .choices("0", "0 12 16 32 64").setNotStrict(),
  /////rule("tickingAreas",          "creative", "Enable use of ticking areas.")
  //                              .extraInfo("As set by the /tickingarea comamnd.",
  //                              "Ticking areas work as if they are the spawn chunks."),
  //!rule("disableSpawnChunks",    "creative", "Removes the spawn chunks."),
  rule("kelpGenerationGrowLimit", "feature", "limits growth limit of newly naturally generated kelp to this amount of blocks")
                                  .choices("25", "0 2 25").setNotStrict(),
  rule("renewableCoral",          "feature", "Alternative cashing strategy for nether portals"),
  rule("placementRotationFix",              "fix", "fixes block placement rotation issue when player rotates quickly while placing blocks"),
        };
        for (CarpetSettingEntry rule: RuleList)
        {
            settings_store.put(rule.getName(), rule);
        }
    }

    /* should not be needed
    public static void reload_all_statics()
    {
        for (String rule: settings_store.keySet())
        {
            reload_stat(rule);
        }
    }
    */

    /* should not be needed due to validators
    public static void reload_stat(String rule)
    {
    }

        extendedConnectivity = CarpetSettings.getBool("extendedConnectivity");
        quasiConnectivity = CarpetSettings.getBool("quasiConnectivity");
        hopperCounters = CarpetSettings.getBool("hopperCounters");
        fastRedstoneDust = CarpetSettings.getBool("fastRedstoneDust");
        wirelessRedstone = CarpetSettings.getBool("wirelessRedstone");
        unloadedEntityFix = CarpetSettings.getBool("unloadedEntityFix");
        optimizedTileEntities = CarpetSettings.getBool("optimizedTileEntities");
        hardcodeTNTangle = CarpetSettings.getFloat("hardcodeTNTangle");
        tntRandomRange = CarpetSettings.getFloat("tntRandomRange");
        antiCheat = CarpetSettings.getBool("antiCheatSpeed");
        optimizedTNT = CarpetSettings.getBool("optimizedTNT");
        huskSpawningInTemples = CarpetSettings.getBool("huskSpawningInTemples");
        redstoneMultimeter = CarpetSettings.getBool("redstoneMultimeter");
        movableTileEntities = CarpetSettings.getBool("movableTileEntities");
        fastMovingEntityOptimization = CarpetSettings.getBool("fastMovingEntityOptimization");
        blockCollisionsOptimization = CarpetSettings.getBool("blockCollisionsOptimization");
        explosionNoBlockDamage = CarpetSettings.getBool("explosionNoBlockDamage");
        movingBlockLightOptimization = CarpetSettings.getBool("movingBlockLightOptimization");
        noteBlockImitation = CarpetSettings.getBool("noteBlockImitationOf1.13");
        displayMobAI = CarpetSettings.getBool("displayMobAI");
        newLight = CarpetSettings.getBool("newLight");
        doubleRetraction = CarpetSettings.getBool("doubleRetraction");
        netherRNG = CarpetSettings.getBool("netherRNG");
        endRNG = CarpetSettings.getBool("endRNG");

        if ("pistonGhostBlocksFix".equalsIgnoreCase(rule))
        {
            pistonGhostBlocksFix = 0;
            if("true".equalsIgnoreCase(CarpetSettings.getString("pistonGhostBlocksFix")))
            {
                pistonGhostBlocksFix = 1;
            }
            if("clientAndServer".equalsIgnoreCase(CarpetSettings.getString("pistonGhostBlocksFix")))
            {
                pistonGhostBlocksFix = 2;
            }
        }
        else if ("flyingMachineTransparent".equalsIgnoreCase(rule))
        {
            if(CarpetSettings.getBool("flyingMachineTransparent"))
            {
                Blocks.OBSERVER.setLightOpacity(0);
                Blocks.REDSTONE_BLOCK.setLightOpacity(0);
                Blocks.TNT.setLightOpacity(0);
            }
            else
            {
                Blocks.OBSERVER.setLightOpacity(255);
                Blocks.REDSTONE_BLOCK.setLightOpacity(255);
                Blocks.TNT.setLightOpacity(255);
            }
        }
        else if ("liquidsNotRandom".equalsIgnoreCase(rule))
        {
            if(CarpetSettings.getBool("liquidsNotRandom"))
            {
                worldGenBug = true;
                Blocks.FLOWING_WATER.setTickRandomly(false);
                Blocks.FLOWING_LAVA.setTickRandomly(false);
            }
            else
            {
                worldGenBug = false;
                Blocks.FLOWING_WATER.setTickRandomly(true);
                Blocks.FLOWING_LAVA.setTickRandomly(true);
            }
        }
        else if ("spongeRandom".equalsIgnoreCase(rule))
        {
            if(CarpetSettings.getBool("spongeRandom"))
            {
                Blocks.SPONGE.setTickRandomly(true);
            }
            else
            {
                Blocks.SPONGE.setTickRandomly(false);
            }
        }
        else if ("reloadSuffocationFix".equalsIgnoreCase(rule))
        {
            if(CarpetSettings.getBool("reloadSuffocationFix"))
            {
                AxisAlignedBB.margin = 1.0 / (1L<<27);
            }
            else
            {
                AxisAlignedBB.margin = 0;
            }
        }
        else if("pushLimit".equalsIgnoreCase(rule))
        {
            pushLimit = getInt("pushLimit");
        }
        else if("railPowerLimit".equalsIgnoreCase(rule))
        {
            // Rail limit -1 because 8 is the code default. But counted to 9 including the source in human terms.
            railPowerLimit = getInt("railPowerLimit") - 1;
        }
        else if("waterFlow".equalsIgnoreCase(rule))
        {
            waterFlow = 0;
            if ("optimized".equalsIgnoreCase(getString("waterFlow")))
            {
                waterFlow = 3;
            }
            if ("correct".equalsIgnoreCase(getString("waterFlow")))
            {
                waterFlow = 1;
            }
        }
        else if("shulkerSpawningInEndCities".equalsIgnoreCase(rule))
        {
            if(CarpetSettings.getBool("shulkerSpawningInEndCities"))
            {
                net.minecraft.world.gen.structure.MapGenEndCity.shulkerSpawning(true);
                shulkerSpawningInEndCities = true;
            }
            else
            {
                net.minecraft.world.gen.structure.MapGenEndCity.shulkerSpawning(false);
                shulkerSpawningInEndCities = false;
            }
        }
        else if ("viewDistance".equalsIgnoreCase(rule))
        {
            int viewDistance = getInt("viewDistance");
            if (viewDistance < 2)
                viewDistance = ((DedicatedServer) CarpetServer.minecraft_server).getIntProperty("view-distance", 10);
            if (viewDistance > 64)
                viewDistance = 64;
            if (viewDistance != CarpetServer.minecraft_server.getPlayerList().getViewDistance())
                CarpetServer.minecraft_server.getPlayerList().setViewDistance(viewDistance);
        }
        else if ("tickingAreas".equalsIgnoreCase(rule))
        {
            if (CarpetSettings.getBool("tickingAreas") && CarpetServer.minecraft_server.worlds != null)
            {
                TickingArea.initialChunkLoad(CarpetServer.minecraft_server, false);
            }
        }
        else if ("disableSpawnChunks".equalsIgnoreCase(rule))
        {
            if (!CarpetSettings.getBool("disableSpawnChunks") && CarpetServer.minecraft_server.worlds != null)
            {
                World overworld = CarpetServer.minecraft_server.worlds[0];
                for (ChunkPos chunk : new TickingArea.SpawnChunks().listIncludedChunks(overworld))
                {
                    overworld.getChunkProvider().provideChunk(chunk.x, chunk.z);
                }
            }
        }
    }
    */
    private static void notifyPlayersCommandsChanged()
    {
        if (CarpetServer.minecraft_server == null)
        {
            return;
        }
        for (EntityPlayerMP entityplayermp : CarpetServer.minecraft_server.getPlayerList().getPlayers())
        {
            CarpetServer.minecraft_server.getCommandManager().sendCommandListPacket(entityplayermp);
        }
    }

    public static void apply_settings_from_conf(MinecraftServer server)
    {
        Map<String, String> conf = read_conf(server);
        boolean is_locked = locked;
        locked = false;
        if (is_locked)
        {
            LOG.info("[CM]: Carpet Mod is locked by the administrator");
        }
        for (String key: conf.keySet())
        {
            set(key, conf.get(key));
            LOG.info("[CM]: loaded setting "+key+" as "+conf.get(key)+" from carpet.conf");
        }
        locked = is_locked;
    }
    private static void disable_commands_by_default()
    {
        for (CarpetSettingEntry entry: settings_store.values())
        {
            if (entry.getName().startsWith("command"))
            {
                entry.defaultFalse();
            }
        }
    }

    private static Map<String, String> read_conf(MinecraftServer server)
    {
        try
        {
            File settings_file = server.getActiveAnvilConverter().getFile(server.getFolderName(), "carpet.conf");
            BufferedReader b = new BufferedReader(new FileReader(settings_file));
            String line = "";
            Map<String,String> result = new HashMap<String, String>();
            while ((line = b.readLine()) != null)
            {
                line = line.replaceAll("\\r|\\n", "");
                if ("locked".equalsIgnoreCase(line))
                {
                    disable_commands_by_default();
                    locked = true;
                }
                String[] fields = line.split("\\s+",2);
                if (fields.length > 1)
                {
                    if (get(fields[0])==FalseEntry)
                    {
                        LOG.error("[CM]: Setting " + fields[0] + " is not a valid - ignoring...");
                        continue;
                    }
                    if (!(Arrays.asList(get(fields[0]).getOptions()).contains(fields[1])) && get(fields[0]).isStrict())
                    {
                        LOG.error("[CM]: The value of " + fields[1] + " for " + fields[0] + " is not valid - ignoring...");
                        continue;
                    }
                    result.put(fields[0],fields[1]);
                }
            }
            b.close();
            return result;
        }
        catch(FileNotFoundException e)
        {
            return new HashMap<>();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return new HashMap<>();
        }
        
    }
    private static void write_conf(MinecraftServer server, Map<String, String> values)
    {
        if (locked) return;
        try
        {
            File settings_file = server.getActiveAnvilConverter().getFile(server.getFolderName(), "carpet.conf");
            FileWriter fw = new FileWriter(settings_file);
            for (String key: values.keySet())
            {
                fw.write(key+" "+values.get(key)+"\n");
            }
            fw.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            LOG.error("[CM]: failed write the carpet.conf");
        }
    }
    
    // stores different defaults in the file
    public static boolean setDefaultRule(MinecraftServer server, String setting_name, String string_value)
    {
        if (locked) return false;
        if (settings_store.containsKey(setting_name))
        {
            Map<String, String> conf = read_conf(server);
            conf.put(setting_name, string_value);
            write_conf(server, conf);
            set(setting_name,string_value);
            return true;
        }
        return false;
    }
    // removes overrides of the default values in the file  
    public static boolean removeDefaultRule(MinecraftServer server, String setting_name)
    {
        if (locked) return false;
        if (settings_store.containsKey(setting_name))
        {
            Map<String, String> conf = read_conf(server);
            conf.remove(setting_name);
            write_conf(server, conf);
            set(setting_name,get(setting_name).getDefault());
            return true;
        }
        return false;
    }

    //changes setting temporarily
    public static boolean set(String setting_name, String string_value)
    {
        CarpetSettingEntry en = get(setting_name);
        if (en != FalseEntry)
        {
            en.set(string_value);
            //reload_stat(setting_name);
            //CarpetClientRuleChanger.updateCarpetClientsRule(setting_name, string_value);
            return true;
        }
        return false;
    }

    // used as CarpetSettings.get("pushLimit").integer to get the int value of push limit
    public static CarpetSettingEntry get(String setting_name)
    {
        if (!settings_store.containsKey(setting_name) )
        {
            return FalseEntry;
        }
        return settings_store.get(setting_name);
    } 
    
    public static int getInt(String setting_name)
    {
        return get(setting_name).getIntegerValue();
    }
    public static boolean getBool(String setting_name)
    {
        return get(setting_name).getBoolValue();
    }
    public static String getString(String setting_name) { return get(setting_name).getStringValue(); }
    public static float getFloat(String setting_name)
    {
        return get(setting_name).getFloatValue();
    }
    public static CarpetSettingEntry[] findAll(String tag)
    {
        ArrayList<CarpetSettingEntry> res = new ArrayList<CarpetSettingEntry>();
        for (String rule: settings_store.keySet().stream().sorted().collect(Collectors.toList()))
        {
            if (tag == null || settings_store.get(rule).matches(tag))
            {
                res.add(settings_store.get(rule));
            }
        }
        return res.toArray(new CarpetSettingEntry[0]);
    }
    public static CarpetSettingEntry[] find_nondefault(MinecraftServer server)
    {
        ArrayList<CarpetSettingEntry> res = new ArrayList<CarpetSettingEntry>();
        Map <String,String> defaults = read_conf(server);
        for (String rule: settings_store.keySet().stream().sorted().collect(Collectors.toList()))
        {
            if (!settings_store.get(rule).isDefault() || defaults.containsKey(rule))
            {
                res.add(settings_store.get(rule));
            }
        }
        return res.toArray(new CarpetSettingEntry[0]);
    }
    public static CarpetSettingEntry[] findStartupOverrides(MinecraftServer server)
    {
        ArrayList<CarpetSettingEntry> res = new ArrayList<CarpetSettingEntry>();
        if (locked) return res.toArray(new CarpetSettingEntry[0]);
        Map <String,String> defaults = read_conf(server);
        for (String rule: settings_store.keySet().stream().sorted().collect(Collectors.toList()))
        {
            if (defaults.containsKey(rule))
            {
                res.add(settings_store.get(rule));
            }
        }
        return res.toArray(new CarpetSettingEntry[0]);
    }
    public static String[] toStringArray(CarpetSettingEntry[] entry_array)
    {
        return Stream.of(entry_array).map(CarpetSettingEntry::getName).toArray( String[]::new );
    }
    public static ArrayList<CarpetSettingEntry> getAllCarpetSettings()
    {
        ArrayList<CarpetSettingEntry> res = new ArrayList<CarpetSettingEntry>();
        for (String rule: settings_store.keySet().stream().sorted().collect(Collectors.toList()))
        {
            res.add(settings_store.get(rule));
        }
        
        return res;
    }
    public static CarpetSettingEntry getCarpetSetting(String rule)
    {
        return settings_store.get(rule);
    }
    
    public static void resetToVanilla()
    {
        for (String rule: settings_store.keySet())
        {
            get(rule).reset();
            //reload_stat(rule);
        }
    }
    
    public static void resetToUserDefaults(MinecraftServer server)
    {
        resetToVanilla();
        apply_settings_from_conf(server);
    }
    
    public static void resetToCreative()
    {
        resetToBugFixes();
        set("fillLimit","500000");
        set("fillUpdates","false");
        set("portalCreativeDelay","true");
        set("portalCaching","true");
        set("flippinCactus","true");
        set("hopperCounters","true");
        set("antiCheatSpeed","true");
        
    }
    public static void resetToSurvival()
    {
        resetToBugFixes();
        set("ctrlQCraftingFix","true");
        set("persistentParrots", "true");
        set("stackableEmptyShulkerBoxes","true");
        set("flippinCactus","true");
        set("hopperCounters","true");
        set("carpets","true");
        set("missingTools","true");
        set("portalCaching","true");
        set("miningGhostBlocksFix","true");
    }
    public static void resetToBugFixes()
    {
        resetToVanilla();
        set("portalSuffocationFix","true");
        set("pistonGhostBlocksFix","serverOnly");
        set("portalTeleportationFix","true");
        set("entityDuplicationFix","true");
        set("inconsistentRedstoneTorchesFix","true");
        set("llamaOverfeedingFix","true");
        set("invisibilityFix","true");
        set("potionsDespawnFix","true");
        set("liquidsNotRandom","true");
        set("mobsDontControlMinecarts","true");
        set("breedingMountingDisabled","true");
        set("growingUpWallJump","true");
        set("reloadSuffocationFix","true");
        set("watchdogFix","true");
        set("unloadedEntityFix","true");
        set("hopperDuplicationFix","true");
        set("calmNetherFires","true");
    }

    public static class CarpetSettingEntry 
    {
        private String rule;
        private String string;
        private int integer;
        private boolean bool;
        private float flt;
        private String[] options;
        private String[] tags;
        private String toast;
        private String[] extra_info;
        private String default_string_value;
        private boolean isFloat;
        private boolean strict;
        private List<Consumer<String>> validators;

        //factory
        public static CarpetSettingEntry create(String rule_name, String tags, String toast)
        {
            return new CarpetSettingEntry(rule_name, tags, toast);
        }
        private CarpetSettingEntry(String rule_name, String tags_string, String toast_string)
        {
            set("false");
            rule = rule_name;
            default_string_value = string;
            tags = tags_string.split("\\s+"); // never empty
            toast = toast_string;
            options = "true false".split("\\s+");
            isFloat = false;
            extra_info = null;
            strict = true;
            validators = null;
        }
        public CarpetSettingEntry defaultTrue()
        {
            set("true");
            default_string_value = string;
            options = "true false".split("\\s+");
            return this;
        }
        public CarpetSettingEntry validate(Consumer<String> method)
        {
            if (validators == null)
            {
                validators = new ArrayList<>();
            }
            validators.add(method);
            return this;
        }
        public CarpetSettingEntry boolAccelerate()
        {
            Consumer<String> validator = (name) -> {
                try
                {
                    Field f = CarpetSettings.class.getDeclaredField("b_"+name);
                    f.setBoolean(null, CarpetSettings.getBool(name));
                }
                catch (IllegalAccessException e)
                {
                    CarpetSettings.LOG.error("[CM Error] rule "+name+" has wrong access to boolean accelerator");
                }
                catch (NoSuchFieldException e)
                {
                    CarpetSettings.LOG.error("[CM Error] rule "+name+" doesn't have a boolean accelerator");
                }
            };
            return validate(validator);
        }
        public CarpetSettingEntry numAccelerate()
        {
            Consumer<String> validator = (name) -> {
                try
                {
                    Field f = CarpetSettings.class.getDeclaredField("n_"+name);
                    if (CarpetSettings.get(name).isFloat)
                    {
                        f.setDouble(null, (double) CarpetSettings.getFloat(name));
                    }
                    else
                    {
                        f.setInt(null, CarpetSettings.getInt(name));
                    }
                }
                catch (IllegalAccessException e)
                {
                    CarpetSettings.LOG.error("[CM Error] rule "+name+" wrong type of numerical accelerator");
                }
                catch (NoSuchFieldException e)
                {
                    CarpetSettings.LOG.error("[CM Error] rule "+name+" doesn't have a numerical accelerator");
                }
            };
            return validate(validator);
        }


        public CarpetSettingEntry isACommand()
        {
            return this.defaultTrue().validate( (s) -> notifyPlayersCommandsChanged());
        }

        public CarpetSettingEntry defaultFalse()
        {
            set("false");
            default_string_value = string;
            options = "true false".split("\\s+");
            return this;
        }

        public CarpetSettingEntry choices(String defaults, String options_string)
        {
            set(defaults);
            default_string_value = string;
            options =  options_string.split("\\s+");
            return this;
        }
        public CarpetSettingEntry extraInfo(String ... extra_info_string)
        {
            extra_info = extra_info_string;
            return this;
        }

        public CarpetSettingEntry setFloat()
        {
            isFloat = true;
            strict = false;
            return this;
        }

        public CarpetSettingEntry setNotStrict()
        {
            strict = false;
            return this;
        }

        private void set(String unparsed)
        {
            string = unparsed;
            try
            {
                integer = Integer.parseInt(unparsed);
            }
            catch(NumberFormatException e)
            {
                integer = 0;
            }
            try
            {
                flt = Float.parseFloat(unparsed);
            }
            catch(NumberFormatException e)
            {
                flt = 0.0F;
            }
            bool = (integer > 0)?true:Boolean.parseBoolean(unparsed);
            if (validators != null)
            {
                validators.forEach((r) -> r.accept(this.getName()));
            }
        }

        //accessors
        public boolean isDefault() { return string.equals(default_string_value); }
        public String getDefault() { return default_string_value; }
        public String toString() { return rule + ": " + string; }
        public String getToast() { return toast; }
        public String[] getInfo() { return extra_info == null?new String[0]:extra_info; }
        public String[] getOptions() { return options;}
        public String[] getTags() { return tags; }
        public String getName() { return rule; }
        public String getStringValue() { return string; }
        public boolean getBoolValue() { return bool; }
        public int getIntegerValue() { return integer; }
        public float getFloatValue() { return flt; }
        public boolean getIsFloat() { return isFloat;}

        //actual stuff
        public void reset()
        {
            set(default_string_value);
        }

        public boolean matches(String tag)
        {
            tag = tag.toLowerCase();
            if (rule.toLowerCase().contains(tag))
            {
                return true;
            }
            for (String t: tags)
            {
                if (tag.equalsIgnoreCase(t))
                {
                    return true;
                }
            }
            return false;
        }
        public String getNextValue()
        {
            int i;
            for(i = 0; i < options.length; i++)
            {
                if(options[i].equals(string))
                {
                    break;
                }
            }
            i++;
            return options[i % options.length];
        }

        public boolean isStrict()
        {
            return strict;
        }
    }
}

