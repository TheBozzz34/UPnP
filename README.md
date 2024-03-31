# UPnP
[![build pipeline](https://app.buddy.works/ethan-james-1/upnp/pipelines/pipeline/488670/badge.svg?token=507c126a7263056d9e6167d4037e69fde6cfac7d50cd72a1076c8e9b512c5e97 "buddy pipeline")](https://app.buddy.works/ethan-james-1/upnp/pipelines/pipeline/488670)

This plugin allows for automatic opening of user-selectable ports on supported routers. This is accomplished via the [UPnP Protocol](https://en.wikipedia.org/wiki/Universal_Plug_and_Play). The plugin will automatically open the server port, and additional ports can be added in the config for things like dynmap. You can also choose whether the plugin automatically closes the ports when the server closes.

![stats](https://bstats.org/signatures/bukkit/UPnP.svg)

UPnP is intended to be as lightweight and simple as possible, and only has four dependencies, used for logging, access to the spigot api, and bstats. Overall, UPnP is written in 896 lines of code.
