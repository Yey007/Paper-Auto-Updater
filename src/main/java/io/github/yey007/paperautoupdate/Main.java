package io.github.yey007.paperautoupdate;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.net.*;
import java.util.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public final class Main extends JavaPlugin {

    Updater updater = new Updater();
    ConfigFile configuration;

    @Override
    public void onEnable() {
        getLogger().info("PaperAutoUpdate is now running.");
        makeConfig();
        configuration = loadConfig();
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
                updater.update();
                Bukkit.getLogger().info("Startup: " + configuration.pathToStart);
                Bukkit.getLogger().info("Jar: " + configuration.pathToJar);
                return true;

            }
        }
        return false;
    }

    public void makeConfig() {
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    public ConfigFile loadConfig() {
        return new ConfigFile(getConfig().getString("PathToStartup"), getConfig().getString("PathToJar"));
    }

}

class Updater {

    void update() {

        Bukkit.getLogger().info(Bukkit.getVersion());

        // find integer (version)
        char[] version = Bukkit.getVersion().toCharArray();
        StringBuffer sb = new StringBuffer();
        Boolean hadDigit = false;
        int currentVersion;
        int latestVersion = 0;

        for (int i = 0; i < version.length; i++) {
            if (Character.isDigit(version[i])) {
                hadDigit = true;
                sb.append(version[i]);
            } else if (!Character.isDigit(version[i]) && hadDigit) {
                break;
            }
        }
        currentVersion = Integer.parseInt(sb.toString());

        // get latest version
        URLReader reader = new URLReader();

        Bukkit.getLogger().info("Checking for new version...");

        try {
            Bukkit.getLogger().info("Newest version is version " + Integer.toString(reader.readVersion()));
            latestVersion = reader.readVersion();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (latestVersion > currentVersion) {
            reader.downloadFile();
        }
        else {
            Bukkit.getLogger().info("You are on the newest version.");
        }
    }
}

class URLReader {

    public int readVersion() {

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
            Bukkit.getLogger().warning("Unable to connect to Paper (" + connection + ") in order to check latest version.");
            ex.printStackTrace();
        }

        JSONParser parser = new JSONParser();
        JSONObject json = new JSONObject();
        try {
            json = (JSONObject) parser.parse(content);
        } catch (ParseException e1) {
            Bukkit.getLogger().info("Something went wrong while parsing the JSON.");
            e1.printStackTrace();
        }

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
    public void downloadFile() {
        try {
            BufferedInputStream in = new BufferedInputStream(new URL("https://papermc.io/api/v1/paper/1.15.2/latest/download").openStream());
            FileOutputStream fileOutputStream = new FileOutputStream("papernew.jar");
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class ConfigFile
{
    public ConfigFile(String start, String jar)
    {
        pathToStart = start;
        pathToJar = jar;
    }
    public String pathToStart;
    public String pathToJar;
}