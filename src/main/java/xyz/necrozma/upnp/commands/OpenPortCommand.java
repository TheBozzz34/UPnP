package xyz.necrozma.upnp.commands;

import dev.dejvokep.boostedyaml.route.Route;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.PortMappingEntry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;
import xyz.necrozma.upnp.Config;
import xyz.necrozma.upnp.Main;

import java.io.IOException;

@org.bukkit.plugin.java.annotation.command.Command(
        name = "openport",
        desc = "Open a port via UPnP",
        permission = "upnp.openport",
        permissionMessage = "You do not have permission!",
        usage = "/<command> <port> <protocol> (permanent)"
)
public class OpenPortCommand implements CommandExecutor {

    private final Main mainInstance = JavaPlugin.getPlugin(Main.class);
    private final GatewayDevice gatewayDevice = mainInstance.getGatewayDevice();
    private final Config configManager = Config.getInstance(mainInstance);

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            sendMessage(sender, "§cUsage: /openport <port> <protocol> (permanent)");
            return false;
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "§cInvalid port number: " + args[0]);
            return false;
        }

        String protocol = args[1];
        boolean isPermanent = args.length == 3 && args[2].equalsIgnoreCase("permanent");

        try {
            openPort(sender, port, protocol, isPermanent);
        } catch (IOException | SAXException e) {
            sendErrorMessage(sender, "An error occurred while opening the port", e);
        }

        return true;
    }

    private void openPort(CommandSender sender, int port, String protocol, boolean isPermanent) throws IOException, SAXException {
        sendMessage(sender, "Opening port " + port + "...");

        PortMappingEntry portMapping = new PortMappingEntry();
        if (gatewayDevice.getSpecificPortMappingEntry(port, protocol, portMapping)) {
            sendMessage(sender, "§cPort " + port + " is already mapped.");
            return;
        }

        boolean success = gatewayDevice.addPortMapping(port, port, gatewayDevice.getLocalAddress().getHostAddress(), protocol, "Port forwarded by UPnP plugin");
        if (success) {
            sendMessage(sender, "§aPort " + port + " mapped successfully.");
            if (isPermanent) {
                savePortToConfig(port, protocol);
                sendMessage(sender, "§aPort saved to config.");
            }
        } else {
            sendMessage(sender, "§cFailed to map port " + port + ".");
        }
    }

    private void savePortToConfig(int port, String protocol) {
        String ports = configManager.getString(Route.from("ports", protocol));
        ports = ports.isEmpty() ? String.valueOf(port) : ports + "," + port;
        configManager.set(Route.from("ports", protocol), ports);
        configManager.save();
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(message);
    }

    private void sendErrorMessage(CommandSender sender, String message, Exception e) {
        sender.sendMessage(message + ": " + e.getLocalizedMessage());
    }
}
