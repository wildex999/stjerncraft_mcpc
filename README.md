MCPC-Plus
===========

A Forge/Bukkit/Spigot Minecraft Server

Compilation
-----------

We use maven to handle our dependencies.

* Install [Maven 3](http://maven.apache.org/download.html)
* Install [MCPC API](https://github.com/MinecraftPortCentral/Bukkit/tree/mcpc-api)
* Install [SpecialSource](https://github.com/md-5/SpecialSource)
* Install [SpecialSourceMP](https://github.com/agaricusb/SpecialSourceMP)
* Check out this repo and: `mvn initialize -P -built` then `mvn clean package`


Profiling
---------

We use YourKit as our Java Profiler.

YourKit is kindly supporting open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
* [YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp)
* [YourKit .NET Profiler](http://www.yourkit.com/.net/profiler/index.jsp)


Coding and Pull Request Conventions
-----------

* We generally follow the Sun/Oracle coding standards.
* No tabs; use 4 spaces instead.
* No trailing whitespaces.
* No CRLF line endings, LF only, put your gits 'core.autocrlf' on 'true'.
* No 80 column limit or 'weird' midstatement newlines.
* The number of commits in a pull request should be kept to a minimum (squish them into one most of the time - use common sense!).
* No merges should be included in pull requests unless the pull request's purpose is a merge.
* Pull requests should be tested (does it compile? AND does it work?) before submission.
* Any major additions should have documentation ready and provided if applicable (this is usually the case).
* Most pull requests should be accompanied by a corresponding Leaky ticket so we can associate commits with Leaky issues (this is primarily for changelog generation on dl.bukkit.org).
* Try to follow test driven development where applicable.

If you make changes or add net.minecraft.server classes it is mandatory to:

* Make a separate commit adding the new net.minecraft.server classes (commit message: "Added x for diff visibility" or so).
* Then make further commits with your changes.
* Mark your changes with:
    * 1 line; add a trailing: `// MCPC+ [- Optional reason]`
    * 2+ lines; add
        * Before: `// MCPC+ start [- Optional comment]`
        * After: `// MCPC+ end`
* Keep the diffs to a minimum (*really* important)

Tips to get your pull request accepted
-----------
Making sure you follow the above conventions is important, but just the beginning. Follow these tips to better the chances of your pull request being accepted and pulled.

* Make sure you follow all of our conventions to the letter.
* Make sure your code compiles under Java 5.
* Provide proper JavaDocs where appropriate.
* Provide proper accompanying documentation where appropriate.
* Test your code.
* Make sure to follow coding best practices.
* Provide a test plugin binary and source for us to test your code with.
* Your pull request should link to accompanying pull requests.
* The description of your pull request should provide detailed information on the pull along with justification of the changes where applicable.
