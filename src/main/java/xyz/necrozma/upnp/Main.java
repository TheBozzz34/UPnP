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
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.command.Commands;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.plugin.Description;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.Website;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import org.bukkit.plugin.java.annotation.permission.Permission;
import org.bukkit.plugin.java.annotation.permission.Permissions;
import org.bitlet.weupnp.GatewayDevice;
import org.bstats.bukkit.Metrics;

import java.util.Set;

@Plugin(name="UPnP", version="2.1")
@Description("A Plugin to open UPNP ports for your Minecraft server")
@Author("necrozma")
@Website("necrozma.xyz")
@ApiVersion(ApiVersion.Target.v1_20)
@Commands({
        @org.bukkit.plugin.java.annotation.command.Command(
                name = "upnpverbose",
                desc = "Toggle or check UPnP verbose troubleshooting logs",
                aliases = {"upnpvlog"},
                permission = "upnp.troubleshoot",
                permissionMessage = "You do not have permission to use this command.",
                usage = "/upnpverbose <on|off|toggle|status>"
        )
})
@Permissions({
        @Permission(
                name = "upnp.troubleshoot",
                desc = "Allows toggling UPnP troubleshooting verbose logs",
                defaultValue = PermissionDefault.OP
        )
})
public final class Main extends JavaPlugin {

    private GatewayDevice gatewayDevice;
    private Set<Integer> tcpPorts;
    private Set<Integer> udpPorts;
    private boolean shouldRemovePortsOnStop;
    private boolean verboseLogging;

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
        verboseLogging = configManager.getBoolean(Route.from("troubleshooting", "verbose-logging"));
        String manualRouterIp = configManager.getString(Route.from("troubleshooting", "manual-router-ip"));

        // Parse TCP and UDP ports separately
        tcpPorts = UPnPUtils.parsePorts(configManager, "tcp", this);
        udpPorts = UPnPUtils.parsePorts(configManager, "udp", this);

        // Discover gateway and map ports
        gatewayDevice = UPnPUtils.discoverGateway(this, manualRouterIp, verboseLogging);
        if (gatewayDevice != null) {
            UPnPUtils.mapPorts(gatewayDevice, tcpPorts, udpPorts, verboseLogging, this);
        }

    }

    @Override
    public void onDisable() {
        getLogger().info("UPnP service stopping...");
        if (shouldRemovePortsOnStop && gatewayDevice != null) {
            UPnPUtils.closeMappedPorts(gatewayDevice, tcpPorts, "TCP", verboseLogging, this);
            UPnPUtils.closeMappedPorts(gatewayDevice, udpPorts, "UDP", verboseLogging, this);
        }
    }

    public GatewayDevice getGatewayDevice() {
        return gatewayDevice;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"upnpverbose".equalsIgnoreCase(command.getName()) && !"upnpvlog".equalsIgnoreCase(command.getName())) {
            return false;
        }

        if (!sender.hasPermission("upnp.troubleshoot")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0 || "status".equalsIgnoreCase(args[0])) {
            sender.sendMessage(ChatColor.YELLOW + "UPnP verbose logging is currently " + (verboseLogging ? "enabled" : "disabled") + ".");
            return true;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "on":
                setVerboseLogging(true);
                sender.sendMessage(ChatColor.GREEN + "UPnP verbose logging enabled.");
                return true;
            case "off":
                setVerboseLogging(false);
                sender.sendMessage(ChatColor.GREEN + "UPnP verbose logging disabled.");
                return true;
            case "toggle":
                setVerboseLogging(!verboseLogging);
                sender.sendMessage(ChatColor.GREEN + "UPnP verbose logging is now " + (verboseLogging ? "enabled" : "disabled") + ".");
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <on|off|toggle|status>");
                return true;
        }
    }

    private void setVerboseLogging(boolean enabled) {
        verboseLogging = enabled;
        Config configManager = Config.getInstance();
        configManager.set(Route.from("troubleshooting", "verbose-logging"), enabled);
        configManager.save();
    }
}
