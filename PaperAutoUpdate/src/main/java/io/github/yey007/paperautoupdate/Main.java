package io.github.yey007.paperautoupdate;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import java.net.*;
import java.util.Map;
import java.util.Scanner;

import com.destroystokyo.paper.util.VersionFetcher.DummyVersionFetcher;
import com.google.gson.JsonObject;

import java.io.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public final class Main extends JavaPlugin {

    Updater updater = new Updater();
    Creator create = new Creator();

    @Override
    public void onEnable() {
        getLogger().info("PaperAutoUpdate is now running.");
        create.CreateFiles();
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

class Creator {
    public void CreateFiles() {

        File directory = new File("plugins\\PaperAutoUpdate");
        File config = new File(directory, "config.yml");
        File renamer = new File(directory, "renamer.bat");
        

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
            } catch (IOException e) {
                Bukkit.getLogger().warning("Config creation failed!");
                e.printStackTrace();
            }
            
        } else {

            if (config.exists())
            {
                Bukkit.getLogger().info("Configuration file found!");
            }
            else {
                try {
                    config.createNewFile();
                } catch (IOException e) {
                    Bukkit.getLogger().warning("Config creation failed!");
                    e.printStackTrace();
                }
            }

        }
    }
}
