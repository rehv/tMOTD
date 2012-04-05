package cc.ttsda.tMOTD;

import cc.ttsda.tMOTD.tMOTDGenerator;
import cc.ttsda.tMOTD.extras.Metrics;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;


public class tMOTD extends JavaPlugin implements Listener {
        Logger log;
        public Economy economy = null;
        public Permission permissions = null;
        private FileConfiguration langConfig = null;
        private File langConfigFile = null;

        public void onDisable(){ 
                log.info(getDescription().getFullName() + " by TTSDA has been disabled"); 
        }

        public void onEnable(){
                log = this.getLogger();
                getServer().getPluginManager().registerEvents(this, this);
                reload();

                if (!setupEconomy() ) {
                        log.warning("Economy functions disabled due to no Vault dependency found!");
                } else {
                        log.info(economy.getName() + " installed, enabling Economy functions");
                }

                if (!setupPermissions() ) {
                        log.warning("Permissions functions disabled due to no Vault dependency found!");
                } else {
                        log.info(permissions.getName() + " installed, enabling Permissions functions");
                }

                try {
                        Metrics metrics = new Metrics(this);
                        metrics.start();
                } catch (IOException e) {
                        // Failed to submit the stats :-(
                }

                log.info(getDescription().getFullName() + " by TTSDA has been enabled");

        }

        private void reload(){
                // Reload configuration files
                reloadLangConfig();

                // Create configuration file if it doesn't exist
                if (!new File(this.getDataFolder().getAbsolutePath() + File.separator + "config.yml").exists()){
                        log.info("No config file exists, creating default");    
                        saveDefaultConfig();
                        saveResource("news.txt", false);
                }
                if (!new File(this.getDataFolder().getAbsolutePath() + File.separator + "lang.yml").exists()){
                        log.info("No language file exists, creating default");
                        saveResource("lang.yml", false);
                }
        }

        public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
                if(cmd.getName().equalsIgnoreCase("motd") || cmd.getName().equalsIgnoreCase("tmotd")){
                        if(args.length == 0){
                                if (sender instanceof Player){
                                        sender.sendMessage(tMOTDGenerator.getMotd(this, (Player)sender));
                                        return true;
                                } else {
                                        sender.sendMessage("This command must be executed by a player");
                                        return true;
                                }
                        } else if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("tmotd.reload")){
                                reload();
                                sender.sendMessage("tMOTD reloaded");
                                return true;
                        } else if ((args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add")) && args.length >= 3 && sender.hasPermission("tMOTD.edit")){
                                boolean append = false;
                                if (args[0].equalsIgnoreCase("set")){
                                        append = false;
                                } else if (args[0].equalsIgnoreCase("add")) {
                                        append = true;
                                }

                                if (new File(this.getDataFolder().getAbsolutePath() + File.separator + args[1]).exists()){
                                        if (!args[1].equalsIgnoreCase("config.yml") && !args[1].equalsIgnoreCase("lang.yml")){
                                                String text = "";
                                                for (int i=2; i<args.length; i++) {
                                                        text = text.concat(args[i] + " ");
                                                }

                                                try {
                                                        FileWriter fw = new FileWriter(args[1], append);
                                                        fw.write(text);
                                                        fw.close();
                                                        sender.sendMessage("File successfuly written");
                                                        reload();
                                                        return true;
                                                } catch (IOException e) {
                                                        sender.sendMessage("An error occurred");
                                                        return false;
                                                }
                                        } else {
                                                sender.sendMessage("That file can't be edited");
                                                return true;
                                        }
                                } else {
                                        sender.sendMessage("That file doesn't exist");
                                        return true;
                                }
                        } else if (args.length == 1){
                                if (sender.hasPermission("tmotd.others")){
                                        Player asPlayer = getServer().getPlayer(args[0]);
                                        if (asPlayer != null){
                                                sender.sendMessage(tMOTDGenerator.getMotd(this, asPlayer));
                                        } else {
                                                sender.sendMessage("There isn't such player");
                                        }
                                } else {
                                        sender.sendMessage("You don't have permission to view other players' MOTDs");
                                }
                                return true;
                        }
                }
                return false;
        }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
                event.getPlayer().sendMessage(tMOTDGenerator.getMotd(this, event.getPlayer()));
        }

        // Read a file from the disk
        public String readFile(String filename){
                try {
                        DataInputStream dis = new DataInputStream (new FileInputStream(filename));
                        byte[] datainBytes = new byte[dis.available()];
                        dis.readFully(datainBytes);
                        dis.close();
                        String content = new String(datainBytes, 0, datainBytes.length);
                        return content;

                } catch(Exception exception) {
                        return null;
                }
        }

        public void reloadLangConfig() {
                if (langConfigFile == null) {
                        langConfigFile = new File(getDataFolder(), "lang.yml");
                }
                langConfig = YamlConfiguration.loadConfiguration(langConfigFile);

                // Look for defaults in the jar
                InputStream defConfigStream = getResource("lang.yml");
                if (defConfigStream != null) {
                        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
                        langConfig.setDefaults(defConfig);
                }
        }

        public FileConfiguration getLangConfig() {
                if (langConfig == null) {
                        reloadLangConfig();
                }
                return langConfig;
        }

        private boolean setupEconomy() {
                if (getServer().getPluginManager().getPlugin("Vault") == null) {
                        return false;
                }
                RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
                if (rsp == null) {
                        return false;
                }
                economy = rsp.getProvider();
                return economy != null;
        }

        private boolean setupPermissions() {
                if (getServer().getPluginManager().getPlugin("Vault") == null) {
                        return false;
                }
                RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
                if (rsp == null) {
                        return false;
                }
                permissions = rsp.getProvider();
                return permissions != null;
        }
}
