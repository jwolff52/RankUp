package me.kingdomscraft.rankup;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Rankup extends JavaPlugin{
	public static Logger logger=Logger.getLogger("Minecraft");
	
	public static Rankup plugin;
	
	public static FileConfiguration config;
	public static File cfile;
	
	private ArrayList<String> groups;
	private ArrayList<String> prices;
	
	private static String identifier;
	
	private static String version;
	
	private Permissions perms;
	
	@Override
	public void onEnable(){
		version = "1.0";
		
		groups=new ArrayList<String>();
		prices=new ArrayList<String>();
		
		config=getConfig();
		config.options().copyDefaults(true);
		saveConfig();
		identifier=config.getString("identifier");
		int configNumber=1;
		while (true) {
			if (config.getString("r" + configNumber) != null) {
				groups.add(config.getString("r" + configNumber));
			} else {
				break;
			}
			configNumber++;
		}
		String temp="";
		String subtemp="";
		for(int x=0;x<groups.size();x++){
			temp=groups.get(x);
			for(int y=0;y<temp.length();y++){
				if(temp.charAt(y)==identifier.charAt(0)){
					groups.set(x, temp.substring(0, y));
					subtemp=temp.substring(y);
					prices.add(subtemp.substring(1));
					break;
				}
			}
		}
		cfile=new File(getDataFolder(), "config.yml");
		
		PluginManager pm=getServer().getPluginManager();
		perms=new Permissions();
		pm.addPermission(perms.canPreformAll);
		pm.addPermission(perms.canPreformOnOthers);
		pm.addPermission(perms.canPreformOnSelf);
		
		logger.info("Rankup Version: "+version+" has been enabled!");
	}
	@Override
	public void onDisable(){
		PluginManager pm=getServer().getPluginManager();
		perms=new Permissions();
		pm.removePermission(perms.canPreformAll);
		pm.removePermission(perms.canPreformOnOthers);
		pm.removePermission(perms.canPreformOnSelf);
		
		logger.info("Rankup has been disabled!");
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if(sender instanceof Player){
			Player player=(Player)sender;
			if (label.equalsIgnoreCase("rankup")) {
				if(args.length==1&&(player.hasPermission(perms.canPreformAll)||player.hasPermission(perms.canPreformOnSelf))){
					rankUp(player,args[0]);
					return true;
				}else if(args.length==2&&(player.hasPermission(perms.canPreformAll)||player.hasPermission(perms.canPreformOnOthers))){
					rankUp(Bukkit.getPlayer(args[1]), args[0]);
					return true;
				}else{
					player.sendMessage(ChatColor.RED+"You do not have permission to use that command!");
					return false;
				}
			}
		}else{
			if(label.equalsIgnoreCase("rankup")){
				if(args.length==2){
					rankUp(Bukkit.getPlayer(args[1]), args[0]);
				}else if(args.length<2){
					sender.sendMessage(ChatColor.RED+"Too few arguments!");
				}
			}
		}
		return false;
	}
	private void rankUp(Player player, String group){
		ConsoleCommandSender ccs=Bukkit.getConsoleSender();
		int intNextRank=0;
		for(int x=0;x<groups.size();x++){
			if(groups.get(x).equalsIgnoreCase(group)){
				intNextRank=x+1;
				break;
			}
		}
		if(intNextRank<groups.size()){
			if(getServer().dispatchCommand(ccs, "eco take "+player.getName()+" "+prices.get(intNextRank))){
				if(getServer().dispatchCommand(ccs, "manuadd "+player.getName()+" "+groups.get(intNextRank))){
					Bukkit.broadcastMessage(ChatColor.GOLD+"["+ChatColor.GRAY+"RankUp"+ChatColor.GOLD+"]"+player.getDisplayName()+ChatColor.AQUA+" was promoted from the group "+ChatColor.RED+groups.get(intNextRank-1)+ChatColor.AQUA+" to group "+ChatColor.RED+groups.get(intNextRank)+"!");
				}else{
					player.sendMessage(ChatColor.RED+"An internal error occured");
				}
			}else{
				player.sendMessage(ChatColor.DARK_RED+"You do not have enough money to rank up!");
			}
			
		}
	}
}