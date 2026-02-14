/*
 * This file is part of UPnP.
 *
 * UPnP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 only.
 *
 * UPnP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the GNU Lesser General Public License for more details.
 */

package xyz.necrozma.upnp;

import dev.dejvokep.boostedyaml.route.Route;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.bukkit.plugin.java.JavaPlugin;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
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

    public static GatewayDevice discoverGateway(JavaPlugin plugin, String manualRouterIp, boolean verboseLogging) {
        try {
            GatewayDiscover discover = new GatewayDiscover();
            plugin.getLogger().info("Looking for Gateway Devices");
            Map<InetAddress, GatewayDevice> discoveredGateways = discover.discover();

            if (verboseLogging) {
                plugin.getLogger().info("Discovery returned " + discoveredGateways.size() + " gateway candidate(s).");
                discoveredGateways.forEach((address, device) -> plugin.getLogger().info(
                        "Candidate gateway local address=" + address.getHostAddress() +
                                ", model=" + safeString(device.getModelName()) +
                                ", location=" + safeString(device.getLocation())));
            }

            GatewayDevice gatewayDevice = selectGateway(discover, discoveredGateways, manualRouterIp, plugin);
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

    private static GatewayDevice selectGateway(GatewayDiscover discover, Map<InetAddress, GatewayDevice> discoveredGateways, String manualRouterIp, JavaPlugin plugin) {
        String routerIp = normalizeIp(manualRouterIp);
        if (routerIp == null) {
            return discover.getValidGateway();
        }

        for (Map.Entry<InetAddress, GatewayDevice> entry : discoveredGateways.entrySet()) {
            GatewayDevice candidate = entry.getValue();
            if (routerIp.equals(entry.getKey().getHostAddress()) || routerIp.equals(extractLocationHost(candidate.getLocation()))) {
                plugin.getLogger().info("Using manually configured router IP " + routerIp + " for gateway selection.");
                return candidate;
            }
        }

        plugin.getLogger().warning("Manual router IP override '" + routerIp + "' did not match any discovered gateway. Falling back to automatic selection.");
        return discover.getValidGateway();
    }

    private static String normalizeIp(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String extractLocationHost(String location) {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }
        try {
            URI locationUri = URI.create(location.trim());
            String host = locationUri.getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String safeString(String value) {
        return value == null ? "<unknown>" : value;
    }

    public static void mapPorts(GatewayDevice gatewayDevice, Set<Integer> tcpPorts, Set<Integer> udpPorts, boolean verboseLogging, JavaPlugin plugin) {
        if (verboseLogging) {
            plugin.getLogger().info("Preparing to map " + tcpPorts.size() + " TCP and " + udpPorts.size() + " UDP port(s).");
        }
        tcpPorts.forEach(port -> mapPort(gatewayDevice, port, "TCP", verboseLogging, plugin));
        udpPorts.forEach(port -> mapPort(gatewayDevice, port, "UDP", verboseLogging, plugin));
    }

    private static void mapPort(GatewayDevice gatewayDevice, int port, String protocol, boolean verboseLogging, JavaPlugin plugin) {
        try {
            plugin.getLogger().info("Opening " + protocol + " port " + port);
            PortMappingEntry portMapping = new PortMappingEntry();
            if (gatewayDevice.getSpecificPortMappingEntry(port, protocol, portMapping)) {
                plugin.getLogger().info("Port " + port + " is already mapped. Skipping.");
                if (verboseLogging) {
                    plugin.getLogger().info("Existing mapping details for " + protocol + " port " + port + ": internal client=" +
                            safeString(portMapping.getInternalClient()) + ", internal port=" + portMapping.getInternalPort() +
                            ", enabled=" + safeString(portMapping.getEnabled()));
                }
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

    public static void closeMappedPorts(GatewayDevice gatewayDevice, Set<Integer> ports, String protocol, boolean verboseLogging, JavaPlugin plugin) {
        if (verboseLogging) {
            plugin.getLogger().info("Attempting to close " + ports.size() + " " + protocol + " mapped port(s).");
        }
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
