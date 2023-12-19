package xyz.necrozma.upnp;
public class Utils {
    public static boolean isPortValid(int port) {
        return port >= 0 && port <= 65535;
    }
    public static boolean isDynMapInstalled() {
        return Main.getPlugin(Main.class).getServer().getPluginManager().getPlugin("Dynmap")!=null;
    }
}
