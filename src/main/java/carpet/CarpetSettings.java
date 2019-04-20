package carpet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import carpet.helpers.SpawnChunks;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//import carpet.carpetclient.CarpetClientRuleChanger;
//import carpet.utils.TickingArea;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;

import static carpet.CarpetSettings.RuleCategory.*;

// /s /c commands tp players back where they started
// discuss xcom accurateBlockPlacement -> flexible or alternate block placement
// world.chunk.storage.Anvil Chunk Loader check again code on diff.
// check why 'test' world doesn't respond to commands while warping
// death counter
//unify command meter with other carpet commmands

public class CarpetSettings {
    public static boolean locked = false;
    public static final String carpetVersion = "v19_04_20";

    public static final Logger LOG = LogManager.getLogger();

    //those don't have to mimic defaults - defaults will be '
    //static store
    public static boolean skipGenerationChecks = false;

    //!rule("superSecretSetting",    "experimental","Gbhs sgnf sadsgras fhskdpri!"),

    // ===== COMMANDS ===== //

    //region COMMANDS
    @Rule(desc = "Enables /spawn command for spawn tracking", category = COMMANDS)
    public static boolean commandSpawn = true;

    @Rule(desc = "Enables /tick command to control game speed", category = COMMANDS)
    public static boolean commandTick = true;

    @Rule(desc = "Enables /log command to monitor events in the game via chat and overlays", category = COMMANDS)
    public static boolean commandLog = true;

    @Rule(desc = "Enables /distance command to measure in game distance between points", category = COMMANDS, extra = {
            "Also enables brown carpet placement action if 'carpets' rule is turned on as well"
    })
    public static boolean commandDistance = true;

    @Rule(desc = "Enables /info command for blocks and entities", category = COMMANDS, extra = {
            "Also enables gray carpet placement action ",
            "and yellow carpet placement action for entities if 'carpets' rule is turned on as well"
    })
    public static boolean commandInfo = true;

    @Rule(desc = "Enables /c and /s commands to quickly switch between camera and survival modes", category = COMMANDS, extra = {
            "/c and /s commands are available to all players regardless of their permission levels"
    })
    public static boolean commandCameramode = true;

    @Rule(desc = "Enables /perimeterinfo command", category = COMMANDS, extra = {
            "... that scans the area around the block for potential spawnable spots"
    })
    public static boolean commandPerimeterInfo = true;

    @Rule(desc = "Enables /draw commands", category = COMMANDS, extra = {
            "... allows for drawing simple shapes"
    })
    public static boolean commandDraw = true;

    @Rule(desc = "Enables /script command", category = COMMANDS, extra = {
            "a powerful in-game scripting API"
    })
    public static boolean commandScript = true;

    @Rule(desc = "Enables /player command to control/spawn players", category = COMMANDS)
    public static boolean commandPlayer = true;

    ////@Rule(desc = "Enables /rng command to manipulate and query rng", category = COMMANDS)
    //public static boolean commandRNG = true;
    //endregion

    // ===== CREATIVE TOOLS ===== //

    // region CREATIVE TOOLS
    //@Rule(desc = "Quasi Connectivity doesn't require block updates.", category = EXPERIMENTAL, extra = {
    //        "All redstone components will send extra updates downwards",
    //        "Affects hoppers, droppers and dispensers"
    //})
    //public static boolean extendedConnectivity = false;

    @Rule(desc = "Portals won't let a creative player go through instantly", category = CREATIVE, extra = {
            "Holding obsidian in either hand won't let you through at all"
    })
    public static boolean portalCreativeDelay = false;

    @Rule(desc = "Players absorb XP instantly, without delay", category = CREATIVE)
    public static boolean xpNoCooldown = false;

    @Rule(desc = "XP orbs combine with other into bigger orbs", category = CREATIVE)
    public static boolean combineXPOrbs = false;

    @Rule(desc = "Explosions won't destroy blocks", category = TNT)
    public static boolean explosionNoBlockDamage = false;

    @Rule(desc = "Removes random TNT momentum when primed", category = TNT)
    public static boolean tntPrimerMomentumRemoved = false;

    //<with modified protocol> @Rule(desc = "Allows to place blocks in different orientations. Requires Carpet Client", category = CREATIVE, extra = {
    //      "Also prevents rotations upon placement of dispensers and furnaces","when placed into a world by commands"
    //})
    //public static boolean accurateBlockPlacement = false;

    //!@Rule(desc = "Repeater pointing from and to wool blocks transfer signals wirelessly", category = CREATIVE, extra = {
    //      "Temporary feature - repeaters need an update when reloaded",
    //      "By Narcoleptic Frog"
    //})
    //public static boolean wirelessRedstone = false;

    //???<no block data anymore> @Rule(desc = "Repeater delays depends on stained hardened clay aka terracotta on which they are placed", category = {EXPERIMENTAL, CREATIVE}, extra = {
    //      "1 to 15 gt per delay added (1-15 block data), 0 (white) adds 100gt per tick"
    //})
    //public static boolean repeaterPoweredTerracota = false;

    @Rule(desc = "TNT doesn't update when placed against a power source", category = CREATIVE)
    public static boolean TNTDoNotUpdate = false;

    @Rule(desc = "Pistons, droppers and dispensers react if block above them is powered", category = CREATIVE)
    public static boolean quasiConnectivity = true;

    @Rule(desc = "Players can flip and rotate blocks when holding cactus", category = {CREATIVE, SURVIVAL}, extra = {
            "Doesn't cause block updates when rotated/flipped",
            "Applies to pistons, observers, droppers, repeaters, stairs, glazed terracotta etc..."
    })
    public static boolean flippinCactus = false;

    @Rule(desc = "hoppers pointing to wool will count items passing through them", category = {COMMANDS, CREATIVE, SURVIVAL}, extra = {
            "Enables /counter command, and actions while placing red and green carpets on wool blocks",
            "Use /counter <color?> reset to reset the counter, and /counter <color?> to query",
            "In survival, place green carpet on same color wool to query, red to reset the counters",
            "Counters are global and shared between players, 16 channels available",
            "Items counted are destroyed, count up to one stack per tick per hopper"
    })
    @CreativeDefault
    @SurvivalDefault
    public static boolean hopperCounters = false;

    //???@Rule(desc = "Enables integration with redstone multimeter mod", category = {CREATIVE, SURVIVAL}, extra = {
    //      "Required clients with RSMM Mod by Narcoleptic Frog. Enables multiplayer experience with RSMM Mod"
    //}),
    //public static boolean redstoneMultimeter = false;

    //! will try @Rule(desc = "Pistons can push tile entities, like hoppers, chests etc.", category = EXPERIMENTAL)
    //public static boolean movableTileEntities = false;

    //!@Rule(desc = "Uses nametags to display current mobs AI tasks", category = CREATIVE)
    //public static boolean displayMobAI = false;

    @Rule(desc = "summoning a lightning bolt has all the side effects of natural lightning", category = CREATIVE)
    public static boolean summonNaturalLightning = false;

    /////@Rule("Reintroduces piston warping/translocation bug", category = EXPERIMENTAL)
    //public static boolean pocketPushing = false;

    /////@Rule("Observers don't pulse when placed", category = CREATIVE)
    //public static boolean observersDoNonUpdate = false;

    //!@Rule("Transparent observers, TNT and redstone blocks. May cause lighting artifacts", category = CREATIVE)
    //public static boolean flyingMachineTransparent = false;

    @Rule(desc = "fill/clone/setblock and structure blocks cause block updates", category = CREATIVE)
    public static boolean fillUpdates = true;

    @Rule(desc = "Customizable piston push limit", category = CREATIVE, options = {"10", "12", "14", "100"}, validator = "validateNonNegative")
    public static int pushLimit = 12;

    @Rule(desc = "Customizable powered rail power range", category = CREATIVE, options = {"9", "15", "30"}, validator = "validatePositive")
    public static int railPowerLimit = 9;

    @Rule(desc = "Customizable fill/clone volume limit", category = CREATIVE, options = {"32768", "250000", "1000000"}, validator = "validateNonNegative")
    public static int fillLimit = 32768;

    /////@Rule("tnt", "Sets the horizontal random angle on TNT for debugging of TNT contraptions", category = TNT, options = { "-1" }, validator = "validateTnTHardcodeTNTAngle", extra = {
    //                              "Set to -1 for default behaviour"
    //})
    //public static float hardcodeTNTangle = -1;
    //private static boolean validateHardcodeTNTAngle(double value) {
    //    return value == -1 || (value >= 0 && value < 360);
    //}

    /////@Rule("tnt", "Sets the tnt random explosion range to a fixed value", category = TNT, options = { "-1" }, validator = "validateTntRandomRange", extra = {
    //                              "Set to -1 for default behaviour"
    //})
    //public static float tntRandomRange = -1;
    //private static boolean validateTntRandomRange(double value) {
    //    return value == -1 || value >= 0;
    //}

    @Rule(desc = "Sets a different motd message on client trying to connect to the server", category = CREATIVE, options = "_", extra = {
            "use '_' to use the startup setting from server.properties"
    })
    public static String customMOTD = "_";

    /////@Rule("experimental", "1.8 double retraction from pistons.", category = CREATIVE, extra = {
    //        "Gives pistons the ability to double retract without side effects."
    //})
    //public static boolean doubleRetraction = false;

    /////@Rule("creative", "Turning nether RNG manipulation on or off.", category = CREATIVE, extra = {
    //        "Turning nether RNG manipulation on or off."
    //})
    //public static boolean netherRNG = false;

    /////@Rule("creative", "Turning end RNG manipulation on or off.", category = CREATIVE, extra = {
    //        "Turning end RNG manipulation on or off."
    //})
    //public static boolean endRNG = false;

    @Rule(desc = "Changes the view distance of the server.", category = CREATIVE, options = {"0", "12", "16", "32", "64"},
            validator = "validateViewDistance", extra = {
            "Set to 0 to not override the value in server settings."
    })
    public static int viewDistance = 0;

    private static boolean validateViewDistance(int value) {
        if (CarpetServer.minecraft_server.isDedicatedServer()) {
            if (value < 2) {
                value = ((DedicatedServer) CarpetServer.minecraft_server).getIntProperty("view-distance", 10);
            }
            if (value > 64) {
                value = 64;
            }
            if (value != CarpetServer.minecraft_server.getPlayerList().getViewDistance())
                CarpetServer.minecraft_server.getPlayerList().setViewDistance(value);
        } else {
            if (value < 2) {
                value = 0;
            }
            if (value > 64) {
                value = 64;
            }
        }

        CarpetSettings.viewDistance = value;
        return true;
    }

    /////@Rule("creative", "Enable use of ticking areas.", category = CREATIVE, extra = {
    //    "As set by the /tickingarea comamnd.",
    //    "Ticking areas work as if they are the spawn chunks."
    //})
    //public static boolean tickingAreas = false;

    @Rule(desc = "Removes the spawn chunks.", category = CREATIVE, validator = "validateDisableSpawnChunks")
    public static boolean disableSpawnChunks = false;

    private static boolean validateDisableSpawnChunks(boolean value) {
        if (!value) {
            WorldServer overworld = CarpetServer.minecraft_server.getWorld(DimensionType.OVERWORLD);
            if (overworld == null)
                return true;

            List<ChunkPos> chunkList = SpawnChunks.listIncludedChunks(overworld);

            // Reused from MinecraftServer initialWorldChunkLoad
            CompletableFuture<?> completablefuture = overworld.getChunkProvider().loadChunks(chunkList, (c) -> {
            });
            while (!completablefuture.isDone()) {
                try {
                    completablefuture.get(1L, TimeUnit.SECONDS);
                } catch (InterruptedException interruptedexception) {
                    throw new RuntimeException(interruptedexception);
                } catch (ExecutionException executionexception) {
                    if (executionexception.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) executionexception.getCause();
                    }

                    throw new RuntimeException(executionexception.getCause());
                } catch (TimeoutException var22) {
                }
            }
        }
        return true;
    }
    //endregion

    // ===== FIXES ===== //
    /*
     * Rules in this category should end with the "Fix" suffix
     */

    //region FIXES
    //!@Rule(desc = "Nether portals correctly place entities going through", category = FIX, extra = {
    //    "Entities shouldn't suffocate in obsidian"
    //})
    //@BugFixDefault
    //public static boolean portalSuffocationFix = false;

    /////@Rule(desc = "Nether portals won't teleport you on occasion to 8x coordinates", category = FIX, extra = {
    //    "It also prevents from taking random fire damage when going through portals "
    //})
    //@BugFixDefault
    //public static boolean portalTeleportationFix = false;

    //???@Rule(desc = "Prevents llamas from taking player food while breeding", category = FIX)
    //@BugFixDefault
    //public static boolean llamaOverfeedingFix = false;

    @Rule(desc = "Guardians honor players' invisibility effect", category = FIX)
    @BugFixDefault
    public static boolean invisibilityFix = false;

    // !? @Rule(desc = "Allows mobs with potion effects to despawn outside of player range", category = FIX, extra = {
    //    "Specifically effective to let witches drinking their own stuffs despawn"
    //})
    //@BugFixDefault
    //public static boolean potionsDespawnFix = false;

    //???@Rule(desc = "Prevents players from mounting animals when holding breeding food", category = FIX)
    //@BugFixDefault
    //public static boolean breedingMountingDisabled = false;

    //!@Rule(desc = "Mobs growing up won't glitch into walls or go through fences", category = FIX)
    //@BugFixDefault
    //public static boolean growingUpWallJump = false;

    //!@Rule(desc = "Won't let mobs glitch into blocks when reloaded.", category = {FIX, EXPERIMENTAL}, extra = {
    //    "Can cause slight differences in mobs behaviour"
    //})
    //@BugFixDefault
    //public static boolean reloadSuffocationFix = false;

    @Rule(desc = "Lag optimizations for redstone dust", category = {EXPERIMENTAL, OPTIMIZATIONS}, extra = {
            "by Theosib"
    })
    public static boolean fastRedstoneDust = false;

    /////@Rule(desc = "TNT causes less lag when exploding in the same spot and in liquids", category = TNT)
    //public static boolean extendedConnectivity = false;

    @Rule(desc = "Fixes server crashing supposedly on falling behind 60s in ONE tick, yeah bs.", category = FIX, extra = {
            "Fixed 1.12 watchdog crash in 1.13 pre-releases, reintroduced with 1.13, GG."
    })
    @BugFixDefault
    public static boolean watchdogCrashFix = false;

    //???@Rule(desc = "Reduces the lag caused by tile entities.", category = EXPERIMENTAL, extra = {
    //    "By PallaPalla"
    //})
    //public static boolean extendedConnectivity = false;

    /////@Rule(desc = "Merges stationary primed TNT entities", category = TNT)
    //public static boolean extendedConnectivity = false;

    @Rule(desc = "Entities pushed or moved into unloaded chunks no longer disappear", category = {EXPERIMENTAL, CREATIVE})
    @BugFixDefault
    public static boolean unloadedEntityFix = false;

    @Rule(desc = "Prevents players from rubberbanding when moving too fast", category = {CREATIVE, SURVIVAL})
    @CreativeDefault
    public static boolean antiCheatSpeed = false;

    //@Rule(desc = "Spawned mobs that would otherwise despawn immediately, won't be placed in world", category = OPTIMIZATIONS) // use 1.13 spawning instead
    //public static boolean optimizedDespawnRange = false;

    //???@Rule(desc = "Optimized movement calculation or very fast moving entities", category = EXPERIMENTAL)
    //public static boolean fastMovingEntityOptimization = false;

    //???@Rule(desc = "Optimized entity-block collision calculations. By masa", category = EXPERIMENTAL)
    //public static boolean blockCollisionsOptimization = false;

    //???@Rule(desc = "Structure bounding boxes (i.e. witch huts) will generate correctly", category = FIX, extra = {
    //    "Fixes spawning issues due to incorrect bounding boxes"
    //})
    //public static boolean boundingBoxFix = false;

    /////@Rule(desc = "Blocks inherit the original light opacity and light values while being pushed with a piston", category = OPTIMIZATIONS)
    //public static boolean movingBlockLightOptimization = false;

    /////@Rule(desc = "Note blocks have update capabilities behaviour from 1.13", category = EXPERIMENTAL)
    //public static boolean noteBlockImitationOf1_13 = false;

    /////@Rule(desc = "Hopper duplication fix by Theosib. Fixed in 1.12.2", category = FIX).defaultTrue(),
    //@BugFixDefault
    //public static boolean hopperDuplicationFix = false;

    //??? check vanilla first @Rule(desc = "Chunk saving issues that causes entites and blocks to duplicate or dissapear", category = FIX, extra = {
    //    "By Theosib"
    //})
    //@BugFixDefault
    //public static boolean entityDuplicationFix = false;

    /////@Rule(desc = "Fixes duplication of items when using item frames", category = FIX)
    //public static boolean itemFrameDuplicationFix = false;

    /////@Rule(desc = "Fixes the recipe book duplication caused by clicking to fast while crafting", category = FIX)
    //public static boolean craftingWindowDuplicationFix = false;

    ////@Rule(desc = "Uses alternative lighting engine by PhiPros. AKA NewLight mod", category = OPTIMIZATIONS)
    //public static boolean newLight = false;

    @Rule(desc = "Permanent fires don't schedule random updates", category = EXPERIMENTAL)
    @BugFixDefault
    public static boolean calmNetherFires = false;

    @Rule(desc = "Customizable maximal entity collision limits, 0 for no limits", category = OPTIMIZATIONS, options = {"0", "1", "20"}, validator = "validateNonNegative")
    public static int maxEntityCollisions = 0;

    //???@Rule(desc = "Fix for piston ghost blocks", category = FIX, extra = {
    //    "true(serverOnly) option works with all clients, including vanilla",
    //    "clientAndServer option requires compatible carpet clients and messes up flying machines"
    //})
    //    .choices("false","false true clientAndServer"),
    //@BugFixDefault
    //public static boolean pistonGhostBlocksFix = false;

    //???@Rule(desc = "fixes water flowing issues", category = OPTIMIZATIONS)
    //public static WaterFlow waterFlow = WaterFlow.vanilla;
    //public static enum WaterFlow {
    //    vanilla, optimized, correct
    //}

    @Rule(desc = "fixes block placement rotation issue when player rotates quickly while placing blocks", category = FIX)
    public static boolean placementRotationFix = false;
    //endregion

    // ===== SURVIVAL FEATURES ===== //

    //region SURVIVAL FEATURES
    @Rule(desc = "Dropping entire stacks works also from on the crafting UI result slot", category = { FIX, SURVIVAL } )
    @SurvivalDefault
    public static boolean ctrlQCraftingFix = false;

    @Rule(desc = "Parrots don't get of your shoulders until you receive damage", category = { SURVIVAL, FEATURE } )
    @SurvivalDefault
    public static boolean persistentParrots = false;

    @Rule(desc = "Empty shulker boxes can stack to 64 when dropped on the ground", category = SURVIVAL, extra = {
            "To move them around between inventories, use shift click to move entire stacks"
    })
    @SurvivalDefault
    public static boolean stackableShulkerBoxes = false;

    //!@Rule(desc = "Named ghasts won't attack players and allow to be ridden and controlled", category = { SURVIVAL, FEATURE }, extra = {
    //        "Hold a ghast tear to bring a tamed ghast close to you",
    //        "Use fire charges when riding to shoot fireballs",
    //        "Requires flying to be enabled on the server"
    //}),
    public static boolean rideableGhasts = false;

    //! rename @Rule(desc = "Guardians turn into Elder Guardian when struck by lightning", category = { EXPERIMENTAL, FEATURE } )
    //public static boolean renewableElderGuardians = false;

    /////@Rule(desc = "Only husks spawn in desert temples", category = { EXPERIMENTAL, FEATURE } )
    //public static boolean huskSpawningInTemples = false;

    /////@Rule(desc = "Shulkers will respawn in end cities", category = { FEATURE, EXPERIMENTAL } )
    //public static boolean shulkerSpawningInEndCities = false;

    //!@Rule(desc = "Saplings turn into dead shrubs in hot climates and no water access when it attempts to grow into a tree", category = FEATURE)
    //public static boolean desertShrubs = false;

    /////@Rule(desc = "Nitwit villagers will have 3 hidden crafting recipes they can craft", category = EXPERIMENTAL, extra = {
    //        "They require food for crafting and prefer a specific food type to craft faster.",
    //        "They have one crafting recipe to start out and unlock there higher recipes as they craft",
    //        "The nitwits will craft faster as they progress",
    //        "When a crafting table is nearby they will throw the product towards it",
    //        "They need a crafting table to craft tier 2 and higher recipes"
    //})
    //public static boolean nitwitCrafter = false;

    @Rule(desc = "Silverfish drop a gravel item when breaking out of a block", category = EXPERIMENTAL)
    public static boolean silverFishDropGravel = false;

    /////@Rule(desc = "Multiple ice crushed by falling anvils make packed ice", category = EXPERIMENTAL)
    //public static boolean renewablePackedIce = false;

    /////@Rule(desc = "Dragon eggs when fed meet items spawn more eggs", category = EXPERIMENTAL)
    //public static boolean renewableDragonEggs = false;

    @Rule(desc = "Placing carpets may issue carpet commands for non-op players", category = SURVIVAL)
    @SurvivalDefault
    public static boolean carpets = false;

    @Rule(desc = "Pistons, Glass and Sponge can be broken faster with their appropriate tools", category = SURVIVAL)
    @SurvivalDefault
    public static boolean missingTools = false;

    @Rule(desc = "Using version appropriate spawning rules: ", category = EXPERIMENTAL, options = {"1.8", "1.12", "1.13"},
            validator = "validateMobSpawningAlgorithm", extra = {
            " - 1.8 : fixed 4 mobs per pack for all mobs, 'subchunk' rule",
            " - 1.12 : fixed 1 to 4 pack size, ignoring entity collisions, subchunk rule",
            " - 1.13 : vanilla (per 1.13.2) mobs don't spawn outside of 128 sphere around players"
    })
    public static String mobSpawningAlgorithm = "1.13";
    public static int n_mobSpawningAlgorithm = 113;
    private static boolean validateMobSpawningAlgorithm(String value)
    {
        CarpetSettings.n_mobSpawningAlgorithm = 113;
        switch (value)
        {
            case "1.8":
                CarpetSettings.n_mobSpawningAlgorithm = 18;
                return true;
            case "1.9":
            case "1.10":
            case "1.11":
            case "1.12":
                CarpetSettings.n_mobSpawningAlgorithm = 112;
                return true;
        }
        return false;
    }

    @Rule(desc = "Alternative, persistent caching strategy for nether portals", category = {SURVIVAL, EXPERIMENTAL})
    @CreativeDefault
    @SurvivalDefault
    public static boolean portalCaching = false;

    @Rule(desc = "The percentage of required sleeping players to skip the night", category = EXPERIMENTAL, options = {"0", "10", "50", "100"},
            validator = "validateSleepingThreshold", extra = {
            "Use values from 0 to 100, 100 for default (all players needed)"
    })
    public static int sleepingThreshold = 100;
    private static boolean validateSleepingThreshold(int value) {
        return value >= 0 && value <= 100;
    }

    //???@Rule(desc = "sponge responds to random ticks", category = {EXPERIMENTAL, FEATURE})
    //public static boolean spongeRandom = false;

    @Rule(desc = "Cactus in dispensers rotates blocks.", category = EXPERIMENTAL, extra = {
            "Cactus in a dispenser gives the dispenser the ability to rotate the blocks ",
            "that are in front of it anti-clockwise if possible."
    })
    public static boolean rotatorBlock = false;

    @Rule(desc = "limits growth limit of newly naturally generated kelp to this amount of blocks", category = FEATURE, options = {"0", "2", "25"})
    public static int kelpGenerationGrowLimit = 25;

    @Rule(desc = "Coral structures will grow with bonemeal from coral plants", category = FEATURE)
    public static boolean renewableCoral = false;
    //endregion

    // ==== API ==== //

    // region API
    /**
     * Any field in this class annotated with this class is interpreted as a carpet rule.
     * The field must be static and have a type of one of:
     * - boolean
     * - int
     * - double
     * - string
     * - a subclass of Enum
     * The default value of the rule will be the initial value of the field.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface Rule {
        /**
         * The rule name, by default the same as the field name
         */
        String name() default ""; // default same as field name

        /**
         * A description of the rule
         */
        String desc();

        /**
         * Extra information about the rule
         */
        String[] extra() default {};

        /**
         * A list of categories the rule is in
         */
        RuleCategory[] category();

        /**
         * Options to select in menu and in carpet client
         * Inferred for booleans and enums.. Otherwise, must be present.
         */
        String[] options() default {};

        /**
         * The name of the validator method called when the rule is changed.
         * The validator method must:
         * - be declared in CarpetSettings
         * - be static
         * - have a return type of boolean
         * - have a single parameter whose type is the same as that of the rule
         * The validator returns true if the value of the rule is accepted, and false otherwise.
         */
        String validator() default ""; // default no method
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface CreativeDefault {
        String value() default "true";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface SurvivalDefault {
        String value() default "true";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface BugFixDefault {
        String value() default "true";
    }

    public static enum RuleCategory {
        TNT, FIX, SURVIVAL, CREATIVE, EXPERIMENTAL, OPTIMIZATIONS, FEATURE, COMMANDS
    }

    private static boolean validatePositive(int value) {
        return value > 0;
    }

    private static boolean validateNonNegative(int value) {
        return value >= 0;
    }
    //endregion

    // ===== IMPLEMENTATION ===== //

    //region IMPLEMENTATION
    private static Map<String, Field> rules = new HashMap<>();
    private static Map<String, String> defaults = new HashMap<>();
    static {
        for (Field field : CarpetSettings.class.getFields()) {
            if (field.isAnnotationPresent(Rule.class)) {
                Rule rule = field.getAnnotation(Rule.class);
                String name = rule.name().isEmpty() ? field.getName() : rule.name();

                if (field.getModifiers() != (Modifier.PUBLIC | Modifier.STATIC))
                    throw new AssertionError("Access modifiers of rule field for \"" + name + "\" should be \"public static\"");

                if (field.getType() != boolean.class && field.getType() != int.class && field.getType() != double.class
                    && field.getType() != String.class && !field.getType().isEnum()) {
                    throw new AssertionError("Rule \"" + name + "\" has invalid type");
                }

                Object def;
                try {
                    def = field.get(null);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
                if (def == null)
                    throw new AssertionError("Rule \"" + name + "\" has null default value");

                if (field.getType() != boolean.class && !field.getType().isEnum()) {
                    boolean containsDefault = false;
                    for (String option : rule.options()) {
                        Object val;
                        if (field.getType() == int.class) {
                            try {
                                val = Integer.parseInt(option);
                            } catch (NumberFormatException e) {
                                throw new AssertionError("Rule \"" + name + "\" has invalid option \"" + option + "\"");
                            }
                        } else if (field.getType() == double.class) {
                            try {
                                val = Double.parseDouble(option);
                            } catch (NumberFormatException e) {
                                throw new AssertionError("Rule \"" + name + "\" has invalid option \"" + option + "\"");
                            }
                        } else {
                            val = option;
                        }
                        if (val.equals(def))
                            containsDefault = true;
                    }
                    if (!containsDefault) {
                        throw new AssertionError("Default value of \"" + def + "\" for rule \"" + name + "\" is not included in its options. This is required for Carpet Client to work.");
                    }
                }

                String validator = rule.validator();
                if (!validator.isEmpty()) {
                    Method method;
                    try {
                        method = CarpetSettings.class.getDeclaredMethod(validator, field.getType());
                    } catch (NoSuchMethodException e) {
                        throw new AssertionError("Validator \"" + validator + "\" for rule \"" + name + "\" doesn't exist");
                    }
                    if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != boolean.class) {
                        throw new AssertionError("Validator \"" + validator + "\" for rule \"" + name + "\" must be a static method returning a boolean");
                    }
                }

                rules.put(name.toLowerCase(Locale.ENGLISH), field);
                defaults.put(name.toLowerCase(Locale.ENGLISH), String.valueOf(def));
            }
        }
    }

    public static boolean hasRule(String ruleName) {
        return rules.containsKey(ruleName.toLowerCase(Locale.ENGLISH));
    }

    public static String get(String ruleName) {
        Field field = rules.get(ruleName.toLowerCase(Locale.ENGLISH));
        if (field == null)
            return "false";
        try {
            return String.valueOf(field.get(null));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    public static String getDescription(String ruleName) {
        Field field = rules.get(ruleName.toLowerCase(Locale.ENGLISH));
        if (field == null)
            return "Error";
        return field.getAnnotation(Rule.class).desc();
    }

    public static RuleCategory[] getCategories(String ruleName) {
        Field field = rules.get(ruleName.toLowerCase(Locale.ENGLISH));
        if (field == null)
            return new RuleCategory[0];
        return field.getAnnotation(Rule.class).category();
    }

    public static String getDefault(String ruleName) {
        String def = defaults.get(ruleName.toLowerCase(Locale.ENGLISH));
        return def == null ? "false" : locked && ruleName.startsWith("command")  ? "false" : def;
    }

    @SuppressWarnings("unchecked")
    public static String[] getOptions(String ruleName) {
        Field field = rules.get(ruleName.toLowerCase(Locale.ENGLISH));
        if (field == null || field.getType() == boolean.class) {
            return new String[]{"false", "true"};
        } else if (field.getType().isEnum()) {
            return Arrays.stream(((Class<? extends Enum<?>>)field.getType()).getEnumConstants())
                    .map(Enum::name).toArray(String[]::new);
        } else {
            return field.getAnnotation(Rule.class).options();
        }
    }

    public static String[] getExtraInfo(String ruleName) {
        Field field = rules.get(ruleName.toLowerCase(Locale.ENGLISH));
        if (field == null)
            return new String[0];
        return field.getAnnotation(Rule.class).extra();
    }

    public static String getActualName(String ruleName) {
        Field field = rules.get(ruleName.toLowerCase(Locale.ENGLISH));
        if (field == null)
            return "null";
        String name = field.getAnnotation(Rule.class).name();
        return name.isEmpty() ? field.getName() : name;
    }

    public static boolean isDouble(String ruleName) {
        Field field = rules.get(ruleName.toLowerCase(Locale.ENGLISH));
        if (field == null)
            return false;
        return field.getType() == double.class;
    }

    @SuppressWarnings("unchecked")
    public static boolean set(String ruleName, String value) {
        Field field = rules.get(ruleName.toLowerCase(Locale.ENGLISH));
        if (field == null)
            return false;

        Class<?> fieldType = field.getType();
        Object newValue;
        if (fieldType == boolean.class) {
            if ("true".equalsIgnoreCase(value))
                newValue = true;
            else if ("false".equalsIgnoreCase(value))
                newValue = false;
            else
                return false;
        } else if (fieldType == int.class) {
            try {
                newValue = new Integer(value);
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (fieldType == Double.class) {
            try {
                newValue = new Double(value);
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (fieldType == String.class) {
            newValue = value;
        } else if (fieldType.isEnum()) {
            newValue = null;
            for (Enum<?> constant : ((Class<? extends Enum<?>>)fieldType).getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(value)) {
                    newValue = constant;
                    break;
                }
            }
            if (newValue == null)
                return false;
        } else {
            throw new AssertionError("Rule \"" + ruleName + "\" has an invalid type");
        }

        String validatorMethod = field.getDeclaredAnnotation(Rule.class).validator();
        if (!validatorMethod.isEmpty()) {
            try {
                Method validator = CarpetSettings.class.getDeclaredMethod(validatorMethod, fieldType);
                if (!((Boolean)validator.invoke(null, newValue)))
                    return false;
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }

        try {
            field.set(null, newValue);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }

        //CarpetClientRuleChanger.updateCarpetClientsRule(ruleName, value);

        return true;
    }

    public static String[] findNonDefault() {
        List<String> rules = new ArrayList<>();
        for (String rule : CarpetSettings.rules.keySet())
            if (!get(rule).equalsIgnoreCase(getDefault(rule)))
                rules.add(getActualName(rule));
        Collections.sort(rules);
        return rules.toArray(new String[0]);
    }

    public static String[] findAll(@Nullable String filter) {
        String actualFilter = filter == null ? null : filter.toLowerCase(Locale.ENGLISH);
        return rules.keySet().stream().filter(rule -> {
            if (actualFilter == null) return true;
            if (rule.contains(actualFilter)) return true;
            for (RuleCategory ctgy : getCategories(rule))
                if (ctgy.name().equalsIgnoreCase(actualFilter))
                    return true;
            return false;
        })
        .map(CarpetSettings::getActualName)
        .sorted()
        .toArray(String[]::new);
    }

    public static void resetToUserDefaults(MinecraftServer server) {
        resetToVanilla();
        applySettingsFromConf(server);
    }

    public static void resetToVanilla() {
        for (String rule : rules.keySet())
            set(rule, getDefault(rule));
    }

    public static void resetToBugFixes() {
        resetToVanilla();
        rules.forEach((name, field) -> {
            if (field.isAnnotationPresent(BugFixDefault.class)) {
                set(name, field.getAnnotation(BugFixDefault.class).value());
            }
        });
    }

    public static void resetToCreative() {
        resetToVanilla();
        rules.forEach((name, field) -> {
            if (field.isAnnotationPresent(CreativeDefault.class)) {
                set(name, field.getAnnotation(CreativeDefault.class).value());
            }
        });
    }

    public static void resetToSurvival() {
        resetToVanilla();
        rules.forEach((name, field) -> {
            if (field.isAnnotationPresent(SurvivalDefault.class)) {
                set(name, field.getAnnotation(SurvivalDefault.class).value());
            }
        });
    }
    //endregion

    // ===== CONFIG ===== //

    // region CONFIG
    public static void applySettingsFromConf(MinecraftServer server)
    {
        Map<String, String> conf = readConf(server);
        boolean is_locked = locked;
        locked = false;
        if (is_locked)
        {
            LOG.info("[CM]: Carpet Mod is locked by the administrator");
        }
        for (String key: conf.keySet())
        {
            if (!set(key, conf.get(key)))
                LOG.error("[CM]: The value of " + conf.get(key) + " for " + key + " is not valid - ignoring...");
            else
                LOG.info("[CM]: loaded setting "+key+" as "+conf.get(key)+" from carpet.conf");

        }
        locked = is_locked;
    }

    private static Map<String, String> readConf(MinecraftServer server)
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
                    locked = true;
                }
                String[] fields = line.split("\\s+",2);
                if (fields.length > 1)
                {
                    if (!hasRule(fields[0]))
                    {
                        LOG.error("[CM]: Setting " + fields[0] + " is not a valid - ignoring...");
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

    private static void writeConf(MinecraftServer server, Map<String, String> values)
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
    public static boolean addOrSetPermarule(MinecraftServer server, String setting_name, String string_value)
    {
        if (locked) return false;
        if (hasRule(setting_name))
        {
            Map<String, String> conf = readConf(server);
            conf.put(setting_name, string_value);
            writeConf(server, conf);
            return set(setting_name,string_value);
        }
        return false;
    }
    // removes overrides of the default values in the file
    public static boolean removeDefaultRule(MinecraftServer server, String setting_name)
    {
        if (locked) return false;
        if (hasRule(setting_name))
        {
            Map<String, String> conf = readConf(server);
            conf.remove(setting_name);
            writeConf(server, conf);
            return set(setting_name,getDefault(setting_name));
        }
        return false;
    }

    public static String[] findStartupOverrides(MinecraftServer server)
    {
        ArrayList<String> res = new ArrayList<String>();
        if (locked) return res.toArray(new String[0]);
        Map <String,String> defaults = readConf(server);
        for (String rule: rules.keySet().stream().sorted().collect(Collectors.toList()))
        {
            if (defaults.containsKey(rule))
            {
                res.add(get(rule));
            }
        }
        return res.toArray(new String[0]);
    }
    //endregion
}

