# CarpetMod for Minecraft 1.13.2
The most comprehensive and convoluted mod for carpets evar. Built based on jarmod-buildsystem-2 by Earthcomputer using Forge Gradle system by Minecraft Forge team. See Earthcomputer's repo for details on the build system.

## Gimme, gimme, I just wanna play
- Then go get the install package (zip folder) for your system from https://github.com/gnembon/carpetmod/releases , and follow the README that is inside of the zip package. Each installer contains patches for both server and singleplayer versions, and (the best case scenario) should require you to just doubleclick (running) on one patching script.

## I installed it. Need help
- Run `/carpet list` to see all the togglable options with description
- Click on a feature in the list in chat, or type `/carpet <featureName>` to get detailed help about each feature.

## tl;dr (to help us develop carpet mod)
- Have Java SDK and git installed (prefferably, or download zipped repo to a folder)
- open command prompt  
- type:
- - `git clone https://github.com/gnembon/carpetmod.git`
- - `cd carpetmod`
- - `gradlew setup`
- - `gradlew genPatches`
- - `gradlew createRelease`
- patches for client and server are in `/build/distributions`
- optionally (to patch the server automatically)
- - have commandline 7za installed
- - `patch_server.cmd`
- - your server should be running in your saves folder already. Connect to `localhost`
- optionally: create installers (depending on the host OS and target OS)
- - `cd installer`
- - `create_installer_<win|mac|ux>.<cmd|sh>`
 

## Requirements
- You need to have at least JDK8 update 92 for recompilation to work, due to a bug in earlier versions of `javac`. You also cannot use JDK9 or JDK10 yet.
- You need to have `git` installed.
- Eclipse Oxygen.3 or later, due to [this Eclipse bug](https://bugs.eclipse.org/bugs/show_bug.cgi?id=526911).
- Or Intellij

OR

- Download the patches from the releases section and apply them directly to game's or server's jars.

## First-time setup
- Copy all the files in this repository into your new project folder.
- Run `gradlew setup` to decompile, deobfuscate the code and apply carpet patches.
- Run `gradlew eclipse` to setup the appropriate Eclipse projects. Do this even if you are planning on using Intellij IDEA.
- If you use Eclipse, open Eclipse, and navigate to `File -> Import -> General -> Existing Projects into Workspace`. Navigate to and select the `projects` subdirectory, and check your mod project, and optionally the clean (unmodified) project too.
- Otherwise, open Intellij IDEA and import the Eclipse project. From 3 available projects choose one in `projects/carpetmod`

## Project layout + management
Once you have setup the project, you should see a file structure which looks something like this:
```
- src/main/java This is where all of the MINECRAFT classes go, i.e. classes which you may or may not have modified, but no classes you have added.
- src/main/resources Similar to src/main/java except for non-java files.
- main-java This is where all of the MOD classes go, i.e. the classes which you have added.
- main-resources Similar to main-java except for non-java files.
```
From outside Eclipse, the file structure looks a little different. However, you should avoid editing these files from outside your IDE of choice:
```
- src/main/java The MOD classes
- src/main/resources The MOD resources
- patches Patches your mod has made to the MINECRAFT classes, which can be pushed to public repositories
- projects/<modname>/src/main/java * The MINECRAFT classes
- projects/<modname>/src/main/resources * The MINECRAFT resources
- projects/clean/src/main/java * The unmodified MINECRAFT classes
- projects/clean/src/main/resources * The unmodified MINECRAFT resources
* = ignored by git
```

- You should be able to run Minecraft directly from within the IDE.
- Every time you checkout a branch which has changed files in the `patches` directory, you need to run `gradlew setup` again to update the code in `src/main/java` inside the IDE. This will not try to decompile again like it did the first time, so won't take long. This will overwrite your local changes.
- Every time you make changes to MINECRAFT classes and want to push to the public repo, you need to run `gradlew genPatches` to update the patch files in the `patches` directory. This takes a few seconds.
- When you are ready to create a release, run `gradlew createRelease`. This may take longer than the other tasks because it is recompiling the code. Once it is done, your releases can be found in the `build/distributions` directory. It includes patches for server jar as well as standalone client jar

## Settings you can change
Accessible in `conf/settings.json`. Beware that changes to this may significanly modify the carpetmod patches so only do it if you know what your are doing or you want to keep your own fork
- `modname` the name of your mod.
- `modversion` the version your mod is on.
- `mcpconfig` the MCPConfig version you are using.
- `mappings` the MCP mappings you are using.
- `mcversion` the Minecraft version.
- `pipeline`, either `joined`, `client` or `server` - whether your mod is to be a client-side-only or server-side-only mod, or to be both and share the same codebase.
- `clientmain` the main class on the client.
- `servermain` the main class on the server.
- `reformat` whether to run Artistic Style on the code to reformat it. Makes the build process a little slower but does mean you can change the formatting options with `conf/astyle.cfg`.
- `customsrg` The custom tsrg file inside the `conf/` folder, to override the one in the MCPConfig distribution, used to deobfuscate even newer Minecraft versions.

## A word of warning
1.13 modding is still in its infancy, and there are already known bugs that occur in the decompiled code which do not occur in vanilla. If you care about maintaining vanilla behaviour, then whenever making a change which may modify a certain vanilla class, make sure to weigh up the benefit of modifying said class against the risk that there might be a decompile bug in the class. This situation is constantly improving as 1.13 modding matures, but for now you can at least minimize the effect by distributing as few modified classes as possible.
