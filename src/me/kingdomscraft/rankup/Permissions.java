package me.kingdomscraft.rankup;

import org.bukkit.permissions.Permission;

public class Permissions {
	public Permission canPreformAll=new Permission("rankup.*");
	
	public Permission canPreformOnOthers=new Permission("rankup.others");
	public Permission canPreformOnSelf=new Permission("rankup.self");
}
