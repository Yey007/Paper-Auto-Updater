package io.github.yey007.paperautoupdate;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

 public final class Updater extends JavaPlugin
{
    @Override
    public void onEnable() {
     
        getLogger().info("Paper auto updater is now running.");

    }

    @Override
    public void onDisable() {
        getLogger().info("Paper auto updater is no longer running.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
  
        if(cmd.getName().equalsIgnoreCase("update")){
 
            if(sender instanceof Player && sender.hasPermission("paperautoupdate.update")) {

                getLogger().info("Update has been called");
                return true;

            } else if (sender instanceof Player && !sender.hasPermission("paperautoupdate.update")){

                getLogger().info(this.getCommand("update").getPermissionMessage());
                return false;

            } else {

                getLogger().info("Update has been called");
                return true;

            }
        }
		return false;
    } 
}
