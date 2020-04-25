package io.github.yey007.paperautoupdate;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.Bukkit;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.util.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public final class Main extends JavaPlugin {

    Updater updater = new Updater();
    ConfigFile configuration;
    FileBoi fileBoi = new FileBoi(this);
    public static Boolean updateNeeded = false;

    @Override
    public void onEnable() {
        getLogger().info("PaperAutoUpdate is now running.");
        fileBoi.makeConfig();
        configuration = fileBoi.loadConfig();
        fileBoi.makeRenamer();
        
        new BukkitRunnable(){
            char[] version = Bukkit.getVersion().toCharArray();
            @Override
            public void run() {
                if(!updateNeeded)
                {
                    updater.update(version);
                }
            }
        }.runTaskTimerAsynchronously(this, 0, configuration.secondsBetweenUpdates * 20L);
    }

    @Override
    public void onDisable() {
        getLogger().info("PaperAutoUpdate is no longer running.");
        if (updateNeeded) {
            fileBoi.runRenamer();
        } 
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("paperautoupdate")) {

            if (args[0] == "update" && sender instanceof Player && sender.hasPermission("paperautoupdate.update")) {

                new BukkitRunnable(){
                    char[] version = Bukkit.getVersion().toCharArray();
                    @Override
                    public void run() {
                        updater.update(version);
                    }
                }.runTaskAsynchronously(this);
                return true;

            } else if (args[0] == "update" && sender instanceof Player && !sender.hasPermission("paperautoupdate.update")) {

                sender.sendMessage(this.getCommand("update").getPermissionMessage());
                return false;

            } else if (args[0] == "update" && !(sender instanceof Player)) {

                new BukkitRunnable(){
                    char[] version = Bukkit.getVersion().toCharArray();
                    @Override
                    public void run() {
                        updater.update(version);
                    }
                }.runTaskAsynchronously(this);
                
                return true;
            } else {
                if(sender instanceof Player)
                {
                    sender.sendMessage("Incorrect usage. Type /PaperAutoUpdate for help");
                }
                else {
                    Bukkit.getLogger().info("/PaperAutoUpdate update: Updates the server on the next restart.");
                }
            }
        }
        else if(cmd.getName().equalsIgnoreCase("paperautoupdate"))
        {
            Bukkit.getLogger().info("/PaperAutoUpdate update: Updates the server on the next restart.");
        }
        return false;
    }
}

class Updater {

    //the version message of the server must be gotten outside of update for thread safety
    void update(char[] versionRetrieved) {

        // find integer (version)
        StringBuffer sb = new StringBuffer();
        Boolean hadDigit = false;
        int currentVersion;
        int latestVersion = 0;

        for (int i = 0; i < versionRetrieved.length; i++) {
            if (Character.isDigit(versionRetrieved[i])) {
                hadDigit = true;
                sb.append(versionRetrieved[i]);
            } else if (!Character.isDigit(versionRetrieved[i]) && hadDigit) {
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

        Bukkit.getLogger().info("Your version is " + Integer.toString(currentVersion));

        if (latestVersion > currentVersion) {
            Bukkit.getLogger().info("You are " + Integer.toString(latestVersion - currentVersion) + " versions behind. Update will apply on restart.");
            reader.downloadFile();
            Main.updateNeeded = true;
        } else {
            Bukkit.getLogger().info("You are on the newest version.");
            Main.updateNeeded = false;
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
            Bukkit.getLogger().warning("Something went wrong while parsing the JSON.");
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
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class FileBoi {
    Main plugin;

    public FileBoi(Main instance) {
        plugin = instance;
    }

    public void makeRenamer() {
        ConfigFile config = loadConfig();
        try {
            File renamer = new File("plugins\\paperautoupdate\\renamer.bat");
            renamer.createNewFile();
            FileWriter fw = new FileWriter(renamer);
            fw.write("timeout /t 10 \n");
            fw.write("xcopy \"" + config.pathToServer + "\\" + "papernew.jar\" \"" + config.pathToJar + "\"" + "\n");
            fw.write("del " + "\"" + config.pathToServer + "\\" + "papernew.jar\"\n");
            if(config.restartServer == "true")
            {
                fw.write("start \"\" \"" + config.pathToStart + "\"");
            }
            else if(config.restartServer == "jar")
            {
                fw.write("start javaw -jar \"" + config.pathToJar + "\"");
            }
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runRenamer() {
        try {
            Main.updateNeeded = false;
            Runtime.getRuntime().exec("cmd /c start \"\" plugins\\paperautoupdate\\renamer.bat");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void makeConfig() {
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

    public ConfigFile loadConfig() {
        FileConfiguration fc = plugin.getConfig();
        return new ConfigFile(fc.getString("PathToStartup"), fc.getString("PathToJar"), fc.getString("PathToServer"), fc.getString("RestartAfterUpdate"), fc.getInt("SecondsBetweenUpdateChecks"));
    }
}

class ConfigFile
{
    public ConfigFile(String start, String jar, String server, String restart, int update)
    {
        pathToStart = start;
        pathToJar = jar;
        pathToServer = server;
        restartServer = restart;
        secondsBetweenUpdates = update;
    }
    public String pathToStart;
    public String pathToJar;
    public String pathToServer;
    public String restartServer;
    public int secondsBetweenUpdates;
}