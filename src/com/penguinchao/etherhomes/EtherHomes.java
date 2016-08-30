package com.penguinchao.etherhomes;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EtherHomes extends JavaPlugin {
	protected Homes homes;
	//private boolean debugEnabled;
	@Override
	public void onEnable(){
		saveDefaultConfig();
		//debugEnabled = getConfig().getBoolean("debug-enabled");
		homes = new Homes(this);
	}
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) { 
		debugTrace("[onCommand]");
		if (cmd.getName().equalsIgnoreCase("home")){
			debugTrace("[onCommand] [home]");
			if(sender instanceof Player){
				//Correct -- Do Nothing
				debugTrace("[onCommand] [home] A player sent command");
			}else{
				debugTrace("[onCommand] [home] A nonplayer sent command -- Done");
				sender.sendMessage(ChatColor.RED + "This can only be sent by a player");
				return false;
			}
			Player player = (Player) sender;
			if(!ArrayUtils.isNotEmpty(args)){
				debugTrace("[onCommand] [home] args were 0");
				//Go to default home
				Location destination = homes.getHomeLocation(player.getUniqueId(), "[default]");
				if(destination == null){
					player.sendMessage(ChatColor.RED+"That home does not exist");
					return false;
				}
				player.teleport(destination);
				player.sendMessage(ChatColor.GOLD+"Teleported");
				return false;
			}else if(args.length == 1){
				debugTrace("[onCommand] [home] args were 1");
				//Go to specific home
				if(args[0].contains("\"") || args[0].contains("'") || args[0].contains(" ")){
					debugTrace("[onCommand] [home] argument had spaces - Done");
					sender.sendMessage(ChatColor.RED+"You cannot have quotations in your home name");
					getLogger().info(ChatColor.RED+sender.getName()+" possibly tried to use SQL injection");
					return false;
				}else if( args[0].contains(":") ){
					//Visiting someone else's home
					debugTrace("[onCommand] [home] argument had colon");
					if(player.hasPermission("etherhomes.other.visit")){
						//Has permission to visit the home of others
						String[] argSplit = args[0].split(":");
						if(argSplit.length != 2){
							//ERROR
							sender.sendMessage(ChatColor.RED + "Syntax: /home <PlayerName>:<HomeName>");
							sender.sendMessage(ChatColor.GRAY + "To go to a default home, use [default] as the home name");
							return false;
						}else{
							//Continue as normal
							UUID homeOwner = Homes.getPlayerUUID(argSplit[0]);
							String homeName = argSplit[1];
							Location destination = homes.getHomeLocation(homeOwner, homeName);
							if(destination == null){
								player.sendMessage(ChatColor.RED+"That home is not set. Use /listhomes [name]");
								return false;
							}
							player.teleport(destination);
							player.sendMessage(ChatColor.GOLD+"Teleported");
							return false;
						}
					}else{
						//Does not have permission -- using "decoy" message
						sender.sendMessage(ChatColor.RED+"You cannot have colons in your home name");
						return false;
					}
				}else{
					//Going to own home
					debugTrace("[onCommand] [home] Argument did not have a colon. Sending to own home.");
					Location destination = homes.getHomeLocation(player.getUniqueId(), args[0]);
					debugTrace("[onCommand] [home] Location retrieved. Teleporting...");
					if(destination == null){
						player.sendMessage(ChatColor.RED+"That home does not exist");
						return false;
					}
					player.teleport(destination);
					player.sendMessage(ChatColor.GOLD+"Teleported");
					debugTrace("[onCommand] [home] Done");
					return false;
				}
			}else{
				//Error
				debugTrace("[onCommand] [home] args were > 1");
				Homes.showHelpGoHome(sender);
				return false;
			}
		}else if(cmd.getName().equalsIgnoreCase("sethome")){
			if(sender instanceof Player){
				//Correct -- Do Nothing
			}else{
				sender.sendMessage(ChatColor.RED + "This can only be sent by a player");
				return false;
			}
			if(!ArrayUtils.isNotEmpty(args)){
				//Set Default Home
				Player player = (Player) sender;
				homes.setHomeLocation(player.getLocation(), player.getUniqueId(), "[default]");
				player.sendMessage(ChatColor.GOLD+"Default home has been set");
				return false;
			}if(args.length > 1){
				//Error
				Homes.showHelpSetHome(sender);
				return false;
			}else{
				//Set a home
				if(args[0].contains("\"") || args[0].contains("'") || args[0].contains(" ")){
					sender.sendMessage(ChatColor.RED+"You cannot have quotations in your home name");
					getLogger().info(ChatColor.RED+sender.getName()+" possibly tried to use SQL injection");
					return false;
				}else if(args[0].contains(":")){
					//Visiting someone else's home
					Player player = (Player) sender;
					if(player.hasPermission("etherhomes.other.set")){
						//Has permission to set the home of others
						String[] argSplit = args[0].split(":");
						if(argSplit.length != 2){
							//ERROR
							sender.sendMessage(ChatColor.RED + "Syntax: /sethome <PlayerName>:<HomeName>");
							sender.sendMessage(ChatColor.GRAY + "To set a default home, use [default] as the home name");
							return false;
						}else{
							//Continue as normal
							UUID homeOwner = Homes.getPlayerUUID(argSplit[0]);
							String homeName = argSplit[1];
							if(homeOwner == null){
								player.sendMessage(ChatColor.RED+"That player could not be found");
								return false;
							}
							homes.setHomeLocation(player.getLocation(), homeOwner, homeName);
							player.sendMessage(ChatColor.GOLD+"You have set the home '"+homeName+"' for "+argSplit[0]);
							return false;
						}
					}else{
						//Does not have permission -- using "decoy" message
						sender.sendMessage(ChatColor.RED+"You cannot have colons in your home name");
						return false;
					}
				}else{
					//Setting your own home
					Player player = (Player) sender;
					homes.setHomeLocation(player.getLocation(), player.getUniqueId(), args[0]);
					player.sendMessage(ChatColor.GOLD+"Home '"+args[0]+"' has been set");
					return false;
				}
			}
		}else if(cmd.getName().equalsIgnoreCase("listhomes")){
			if(!ArrayUtils.isNotEmpty(args)){
				//List Own Homes
				if(sender instanceof Player){
					Player player = (Player) sender;
					List<String> allHomes = homes.getAllHomes(player.getUniqueId());
					if(allHomes == null){
						sender.sendMessage(ChatColor.RED+"No homes have been set! Use /sethome [name] to set one");
					}else{
						String sendMe = "Your homes: ";
						boolean onFirst = true;
						for(String entry : allHomes){
							if(onFirst){
								onFirst = false;
								sendMe = sendMe+entry;
							}else{
								sendMe = sendMe+", "+entry;
							}
						}
						sender.sendMessage(ChatColor.GOLD+sendMe);
					}
				}else{
					sender.sendMessage(ChatColor.RED+"Console cannot set homes. Please specify a player: /listhomes <player name>");
				}
			}else if(args.length == 1){
				//List Someone else's homes
				if(sender instanceof Player){
					Player player = (Player) sender;
					if(player.hasPermission("etherhomes.other.visit")){
						//Has Permission -- Do nothing
					}else{
						player.sendMessage(ChatColor.RED+"You do not have permission to view the homes of others!");
						return false;
					}
				}
				List<String> allHomes = homes.getAllHomes(Homes.getPlayerUUID(args[0]));
				if(allHomes == null){
					sender.sendMessage(ChatColor.RED+"No homes have been set by "+args[0]);
				}else{
					String sendMe = args[0]+"'s homes: ";
					boolean onFirst = true;
					for(String entry : allHomes){
						if(onFirst){
							onFirst = false;
							sendMe = sendMe+entry;
						}else{
							sendMe = sendMe+", "+entry;
						}
					}
					sender.sendMessage(ChatColor.GOLD+sendMe);
				}
			}else{
				//Error
				Homes.showHelpListHomes(sender);
			}
		}else if(cmd.getName().equalsIgnoreCase("deletehome")){
			if(!ArrayUtils.isNotEmpty(args)){
				//Delete Default Home
				if(sender instanceof Player){
					Player player = (Player) sender;
					homes.deleteHomeLocation(player.getUniqueId(), "[default]");
					player.sendMessage(ChatColor.GOLD+"Your default home has been unset!");
				}else{
					sender.sendMessage(ChatColor.RED+"Console does not have a default home. Please specify a player before the homename: /deletehome <PlayerName>:<HomeName>");
				}
			}else if(args.length == 1){
				//Delete a named home or someone else's default
				if(args[0].contains("\"") || args[0].contains("'") || args[0].contains(" ")){
					debugTrace("[onCommand] [home] argument had spaces - Done");
					sender.sendMessage(ChatColor.RED+"You cannot have quotations in your home name");
					getLogger().info(ChatColor.RED+sender.getName()+" possibly tried to use SQL injection");
					return false;
				}else if( args[0].contains(":") ){
					//Delete another player's home
					Player player = (Player) sender;
					if(player.hasPermission("etherhomes.other.set")){
						//Has permission
						String[] argSplit = args[0].split(":");
						if(argSplit.length != 2){
							player.sendMessage(ChatColor.RED+"Syntax: /deletehome <PlayerName>:<HomeName>");
						}else{
							UUID uuid = Homes.getPlayerUUID(argSplit[0]);
							homes.deleteHomeLocation(uuid, argSplit[1]);
							player.sendMessage(ChatColor.GOLD+"Player "+argSplit[0]+" no longer has home '"+argSplit[1]+"' set!");
						}
					}else{
						//Doesn't have permission
						player.sendMessage(ChatColor.RED+"You cannot have colons in your home name");
						return false;
					}
				}else{
					//Delete own home
					Player player = (Player) sender;
					homes.deleteHomeLocation(player.getUniqueId(), args[0]);
					player.sendMessage(ChatColor.GOLD+"Your home '"+args[0]+"' is no longer set!");
				}
			}else{
				//Show help
				Homes.showHelpDeleteHome(sender);
			}
		}
		return false;
	}
	@SuppressWarnings("unused")
	protected void debugTrace(String message){
		//Did not use debug much
		if(false){
			getLogger().info("[DEBUG] "+message);
		}
	}
}
