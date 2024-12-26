package xyz.necrozma.upnp;

import dev.dejvokep.boostedyaml.route.Route;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.bukkit.plugin.java.JavaPlugin;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class UPnPUtils {

    public static Set<Integer> parsePorts(Config configManager, String type, JavaPlugin plugin) {
        Set<Integer> uniqueValidPorts = new HashSet<>();
        String portsString = configManager.getString(Route.from("ports", type));
        if (portsString != null) {
            for (String port : portsString.split(",")) {
                try {
                    int portNumber = Integer.parseInt(port.trim());
                    if (isPortValid(portNumber)) {
                        uniqueValidPorts.add(portNumber);
                    } else {
                        plugin.getLogger().severe("Port number out of range: " + portNumber);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().severe("Invalid port number: " + port);
                }
            }
        }
        return uniqueValidPorts;
    }

    public static boolean isPortValid(int port) {
        return port >= 0 && port <= 65535;
    }

    public static GatewayDevice discoverGateway(JavaPlugin plugin) {
        try {
            GatewayDiscover discover = new GatewayDiscover();
            plugin.getLogger().info("Looking for Gateway Devices");
            discover.discover();
            GatewayDevice gatewayDevice = discover.getValidGateway();
            if (gatewayDevice != null) {
                plugin.getLogger().info("Found gateway device: " + gatewayDevice.getModelName() + " " + gatewayDevice.getModelDescription());
                plugin.getLogger().info("Using local address: " + gatewayDevice.getLocalAddress());
                plugin.getLogger().info("External address: " + gatewayDevice.getExternalIPAddress());
            } else {
                plugin.getLogger().info("No valid gateway device found.");
            }
            return gatewayDevice;
        } catch (IOException | SAXException | ParserConfigurationException e) {
            plugin.getLogger().severe("Error while discovering gateway: " + e.getLocalizedMessage());
            return null;
        }
    }

    public static void mapPorts(GatewayDevice gatewayDevice, Set<Integer> tcpPorts, Set<Integer> udpPorts, JavaPlugin plugin) {
        tcpPorts.forEach(port -> mapPort(gatewayDevice, port, "TCP", plugin));
        udpPorts.forEach(port -> mapPort(gatewayDevice, port, "UDP", plugin));
    }

    private static void mapPort(GatewayDevice gatewayDevice, int port, String protocol, JavaPlugin plugin) {
        try {
            plugin.getLogger().info("Opening " + protocol + " port " + port);
            PortMappingEntry portMapping = new PortMappingEntry();
            if (gatewayDevice.getSpecificPortMappingEntry(port, protocol, portMapping)) {
                plugin.getLogger().info("Port " + port + " is already mapped. Skipping.");
            } else {
                addPortMapping(gatewayDevice, port, protocol, plugin);
            }
        } catch (IOException | SAXException e) {
            plugin.getLogger().severe("Error while mapping " + protocol + " port " + port + ": " + e.getLocalizedMessage());
        }
    }

    private static void addPortMapping(GatewayDevice gatewayDevice, int port, String protocol, JavaPlugin plugin) throws IOException, SAXException {
        if (gatewayDevice.addPortMapping(port, port, gatewayDevice.getLocalAddress().getHostAddress(), protocol, "Port forwarded by UPnP plugin")) {
            plugin.getLogger().info(protocol + " port mapping successful for port " + port);
        } else {
            plugin.getLogger().info(protocol + " port mapping attempt failed for port " + port);
        }
    }

    public static void closeMappedPorts(GatewayDevice gatewayDevice, Set<Integer> ports, String protocol, JavaPlugin plugin) {
        for (int port : ports) {
            try {
                gatewayDevice.deletePortMapping(port, protocol);
                plugin.getLogger().info(protocol + " Port " + port + " closed for UPnP");
            } catch (IOException | SAXException e) {
                plugin.getLogger().severe("Error while closing " + protocol + " port " + port + " for UPnP: " + e.getLocalizedMessage());
            }
        }
    }
}
