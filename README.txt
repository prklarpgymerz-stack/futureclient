FUTURE CLIENT (Forge 1.8.9)
Build (on PC, JDK 8): put this folder in a Forge 1.8.9 MDK setup (or use it directly with ForgeGradle 2.1),
then run:  gradlew build   -> jar in build/libs/ -> drop into .minecraft/mods with Forge 1.8.9.
In game: RIGHT SHIFT opens the ClickGUI (rebind in Controls).

NAMETAG LOGO
- You always get the logo (third person) on your own nametag.
- To show it on OTHER Future Client users: run server/server.py somewhere reachable,
  then set FutureNet.API in src/main/java/com/futureclient/net/FutureNet.java and rebuild.
