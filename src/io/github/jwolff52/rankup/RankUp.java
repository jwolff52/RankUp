package io.github.jwolff52.rankup;

import io.github.jwolff52.rankup.util.RankUpListener;
import io.github.jwolff52.rankup.util.SettingsManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class RankUp extends JavaPlugin {
	public static Logger logger = Logger.getLogger("Minecraft");

	public static RankUp plugin;

	private static PluginDescriptionFile pdf;
	
	private static ArrayList<File> playerFiles;
	
	private static RankUpListener rl;

	private static SettingsManager sm;

	private ArrayList<String> groups;
	private ArrayList<String> prices;

	private static String identifier;

	@Override
	public void onEnable() {
		pdf = this.getDescription();
		PluginManager pm=getServer().getPluginManager();
		File playerDir=new File(getDataFolder(), "players");
		if (!playerDir.exists()) {
			logger.info("RankUp - INFO - \"players\" directory not found! Preforming first time setup!");
			playerDir.mkdir();
		}
		sm = SettingsManager.getInstance();
		sm.setup(this);
		getConfigOptions();
		rl=new RankUpListener(this);
		pm.registerEvents(rl, this);
		initPlayerFiles();
		logger.info("Rankup Version: " + pdf.getVersion()
				+ " has been enabled!");
	}

	@Override
	public void onDisable() {
		logger.info("Rankup has been disabled!");
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args.length < 1) {
			sender.sendMessage(ChatColor.DARK_RED + "Not enough arguments!");
			return false;
		} if(args[0].equalsIgnoreCase("reload")) {
			if (!(sender.hasPermission("rankup.*")||sender.hasPermission("rankup.reload"))) {
				sender.sendMessage(ChatColor.GOLD+"["+ChatColor.GRAY+"RankUp"+ChatColor.GOLD+"]"+ChatColor.RED+"You do not have permission to use that command!");
				return false;
			} 
			sm.reloadConfig();
			getConfigOptions();
			sender.sendMessage(ChatColor.GOLD+"["+ChatColor.GRAY+"RankUp"+ChatColor.GOLD+"]"+"RankUp config successfully reloaded!");
		} else if(args[0].equalsIgnoreCase("reset")) {
			if(args.length < 2) {
				sender.sendMessage(ChatColor.DARK_RED + "Not enough arguments!");
				return false;
			} if (!(sender.hasPermission("rankup.*") || sender.hasPermission("rankup.reset"))) {
				sender.sendMessage(ChatColor.GOLD+"["+ChatColor.GRAY+"RankUp"+ChatColor.GOLD+"]"+ChatColor.RED+"You do not have permission to use that command!");
				return false;
			} 
			return reset(Bukkit.getPlayer(args[1]), sender.getName());
		} else {
			if(args.length == 1) {
				if (! (sender.hasPermission("rankup.*") || sender.hasPermission("rankup.self"))) {
					sender.sendMessage(ChatColor.GOLD+"["+ChatColor.GRAY+"RankUp"+ChatColor.GOLD+"]"+ChatColor.RED+"You do not have permission to use that command!");
					return false;
				} if(sender instanceof Player) {
					if(rankUp((Player) sender, args[0])){
						return true;
					}else{
						sender.sendMessage(ChatColor.GOLD+"["+ChatColor.GRAY+"RankUp"+ChatColor.GOLD+"]"+ChatColor.DARK_RED+args[0]+" is not a valid Rank!");
						return false;
					}
				} else {
					sender.sendMessage(ChatColor.DARK_RED + "You must be a player to use this command!");
				}
			} else if(args.length == 2) {
				if (! (sender.hasPermission("rankup.*") || sender.hasPermission("rankup.others"))) {
					sender.sendMessage(ChatColor.GOLD+"["+ChatColor.GRAY+"RankUp"+ChatColor.GOLD+"]"+ChatColor.RED+"You do not have permission to use that command!");
					return false;
				}
				if(rankUp(Bukkit.getPlayer(args[1]), args[0])){
					return true;
				}else{
					sender.sendMessage(ChatColor.GOLD+"["+ChatColor.GRAY+"RankUp"+ChatColor.GOLD+"]"+ChatColor.DARK_RED+args[0]+" is not a valid Rank!");
					return false;
				}
			}
		}
		return false;
	}
	
	private boolean rankUp(Player player, String group) {
		ConsoleCommandSender ccs = Bukkit.getConsoleSender();
//Get the  next rank
		int intNextRank = -1;
		for (int x = 0; x < groups.size(); x++) {
			if (groups.get(x).equalsIgnoreCase(group)) {
				intNextRank = x;
				break;
			}
		}
		if(intNextRank<0){
			return false;
		}
//Get the players file
		Path playerFile=getPlayerFile(player.getUniqueId()).toPath();
		ArrayList<String> strings=null;
		try {
			strings = (ArrayList<String>)Files.readAllLines(playerFile, StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.log(Level.WARNING, "RankUp - WARNING - An error occured while locating the players file!");
		}
//Retrieve the players current rank from the file
		int currentRank=0;
		String currentRankString="";
		char[] currentRankArray=strings.get(1).toCharArray();
		for (int i=currentRankArray.length-1;i>=strings.get(1).indexOf(":");i--) {
			try {
				int currentRankPlace=Integer.valueOf(currentRankArray[i]);
				currentRankString=currentRankPlace+currentRankString;
			} catch(NumberFormatException e) {
				break;
			}
		}
		currentRank=Integer.valueOf(currentRankString)-5849;
		if (currentRank<intNextRank) {
			int price=0;
			for (int i=currentRank+1;i<=intNextRank;i++) {
				price+=Integer.valueOf(prices.get(i));
			}
			if (intNextRank < groups.size()) {
				if (getServer().dispatchCommand(ccs,"eco take "+player.getName()+" "+price)) {
					if (getServer().dispatchCommand(ccs,"manuadd "+player.getName()+" "+groups.get(intNextRank))) {
						strings.set(1, "CurrentGroup:"+(intNextRank+1));
						try {
							Files.write(playerFile, strings, StandardCharsets.UTF_8);
						} catch (IOException e) {
							logger.log(Level.WARNING, "RankUp - WARNING - An error occured while writing the players file!");
						}
						Bukkit.broadcastMessage(ChatColor.GOLD+"["+ChatColor.GRAY+"RankUp"+ChatColor.GOLD+"]"+player.getDisplayName()+ChatColor.AQUA+" was promoted from the group "+ChatColor.RED+groups.get(currentRank)+ChatColor.AQUA+" to group "+ChatColor.RED+groups.get(intNextRank)+"!");
						return true;
					} else {
						player.sendMessage(ChatColor.RED+"An internal error occured");
					}
				} else {
					player.sendMessage(ChatColor.DARK_RED+"You do not have enough money to rank up!");
				}
			}
		} else if (currentRank==intNextRank) {
			player.sendMessage(ChatColor.GOLD+"["+ChatColor.GRAY+"RankUp"+ChatColor.GOLD+"]"+"You are already in the group "+groups.get(currentRank)+"!");
		} else {
			player.sendMessage(ChatColor.GOLD+"["+ChatColor.GRAY+"RankUp"+ChatColor.GOLD+"]"+"The plugin is called \"RankUp\" not \"RankDown\"!");
		}
		return true;
	}
	
	private boolean reset(Player player, String commandSender) {
		ConsoleCommandSender ccs = Bukkit.getConsoleSender();
		Path playerFile=getPlayerFile(player.getUniqueId()).toPath();
		ArrayList<String> strings=null;
		try {
			strings = (ArrayList<String>)Files.readAllLines(playerFile, StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.log(Level.WARNING, "RankUp - WARNING - An error occured while locating the players file!");
			return false;
		}
		
		if (getServer().dispatchCommand(ccs,"manuadd "+player.getName()+" "+groups.get(0))) {
			strings.set(1, "CurrentGroup:"+1);
			try {
				Files.write(playerFile, strings, StandardCharsets.UTF_8);
			} catch (IOException e) {
				logger.log(Level.WARNING, "RankUp - WARNING - An error occured while writing the players file!");
			}
			player.sendMessage(ChatColor.GOLD+"["+ChatColor.GRAY+"RankUp"+ChatColor.GOLD+"]"+"You were set to the Default rank by "+commandSender);
			return true;
		} else {
			player.sendMessage(ChatColor.RED+"An internal error occured");
			return false;
		}
	}
	
	private void getConfigOptions(){
		groups = new ArrayList<String>();
		prices = new ArrayList<String>();
		identifier = sm.getConfig().getString("identifier");
		int configNumber = 1;
		while (true) {
			if (sm.getConfig().getString("r" + configNumber) != null) {
				groups.add(sm.getConfig().getString("r" + configNumber));
			} else {
				break;
			}
			configNumber++;
		}
		String temp = "";
		String subtemp = "";
		for (int x = 0; x < groups.size(); x++) {
			temp = groups.get(x);
			for (int y = 0; y < temp.length(); y++) {
				if (temp.charAt(y) == identifier.charAt(0)) {
					groups.set(x, temp.substring(0, y));
					subtemp = temp.substring(y);
					prices.add(subtemp.substring(1));
					break;
				}
			}
		}
	}
	
	private File getPlayerFile(UUID uuid) {
		for (File f: playerFiles) {
			if (f.getName().equals(uuid+".yml")) {
				return f;
			}
		}
		return null;
	}
	
	private void initPlayerFiles() {
		playerFiles=new ArrayList<>();
		for (File f:new File(getDataFolder(), "players").listFiles()) {
			addPlayerFile(f);
		}
	}
	
	public void addPlayerFile(File newPlayer){
		playerFiles.add(newPlayer);
	}
}