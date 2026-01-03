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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.plugin.Description;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.Website;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import org.bitlet.weupnp.GatewayDevice;
import org.bstats.bukkit.Metrics;

import java.util.Objects;
import java.util.Set;

@Plugin(name="UPnP", version="2.0")
@Description("A Plugin to open UPNP ports for your Minecraft server")
@Author("necrozma")
@Website("necrozma.xyz")
@ApiVersion(ApiVersion.Target.v1_20)
public final class Main extends JavaPlugin {

    private GatewayDevice gatewayDevice;
    private Set<Integer> tcpPorts;
    private Set<Integer> udpPorts;
    private boolean shouldRemovePortsOnStop;

    @Override
    public void onEnable() {
        Config configManager = Config.getInstance(this);

        // Enable metrics if configured
        if (configManager.getBoolean(Route.from("bstats"))) {
            int pluginId = 20515;
            new Metrics(this, pluginId);
            getLogger().info("Enabled Metrics");
        } else {
            getLogger().info("Disabling bstats because of config");
        }

        shouldRemovePortsOnStop = configManager.getBoolean(Route.from("close-ports-on-stop"));

        // Parse TCP and UDP ports separately
        tcpPorts = UPnPUtils.parsePorts(configManager, "tcp", this);
        udpPorts = UPnPUtils.parsePorts(configManager, "udp", this);

        // Discover gateway and map ports
        gatewayDevice = UPnPUtils.discoverGateway(this);
        if (gatewayDevice != null) {
            UPnPUtils.mapPorts(gatewayDevice, tcpPorts, udpPorts, this);
        }

    }

    @Override
    public void onDisable() {
        getLogger().info("UPnP service stopping...");
        if (shouldRemovePortsOnStop && gatewayDevice != null) {
            UPnPUtils.closeMappedPorts(gatewayDevice, tcpPorts, "TCP", this);
            UPnPUtils.closeMappedPorts(gatewayDevice, udpPorts, "UDP", this);
        }
    }

    public GatewayDevice getGatewayDevice() {
        return gatewayDevice;
    }
}
