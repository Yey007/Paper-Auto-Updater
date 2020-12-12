# IMPORTANT #
This project is no longer maintained. Use at your own risk. An auto updater likely isn't a good idea unless you have full trust in bug free builds anyways. 

# Paper-Auto-Updater
A plugin that automatically update paper.jar when there is a new version.

Hello! This is my first plugin. I was tired of manually updating paper for new patches, so I made a plugin to do it for me!
Basically, it generates  .bat or .sh script based on your OS and parameters you define in config.yml. It runs this script on shutdown
in order to replace the old paper.jar with the new one it downloaded while the server was running. And then it's done! Everything is
up to date and running again.


# config.yml
This is the config file. You *must* replace the default values with your own before your first update, or else it will not work.

PathToStartup: The absolute path to your startup script. If you don't have a separate startup script, leave this blank.

PathToJar: The absolute path to your paper.jar or paperclip.jar.

PathToServer: The absolute path to the folder your server is in.

RestartAfterUpdate: true if you want to restart the server after it updates, false if not. If you left PathToStartup blank, set this to jar. If you set this as true or jar, *always* use the stop command and not restart.

SecondsBetweenUpdateChecks: The amount of seconds between update checks. Don't set this too low. Anything above 20 is probably safe.
Edit: I have been told that papermc.io runs cloudflare on their API sites, and for some reason have it set to detecting automated requests? Due to this, frequent access may get your requests blocked. It's best to only check every couple of days to avoid 403 errors.


Thanks for reading! If you find any issues, you can open an issue here.
