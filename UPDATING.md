This document describes how to update the upstream projects for MCPC+.

## Updating CraftBukkit

* org.bukkit.craftbukkit
* net.minecraft

Checkout MCPBukkit:

    git clone https://github.com/MinecraftForge/MCPBukkit

"Expand" the patches and update names from srg to csv, using
the expandmcpb from the [MinecraftRemapping](https://github.com/agaricusb/MinecraftRemapping)
repository. Note this script will require editing to point to your local paths as appropriate.

    python expandmcpb.py

Compare and merge the differences, using a tool such as [Araxis Merge](http://www.araxis.com/merge/),
for a directory comparison between `src/java/minecraft` vs `/tmp/out`.

Commit preserving the original author and date, per each upstream commit:

    git commit -a --author="..." --date="..."

### Updating Bukkit

* org.bukkit

The Bukkit API dependency for MCPC+ is maintained as za.co.mcportcentral:mcpc-api at [https://github.com/MinecraftPortCentral/Bukkit/tree/mcpc-api](https://github.com/MinecraftPortCentral/Bukkit/tree/mcpc-api). To update:

    git pull origin master 
    git checkout mcpc-api
    git merge master
    mvn install

Be sure to push to MCPC-API before MCPC-Plus if there are any required API changes.

Merge Spigot-API patches in the same way.

### Updating Spigot

* org.bukkit
* org.spigotmc
* net.minecraft

[Spigot](http://www.spigotmc.org/) source is distributed as patches on top of CraftBukkit and Bukkit,
in the [Spigot](http://github.com/EcoCityCraft/Spigot) repository. [Spigot-Server](https://github.com/EcoCityCraft/Spigot-Server) and
[Spigot-API](https://github.com/EcoCityCraft/Spigot-API) repositories are auto-generated from the patch repository (on top of CraftBukkit
and Bukkit, respectively), and can be used to merge Spigot changes into MCPC+.

Checkout Spigot-Server, compile and install (including Spigot-API if necessary).

Run spigot-remap from [MinecraftRemapping](https://github.com/agaricusb/MinecraftRemapping). This will run [Srg2Source](https://github.com/MinecraftForge/Srg2Source)
to remap the source to MCP mappings, suitable for merging into MCPC+.

Diff and merge the changes, committing preserving the original author and date for each original commit. Prefix the
original commit message with "Spigot patch: " for new patches. Include a link to GitHub for the Spigot patch *and git revision*
in the end of the commit message (see the previous MCPC+ Spigot commits for examples).

If significant changes are required for MCPC+ integration beyond the original Spigot patch, make the changes
in a separate commit under your own authorship.

For updating Spigot patches, repeat the same process except prefix the commit message with "Spigot patch update: ".

## Updating Forge and FML

* net.minecraftforge
* cpw.mods.fml
* net.minecraft

Install and build the latest [MinecraftForge](https://github.com/MinecraftForge/MinecraftForge).

Compare and merge `src/java/minecraft` vs `mcp/src_work/minecraft`. 

Note that files edited by Forge,
but not by MCPC+ or CraftBukkit, do not need to be included since they will be pulled from the 
remapped net.minecraftforge:minecraft-forge dependency.

After merging:

1. In `pom.xml`, update `forge.build` (and `forge.version` if needed)
2. Update `buildVersion` (etc.) in `src/minecraft/net/minecraftforge/common/ForgeVersion.java`
3. Update the resources: download the latest universal build from [files.minecraftforge.net](http://files.minecraftforge.net/) then:

        cd resources
        unzip minecraftforge-universal* fmlversion.properties
        unzip minecraftforge-universal* forgeversion.properties

4.  Create the new remapped Forge dependency: `mvn initialize -P -built`
5. `git commit -am "Update to Forge ..."`

## Updating Minecraft

Update mappings in `resources/mappings` in a subdirectory matching the CraftBukkit
obfuscation version ("v" followed by major version, then an increasing counter starting
at 1 for each major Minecraft version, incrementing by one for each obfuscation change).
See [MinecraftRemapping](http://github.com/agaricusb/MinecraftRemapping) repository
and commit history for how to generate the required mappings (mcp for Forge dependency,
pkgmcp2obf for reobfuscation, cb2numpkg for plugin loader).

Update Bukkit.

Update CraftBukkit. Either start from scratch with MCPBukkit, or merge in the Minecraft updates,
whichever is most feasible.

Update Forge.

Update Spigot.

