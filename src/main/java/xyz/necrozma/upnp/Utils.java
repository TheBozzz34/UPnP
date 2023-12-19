package xyz.necrozma.upnp;
public class Utils {
    public static boolean isPortValid(int port) {
        return port >= 0 && port <= 65535;
    }
}
