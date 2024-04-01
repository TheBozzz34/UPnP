/*
    This simple plugin allows a Minecraft server to automatically open network ports on supported routers using UPnP.
    Copyright (C) 2024  Necrozma

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


package xyz.necrozma.upnp;

import dev.dejvokep.boostedyaml.route.Route;

import org.bukkit.plugin.java.JavaPlugin;


import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.plugin.Description;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.Website;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import xyz.necrozma.upnp.network.GatewayDevice;
import xyz.necrozma.upnp.network.GatewayDiscover;
import xyz.necrozma.upnp.network.PortMappingEntry;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


import org.bstats.bukkit.Metrics;

@Plugin(name="UPnP", version="1.8.1")
@Description("A Plugin to open UPNP ports for your Minecraft server")
@Author("necrozma")
@Website("necrozma.xyz")
@ApiVersion(ApiVersion.Target.v1_20)
public final class Main extends JavaPlugin {

    public static Main pluginInstance;
    public static GatewayDevice gatewayDevice;
    private Integer[] uniqueValidPortsArray = null;
    private boolean shouldRemovePortsOnStop = false;

    public static boolean isPortValid(int port) {
        return port >= 0 && port <= 65535;
    }


    @Override
    public void onEnable() {

        pluginInstance = this;

        Config configManager = Config.getInstance();

        if (configManager.getBoolean(Route.from("bstats"))) {
            int pluginId = 20515;
            Metrics metrics = new Metrics(this, pluginId);
            getLogger().info("Enabled Metrics");
        } else {
            getLogger().info("Disabling bstats because of config");
        }

        shouldRemovePortsOnStop = configManager.getBoolean(Route.from("close-ports-on-stop"));

        int serverPort = getServer().getPort();

        String portsString = configManager.getString(Route.from("ports"));
        String[] portsArray;
        if (portsString != null) {
            portsArray = portsString.split(",");
        } else {
            portsArray = new String[0];
        }

        Set<Integer> uniqueValidPorts = new HashSet<>();

        uniqueValidPorts.add(serverPort);

        for (String port : portsArray) {
            try {
                int portNumber = Integer.parseInt(port.trim());
                // Check if the port number is within the valid range
                if (isPortValid(portNumber)) {
                    uniqueValidPorts.add(portNumber);
                } else {
                    getLogger().severe("Port number out of range: " + portNumber);
                }
            } catch (NumberFormatException e) {
                getLogger().severe("Invalid port number: " + port);
            }
        }

        uniqueValidPortsArray = uniqueValidPorts.toArray(new Integer[0]);

        getLogger().info("Starting UPnP with the following ports:");

        getLogger().info(Arrays.toString(uniqueValidPortsArray));

        GatewayDiscover discover = new GatewayDiscover();
        getLogger().info("Looking for Gateway Devices");

        try {
            discover.discover();
            gatewayDevice = discover.getValidGateway();

            if (gatewayDevice != null) {
                getLogger().info("Found gateway device: " + gatewayDevice.getModelName() + " " + gatewayDevice.getModelDescription());
                getLogger().info("Using local address: " +  gatewayDevice.getLocalAddress());
                getLogger().info("External address: " + gatewayDevice.getExternalIPAddress());

                for (int port : uniqueValidPortsArray) {
                    getLogger().info("Opening port " + port);
                    PortMappingEntry portMapping = new PortMappingEntry();

                    getLogger().info("Querying device to see if mapping for port " + port + " already exists");
                    if (gatewayDevice.getSpecificPortMappingEntry(port, "TCP", portMapping)) {
                        getLogger().info("Port was already mapped. Aborting.");
                    } else {
                        getLogger().info("Sending port mapping request");
                        if (!gatewayDevice.addPortMapping(port, port, gatewayDevice.getLocalAddress().getHostAddress(), "TCP", "Port forwarded by UPnP plugin")) {
                            getLogger().info("Port mapping attempt failed");
                        } else {
                            getLogger().info("TCP port mapping successful for port " + port);
                        }
                        if (!gatewayDevice.addPortMapping(port, port, gatewayDevice.getLocalAddress().getHostAddress(), "UDP", "Port forwarded by UPnP plugin")) {
                            getLogger().info("Port mapping attempt failed");
                        } else {
                            getLogger().info("UDP port mapping successful for port " + port);
                        }
                    }

                }
            } else {
                getLogger().info("No valid gateway device found.");
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            getLogger().severe("Error while trying to open ports for UPnP: " + e.getLocalizedMessage());
        }
    }
    @Override
    public void onDisable() {
        getLogger().info("UPnP service stopping...");
        if(shouldRemovePortsOnStop) {
            if (gatewayDevice != null) {
                for (int port : uniqueValidPortsArray) {
                    try {
                        gatewayDevice.deletePortMapping(port, "TCP");
                        getLogger().info("Port " + port + " closed for UPnP");
                    } catch (IOException | SAXException e) {
                        getLogger().severe("Error while trying to close port " + port + " for UPnP");
                    }
                }
            }
        }
    }
}
