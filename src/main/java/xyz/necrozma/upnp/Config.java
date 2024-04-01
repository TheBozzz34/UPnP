package xyz.necrozma.upnp;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;


import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static xyz.necrozma.upnp.Main.pluginInstance;


public class Config {
    private static Config instance;
    private final YamlDocument config;

    private Config() throws IOException {
        config = YamlDocument.create(
                new File(Main.getPlugin(Main.class).getDataFolder(), "config.yml"),
                Objects.requireNonNull(Main.getPlugin(Main.class).getResource("config.yml")),
                GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version")).build()
        );
    }

    public static Config getInstance() {
        if (instance == null) {
            try {
                instance = new Config();
            } catch (IOException e) {
                pluginInstance.getLogger().severe("Error while trying to load config: " + e);
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    public void Reload() {
        try {
            instance.config.reload();
        } catch (Exception e) {
            pluginInstance.getLogger().severe("Error reloading config!");
            throw new RuntimeException(e);
        }
    }

    public String getString(Route route) {
        return config.getString(route);
    }

    public Boolean getBoolean(Route route) {
        return config.getBoolean(route);
    }

}
