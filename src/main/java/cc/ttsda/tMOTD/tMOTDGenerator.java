package cc.ttsda.tMOTD;

import cc.ttsda.tMOTD.tMOTD;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

public class tMOTDGenerator {
        public static String[] getMotd(tMOTD tMOTD, Player player){
                tMOTD.reloadConfig();
                String rawMotd = null;
                if (tMOTD.permissions != null){
                        if (tMOTD.getConfig().getBoolean("per-group-motd")){
                                try {
                                        rawMotd = tMOTD.getConfig().getString("groupmotds." + tMOTD.permissions.getPrimaryGroup(player)).replaceAll("(?i)&([a-fk-or0-9])", "\u00A7$1");
                                } catch (NullPointerException e) {
                                        rawMotd = tMOTD.getConfig().getString("motd").replaceAll("(?i)&([a-fk-or0-9])", "\u00A7$1");
                                }
                        } else {
                                rawMotd = tMOTD.getConfig().getString("motd").replaceAll("(?i)&([a-fk-or0-9])", "\u00A7$1");
                        }
                } else {
                        rawMotd = tMOTD.getConfig().getString("motd").replaceAll("(?i)&([a-fk-or0-9])", "\u00A7$1");
                }
                
                Matcher fileMatcher = Pattern.compile("<(.+)>").matcher(rawMotd);
                while (fileMatcher.find()) {
                        // Load all occurences of 'filename'
                        String filename = fileMatcher.group().substring(1, fileMatcher.group().length()-1);
                        String path = tMOTD.getDataFolder().getAbsolutePath() + File.separator + filename;
                        String content = tMOTD.readFile(path);
                        if (content != null){
                                rawMotd = rawMotd.replace(fileMatcher.group(), content);
                        } else {
                                tMOTD.log.severe("An error occurred while loading the file " + filename);
                                rawMotd.replace(fileMatcher.group(), "error");
                        }
                }

                Map<String,String> vars = new HashMap<String,String>();
                vars.put("player", player.getName());
                vars.put("players", Integer.toString(tMOTD.getServer().getOnlinePlayers().length));
                vars.put("maximum", Integer.toString(tMOTD.getServer().getMaxPlayers()));
                vars.put("world", player.getWorld().getName());
                vars.put("relativetime", Long.toString(player.getWorld().getTime()));
                vars.put("x", Integer.toString((int)player.getLocation().getX()));
                vars.put("y", Integer.toString((int)player.getLocation().getY()));
                vars.put("z", Integer.toString((int)player.getLocation().getZ()));

                // HH:MM time
                if (rawMotd.indexOf("%time%") > 0){
                        String sHours, sMinutes;
                        float hoursDec = ((float)player.getWorld().getTime()/1000);
                        hoursDec = (hoursDec + 7) % 24;
                        int hours = (int)hoursDec;
                        int minutes = (int)((hoursDec - hours) * 60);
                        sHours = String.format("%02d", hours);
                        sMinutes = String.format("%02d", minutes);
                        vars.put("time", sHours + ":" + sMinutes);
                }

                //Weather
                if (player.getWorld().hasStorm()){
                        vars.put("weather", tMOTD.getLangConfig().getString("weather.rain"));
                } else {
                        vars.put("weather", tMOTD.getLangConfig().getString("weather.sun"));
                }

                // Time of the day
                // Day:       0 <= time < 12000
                // Dusk:  12000 <= time < 13800
                // Night: 13800 <= time < 22200
                // Dawn:  22200 <= time < 24000
                if (0 <= player.getWorld().getTime() && player.getWorld().getTime() < 12000){
                        vars.put("timeday", tMOTD.getLangConfig().getString("time.day"));

                } else if (12000 <= player.getWorld().getTime() && player.getWorld().getTime() < 13800){
                        vars.put("timeday", tMOTD.getLangConfig().getString("time.dusk"));

                } else if (13800 <= player.getWorld().getTime() && player.getWorld().getTime() < 22200){
                        vars.put("timeday", tMOTD.getLangConfig().getString("time.night"));

                } else if (22200 <= player.getWorld().getTime() && player.getWorld().getTime() <= 24000){
                        vars.put("timeday", tMOTD.getLangConfig().getString("time.dawn"));
                }

                // Biome
                try {
                        vars.put("biome", tMOTD.getLangConfig().getString("biomes." + player.getLocation().getBlock().getBiome().name()));
                } catch (Exception exception) {
                        tMOTD.log.severe("Unknown biome! (Plugin is outdated?)");
                }

                // Balance and Currency
                if (tMOTD.economy != null){
                        double balance = tMOTD.economy.getBalance(player.getName());
                        vars.put("balance", Double.toString(balance));
                        if (balance == 1){
                                vars.put("currencybalance", (Math.round(balance*100.0)/100.0) + " " + tMOTD.economy.currencyNameSingular());
                                vars.put("currency", tMOTD.economy.currencyNameSingular());
                        } else {
                                vars.put("currencybalance", (Math.round(balance*100.0)/100.0) + " " + tMOTD.economy.currencyNamePlural());
                                vars.put("currency", tMOTD.economy.currencyNameSingular());
                        }
                }

                for (Map.Entry<String, String> entry : vars.entrySet()) {
                        if (entry.getValue() != null){
                                rawMotd = rawMotd.replace("%" + entry.getKey() + "%", entry.getValue());
                        }
                }

                return rawMotd.replace("\r", "").split("\n");
        }
}
