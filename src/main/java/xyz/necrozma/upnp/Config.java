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

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.bukkit.Bukkit.getLogger;

public class Config {

    private static Config instance;
    private YamlDocument config;

    private Config(JavaPlugin plugin) {
        try {
            // Load the config file, or create it if it doesn't exist
             config = YamlDocument.create(new File(plugin.getDataFolder(), "config.yml"),
                     Objects.requireNonNull(plugin.getResource("config.yml")), GeneralSettings.DEFAULT, LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version")).build());

        } catch (IOException e) {
            plugin.getLogger().severe("Could not load config.yml: " + e.getMessage());
        }
    }

    // Singleton instance retrieval
    public static Config getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new Config(plugin);
        }
        return instance;
    }

    public static Config getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Config has not been initialized. Call getInstance(JavaPlugin) first.");
        }
        return instance;
    }

    // Method to get a boolean value from the config
    public boolean getBoolean(Route route) {
        return config.getBoolean(route);
    }

    // Method to get a string value from the config
    public String getString(Route route) {
        return config.getString(route);
    }

    // Method to get a specific integer value from the config
    public int getInt(Route route) {
        return config.getInt(route);
    }

    // set a value in the config
    public void set(Route route, Object value) {
        config.set(route, value);
    }

    // save the config
    public void save() {
        try {
            config.save();
        } catch (IOException e) {
            getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }
}
