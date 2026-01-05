# UPnP

[![Java CI with Maven](https://github.com/TheBozzz34/UPnP/actions/workflows/maven.yml/badge.svg)](https://github.com/TheBozzz34/UPnP/actions/workflows/maven.yml)

## Version 2.0

This plugin allows for automatic opening of user-selectable ports on supported routers. This is accomplished via the [UPnP Protocol](https://en.wikipedia.org/wiki/Universal_Plug_and_Play). The plugin will automatically open the server port, and additional ports can be added in the config for things like dynmap. You can also choose whether the plugin automatically closes the ports when the server closes.

![stats](https://bstats.org/signatures/bukkit/UPnP.svg)

UPnP is intended to be as lightweight and simple as possible, and only has four dependencies, used for logging, access to the spigot api, and bstats. Overall, UPnP is written in 896 lines of code.

## License

This mod is licensed under the **GNU Lesser General Public License v3.0 only (LGPL-3.0-only)**.

### What this means for users and developers

- You may use this mod in modpacks (including commercial modpacks).
- You may run this mod on servers, including paid or hosted servers.
- Other mods may depend on or integrate with this mod under any license.
- Closed-source mods and launchers may link against this mod.

### What you must do

- If you modify *this mod’s source code*, you must release those modifications
  under the same LGPL-3.0-only license.
- You must allow users to replace or update this mod independently (e.g., by
  distributing it as a normal JAR).

### What you do *not* have to do

- You do NOT have to open-source your entire mod, server, or modpack.
- You do NOT have to publish server source code just because players connect.
- You do NOT have to license your project under LGPL if you merely depend on this mod.

### License history

Versions released prior to **January 3rd 2026** were licensed under **AGPL-3.0-only**. You can read more about the change [here](https://github.com/TheBozzz34/UPnP/blob/main/NOTICE.md).

Versions released on or after **January 3rd 2026** are licensed under **LGPL-3.0-only**.

For the full license text, see the [LICENSE](LICENSE) file.
