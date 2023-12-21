/*
    This simple plugin allows a Minecraft server to automatically open network ports on supported routers using UPnP.
    Copyright (C) 2023  Necrozma

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

import org.jetbrains.annotations.NotNull;
import xyz.necrozma.upnp.network.GatewayDevice;
import xyz.necrozma.upnp.network.GatewayDiscover;
import xyz.necrozma.upnp.network.PortMappingEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.net.HttpURLConnection;
import java.io.InputStreamReader;

import org.bstats.bukkit.Metrics;

public final class Main extends JavaPlugin {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static GatewayDevice gatewayDevice;
    private Integer[] uniqueValidPortsArray = null;
    private boolean shouldRemovePortsOnStop = false;

    public static boolean isPortValid(int port) {
        return port >= 0 && port <= 65535;
    }


    @Override
    public void onEnable() {

        Config configManager = Config.getInstance();

        if (configManager.getBoolean(Route.from("bstats"))) {
            int pluginId = 20515;
            Metrics metrics = new Metrics(this, pluginId);
            logger.info("Enabled Bstats");
        } else {
            logger.info("Disabling bstats because of config");
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
                    logger.error("Port number out of range: {}", portNumber);
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid port number: {}", port);
            }
        }

        uniqueValidPortsArray = uniqueValidPorts.toArray(new Integer[0]);

        logger.info("Starting UPnP with the following ports:");

        logger.info(Arrays.toString(uniqueValidPortsArray));

        GatewayDiscover discover = new GatewayDiscover();
        logger.info("Looking for Gateway Devices");

        try {
            discover.discover();
            gatewayDevice = discover.getValidGateway();

            if (gatewayDevice != null) {
                logger.info("Found gateway device: {} ({})", gatewayDevice.getModelName(), gatewayDevice.getModelDescription());
                logger.info("Using local address: {}", gatewayDevice.getLocalAddress());
                logger.info("External address: {}", gatewayDevice.getExternalIPAddress());

                for (int port : uniqueValidPortsArray) {
                    logger.info("Opening port " + port);
                    PortMappingEntry portMapping = new PortMappingEntry();

                    logger.info("Querying device to see if mapping for port " + port + " already exists");
                    if (gatewayDevice.getSpecificPortMappingEntry(port, "TCP", portMapping)) {
                        logger.info("Port was already mapped. Aborting.");
                    } else {
                        logger.info("Sending port mapping request");
                        if (!gatewayDevice.addPortMapping(port, port, gatewayDevice.getLocalAddress().getHostAddress(), "TCP", "Port forwarded by UPnP plugin")) {
                            logger.info("Port mapping attempt failed");
                        } else {

                            if(port == getServer().getPort()) {
                                if (!gatewayDevice.addPortMapping(port, port, gatewayDevice.getLocalAddress().getHostAddress(), "UDP", "Port forwarded by UPnP plugin")) {
                                    logger.info("Port mapping attempt failed");
                                } else {
                                    logger.info("UDP Port Mapping successful");

                                }
                            }
                            logger.info("TCP Port Mapping successful");
                            logger.info("Checking if server is online");
                        }
                    }
                }

            } else {
                logger.info("No valid gateway device found.");
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            logger.error("Error while trying to open ports for UPnP", e);
        }

        String serverOnline = isServerOnline();
        logger.info("Server status: " + serverOnline);
    }

    private static String isServerOnline() {
        try {

            String AppUrl = "https://proxy.necrozma.xyz/proxy";

            String ipAddress = gatewayDevice.getExternalIPAddress();
            String port = String.valueOf(Main.getPlugin(Main.class).getServer().getPort());

            String data = "{\"ip\": \"" + ipAddress + "\", \"port\": \"" + port + "\"}";

            HttpURLConnection connection = getHttpURLConnection(AppUrl);
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(data);
            outputStream.flush();
            outputStream.close();

            int responseCode = connection.getResponseCode();

            // Read response
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                logger.info(response.toString());
                return response.toString();
            } else {
                return("Request failed");
            }
        } catch (Exception e) {
            logger.error("Error while trying to check if server is online", e);
        }
        return null;
    }

    @NotNull
    private static HttpURLConnection getHttpURLConnection(String AppUrl) throws IOException {
        URL url = new URL(AppUrl);

        // Create connection object
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set request method
        connection.setRequestMethod("POST");

        // Set request headers
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        // Enable output and set data
        connection.setDoOutput(true);
        return connection;
    }

    @Override
    public void onDisable() {
        logger.info("UPnP service stopping...");
        if(shouldRemovePortsOnStop) {
            if (gatewayDevice != null) {
                for (int port : uniqueValidPortsArray) {
                    try {
                        gatewayDevice.deletePortMapping(port, "TCP");
                        logger.info("Port " + port + " closed for UPnP");
                    } catch (IOException | SAXException e) {
                        logger.error("Error while trying to close port " + port + " for UPnP", e);
                    }
                }
            }
        }
    }
}
