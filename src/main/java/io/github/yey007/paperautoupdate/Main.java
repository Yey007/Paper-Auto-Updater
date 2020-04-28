package io.github.yey007.paperautoupdate;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
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
        this.registerCommands();

        new BukkitRunnable() {
            char[] version = Bukkit.getVersion().toCharArray();

            @Override
            public void run() {
                if (!updateNeeded) {
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

    public void registerCommands()
    {
        CommandHandler handler = new CommandHandler();
        handler.register("paperautoupdate", new PaperAutoUpdate());
        handler.register("update", new Updater());
        getCommand("paperautoupdate").setExecutor(handler);
    }

    public static Main getInstance(){
        return (Main) Bukkit.getPluginManager().getPlugin("paperautoupdate");
    }
}

interface CommandInterface {

    // Every time I make a command, I will use this same method.
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args);
}

//Thank you to user AoH_Ruthless on the Bukkit forums for creating a tutorial on making sub-commands!
class CommandHandler implements CommandExecutor {

    private static HashMap<String, CommandInterface> commands = new HashMap<String, CommandInterface>();
 
    //Register method. When we register commands in our onEnable() we will use this.
    public void register(String name, CommandInterface cmd) {
 
        //When we register the command, this is what actually will put the command in the hashmap.
        commands.put(name, cmd);
    }
 
    //This will be used to check if a string exists or not.
    public boolean exists(String name) {
 
        //To actually check if the string exists, we will return the hashmap
        return commands.containsKey(name);
    }
 
    //Getter method for the Executor.
    public CommandInterface getExecutor(String name) {
 
        //Returns a command in the hashmap of the same name.
        return commands.get(name);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(args.length == 0) {

            getExecutor("paperautoupdate").onCommand(sender, command, label, args);
            return true;

        } else if(exists(args[0])) {

            getExecutor(args[0]).onCommand(sender, command, label, args);
            return true;

        } else {

            if(sender instanceof Player) {

                sender.sendMessage("Invalid command.");
                return false;
            }
            else {

                Bukkit.getLogger().info("Invalid command.");
                return false;
            }
        }
    }
}

class PaperAutoUpdate implements CommandInterface {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        if (sender instanceof Player) {
            if (sender.hasPermission("paperautoupdate")) {
                sender.sendMessage("Aliases: " + cmd.getAliases());
                sender.sendMessage("/PaperAutoUpdate: All commands for PaperAutoUpdate");
                sender.sendMessage("/PaperAutoUpdate update: Checks for a new version now and applies it on stop");
                return true;
            } else {
                sender.sendMessage("You do not have the permission for this command (" + cmd.getPermission() + ")");
                return false;
            }
        } else {
            Bukkit.getLogger().info("Aliases: " + cmd.getAliases());
            Bukkit.getLogger().info("/PaperAutoUpdate: All commands for PaperAutoUpdate");
            Bukkit.getLogger().info("/PaperAutoUpdate update: Checks for a new version now and applies it on stop");
            return true;
        }
    }
}

class Updater implements CommandInterface{

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

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        if (sender instanceof Player) {
            if (sender.hasPermission("paperautoupdate.update")) {
                new BukkitRunnable() {
                    char[] version = Bukkit.getVersion().toCharArray();

                    @Override
                    public void run() {
                        update(version);
                    }
                }.runTaskAsynchronously(Main.getInstance());
                return true;
            } else {
                sender.sendMessage("You do not have the permission for this command (paperautoupdate.update)");
                return false;
            }
        } else {
            new BukkitRunnable() {
                char[] version = Bukkit.getVersion().toCharArray();

                @Override
                public void run() {
                    update(version);
                }
            }.runTaskAsynchronously(Main.getInstance());
            return true;
        }
    }
}

class URLReader {

    public int readVersion() {

        String content = null;
        URLConnection connection = null;
        int latestInt = 0;

        System.setProperty("http.agent", "Chrome");
        try {
            connection = new URL("https://papermc.io/api/v1/paper/1.15.2").openConnection();
            connection.addRequestProperty("User-Agent", "Chrome");
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
        String os = System.getProperty("os.name");
        Bukkit.getLogger().info(os);
        if (os.contains("Windows")) {
            try {
                File renamer = new File("plugins\\paperautoupdate\\renamer.bat");
                renamer.createNewFile();
                FileWriter fw = new FileWriter(renamer);
                fw.write("timeout /t 10 \n");
                fw.write("xcopy \"" + config.pathToServer + "\\" + "papernew.jar\" \"" + config.pathToJar + "\"" + "\n");
                fw.write("del " + "\"" + config.pathToServer + "\\" + "papernew.jar\"\n");
                if (config.restartServer == "true") {
                    fw.write("start \"\" \"" + config.pathToStart + "\"");
                } else if (config.restartServer == "jar") {
                    fw.write("start javaw -jar \"" + config.pathToJar + "\"");
                }
                fw.write("exit");
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (os.contains("linux") || os.contains("mpe/ix") || os.contains("freebsd") || os.contains("irix")
                || os.contains("digital unix") || os.contains("unix") || os.contains("mac os")) {

            try {
                File renamer = new File("plugins/paperautoupdate/renamer.sh");
                renamer.createNewFile();
                FileWriter fw = new FileWriter(renamer);
                fw.write("#!/bin/bash");
                fw.write("sleep 10 \n");
                fw.write("mv \"" + config.pathToServer + "/" + "papernew.jar\" \"" + config.pathToJar + "\"" + "\n");
                if (config.restartServer == "true") {
                    fw.write("bash \"\" \"" + config.pathToStart + "\"");
                } else if (config.restartServer == "jar") {
                    fw.write("bash javaw -jar \"" + config.pathToJar + "\"");
                }
                fw.write("exit");
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void runRenamer() {
        String os = System.getProperty("os.name");
        Bukkit.getLogger().info(os);
        if (os.contains("Windows")) {
            try {
                Main.updateNeeded = false;
                Runtime.getRuntime().exec("cmd /c start \"\" plugins\\paperautoupdate\\renamer.bat");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (os.contains("linux") || os.contains("mpe/ix") || os.contains("freebsd") || os.contains("irix")
                || os.contains("digital unix") || os.contains("unix") || os.contains("mac os")) {

            try {
                Main.updateNeeded = false;
                Runtime.getRuntime().exec("/bin/bash -c bash \"\" plugins/paperautoupdate/renamer.sh");
            } catch (IOException e) {
                e.printStackTrace();
            }
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