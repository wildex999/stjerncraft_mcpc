How to remap plugins to support MCPC-Plus
=========================================

REQUIRES: agaricus's srgtool-2.0 located in ./tools
This tool remaps all import NMS calls to import obfs. It also remaps com.google -> guava10.com.google to fix compatiblity issue between forge/bukkit.

* Using the srgtool-2.0.jar located in ./plugins
* Download the required srg @ https://github.com/agaricusb/MinecraftRemapping/blob/master/1.4.7/vcb2obf.srg
* java -jar srgtool-2.0.jar apply --srg vcb2obf.srg --in plugin.jar --inheritance plugin.jar --out plugin-MCPC.jar

Note: this does not handle reflection. We are working on integrating this tool as well as a reflection remapper in upcoming release.
For now, if you want to fix a plugin that uses reflection, request it on forums or grab the plugin source and do it by hand.
