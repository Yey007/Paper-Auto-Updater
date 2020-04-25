package io.github.yey007.paperautoupdate;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import com.destroystokyo.paper.util.VersionFetcher.DummyVersionFetcher;

import java.net.*;
import java.io.*;
import java.util.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

public final class Main extends JavaPlugin {

    Updater updater = new Updater();
    FileBoi fileBoi = new FileBoi();

    @Override
    public void onEnable() {
        getLogger().info("PaperAutoUpdate is now running.");
        fileBoi.CreateFiles();
    }

    @Override
    public void onDisable() {
        getLogger().info("PaperAutoUpdate is no longer running.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("update")) {

            if (sender instanceof Player && sender.hasPermission("paperautoupdate.update")) {

                Bukkit.getLogger().info("Update has been called");
                return true;

            } else if (sender instanceof Player && !sender.hasPermission("paperautoupdate.update")) {

                Bukkit.getLogger().info(this.getCommand("update").getPermissionMessage());
                return false;

            } else {

                Bukkit.getLogger().info("Update has been called");
                updater.Update();
                return true;

            }
        }
        return false;
    }
}

class Updater {

    void Update() {

        DummyVersionFetcher fetcher = new DummyVersionFetcher();

        Boolean behind = fetcher.getVersionMessage(Bukkit.getVersion()).contains("behind");
        String behindString;
        String versionMessage = fetcher.getVersionMessage(Bukkit.getVersion());
        Bukkit.getLogger().info(Bukkit.getVersion().toString());

        // find integer (version)
        StringBuffer sb = new StringBuffer(versionMessage);

        // get latest version
        URLReader reader = new URLReader();

        Bukkit.getLogger().info("Checking for new version...");

        try {
            Bukkit.getLogger().info("Newest version is version " + Integer.toString(reader.ReadVersion()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class URLReader {

    public int ReadVersion() throws Exception {

        String content = null;
        URLConnection connection = null;
        int latestInt = 0;

        System.setProperty("http.agent", "Mozilla/5.0");
        try {
            connection = new URL("https://papermc.io/api/v1/paper/1.15.2").openConnection();
            Scanner scanner = new Scanner(connection.getInputStream());
            scanner.useDelimiter("\\Z");
            content = scanner.next();
            scanner.close();
        } catch (Exception ex) {
            Bukkit.getLogger()
                    .warning("Unable to connect to Paper (" + connection + ") in order to check latest version.");
            ex.printStackTrace();
        }

        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(content);

        try {
            Map builds = (Map) json.get("builds");
            String latest = (String) builds.get("latest");
            latestInt = Integer.parseInt(latest);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Something went wrong while parsing a JSON");
            e.printStackTrace();
        }

        return latestInt;
    }
}

class FileBoi {

    File directory = new File("plugins\\PaperAutoUpdate");
    File config = new File(directory, "config.yml");
    File renamer = new File(directory, "renamer.bat");

    public void CreateFiles() {

        if (directory.exists() == false) {

            try {
                directory.mkdirs();
            } catch (Exception e) {
                Bukkit.getLogger().warning("Directory creation failed!");
                e.printStackTrace();
            }

            try {
                renamer.createNewFile();
            } catch (IOException e) {
                Bukkit.getLogger().warning("Renamer creation failed!");
                e.printStackTrace();
            }

            try {
                config.createNewFile();
                WriteConfig();
                Bukkit.getLogger().info(ReadConfig().PathToJar);
                Bukkit.getLogger().info(ReadConfig().PathToStartup);
            } catch (IOException e) {
                Bukkit.getLogger().warning("Config creation failed!");
                e.printStackTrace();
            }

        } else {

            if (config.exists()) {
                Bukkit.getLogger().info("Configuration file found!");
            } else {
                try {
                    config.createNewFile();
                    WriteConfig();
                    Bukkit.getLogger().info(ReadConfig().PathToJar);
                    Bukkit.getLogger().info(ReadConfig().PathToStartup);
                } catch (IOException e) {
                    Bukkit.getLogger().warning("Configuration file creation failed!");
                    e.printStackTrace();
                }
            }

            if (renamer.exists()) {
                Bukkit.getLogger().info("Renamer script found!");
            } else {
                try {
                    renamer.createNewFile();
                } catch (IOException e) {
                    Bukkit.getLogger().warning("Renamer script creation failed!");
                    e.printStackTrace();
                }
            }

        }
    }

    void WriteConfig() {
        try {
            FileWriter fw = new FileWriter(config);
            fw.write("startupScriptPath: C:\\Server\\start.bat \n");
            fw.write("serverJarFilePath: C:\\Server\\paper.jar \n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    ConfigFile ReadConfig() {
        ConfigFile output = new ConfigFile();
        Yaml yaml = new Yaml(new Constructor(ConfigFile.class));
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(config.getPath());
        output = yaml.load(inputStream);
        return output;
    }

}
