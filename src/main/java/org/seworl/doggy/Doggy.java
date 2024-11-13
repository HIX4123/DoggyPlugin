package org.seworl.doggy;

import org.bukkit.plugin.java.JavaPlugin;
import org.seworl.doggy.Events.ControlDog;

public final class Doggy extends JavaPlugin {


    @Override public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new ControlDog(), this);

        getLogger().info("Doggy has been enabled");
    }

    @Override public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Doggy has been disabled");
    }


}