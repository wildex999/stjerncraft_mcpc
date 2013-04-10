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

### Updating Spigot

* org.bukkit
* org.spigotmc
* net.minecraft

Checkout [Spigot](https://github.com/EcoCityCraft/Spigot), patch, run [Srg2Source](https://github.com/MinecraftForge/Srg2Source), diff and merge.

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

