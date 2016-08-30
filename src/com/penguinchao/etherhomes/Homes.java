package com.penguinchao.etherhomes;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class Homes {
	private EtherHomes main;
	private Connection connection;
	public Homes(EtherHomes passedHomes){
		main = passedHomes;
		establishConnection();
		checkTables();
	}
	public Location getHomeLocation(UUID player, String homeName){
		//returns null if home does not exist
		if(player == null){
			return null;
		}else if(homeName == null){
			homeName = "[default]";
		}else if(homeName == ""){
			homeName = "[default]";
		}
		String worldName;
		double x;
		double y;
		double z;
		float pitch;
		float yaw;
		String query = "SELECT * FROM `etherhomes_homes` WHERE `uuid` = '"+player.toString()+"'; ";
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			ResultSet results = sql.executeQuery();
			while (results.next() ){
				if(results.getString("homename").equalsIgnoreCase(homeName)){
					worldName = results.getString("world");
					x = results.getDouble("x");
					y = results.getDouble("y");
					z = results.getDouble("z");
					pitch = results.getFloat("pitch");
					yaw = results.getFloat("yaw");
					World world = Bukkit.getWorld(worldName);
					if(world == null){
						return null;
					}
					Location returnMe = new Location(world, x, y, z, yaw, pitch);
					return returnMe;
				}
			}
		} catch (SQLException e) {
			main.getLogger().info("[ERROR] Could not get home");
			e.printStackTrace();
			return null;
		}
		return null;
	}
	protected void setHomeLocation(Location newLocation, UUID homeOwner, String homeName){
		if(homeOwner == null){
			return;
		}else if(homeName == null){
			homeName = "[default]";
		}else if(homeName == ""){
			homeName = "[default]";
		}else if(newLocation == null){
			return;
		}
		//Delete old home
		deleteHomeLocation(homeOwner, homeName);
		//Inset new home
		Double x = newLocation.getX();
		Double y = newLocation.getY();
		Double z = newLocation.getZ();
		float pitch = newLocation.getPitch();
		float yaw = newLocation.getYaw();
		String world = newLocation.getWorld().getName();
		String query = "INSERT INTO `etherhomes_homes` (`uuid`, `homename`, `x`, `y`, `z`, `pitch`, `yaw`, `world`) VALUES ('"+homeOwner.toString()+"', '"+homeName+"', '"+x+"', '"+y+"', '"+z+"', '"+pitch+"', '"+yaw+"', '"+world+"')";
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			sql.executeUpdate();
		} catch (SQLException e) {
			main.getLogger().info("[ERROR] Could not set home location");
			e.printStackTrace();
		}
		
	}
	protected void deleteHomeLocation(UUID homeOwner, String homeName){
		boolean homeExists = false;
		if(homeOwner == null){
			return;
		}
		List<String> homes = getAllHomes(homeOwner);
		if(homes == null){
			return;
		}
		for(String entry : homes){
			if(entry.equalsIgnoreCase(homeName)){
				homeExists = true;
				break;
			}
		}
		if(homeExists){
			String query = "DELETE FROM `etherhomes_homes` WHERE `homename` = '"+homeName+"' AND `uuid` = '"+homeOwner.toString()+"';";
			try{
				PreparedStatement sql = connection.prepareStatement(query);
				sql.executeUpdate();
			}catch(SQLException e){
				main.getLogger().info("[ERROR] Could not delete player home");
				e.printStackTrace();
			}
		}
	}
	public List<String> getAllHomes(UUID player){
		if(player == null){
			return null;
		}
		String query = "SELECT `homename` FROM `etherhomes_homes` WHERE `uuid` = '"+player.toString()+"' ORDER BY `homename` ASC ";
		List<String> returnMe = new ArrayList<String>();
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			ResultSet results = sql.executeQuery();
			while(results.next()){
				returnMe.add(results.getString("homename"));
			}
		} catch (SQLException e) {
			main.getLogger().info("[ERROR] Could not get player homes");
			e.printStackTrace();
			return null;
		}
		if(returnMe.size() == 0){
			return null;
		}else{
			return returnMe;
		}
	}
	@SuppressWarnings("deprecation")
	public static UUID getPlayerUUID(String playerName){
		OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		if(player == null){
			return null;
		}
		UUID id = player.getUniqueId();
		return id;
	}
	public static void showHelpSetHome(CommandSender sender){
		sender.sendMessage(ChatColor.GOLD + "/sethome [name]");
	}
	public static void showHelpGoHome(CommandSender sender){
		sender.sendMessage(ChatColor.GOLD + "/home [name]");
	}
	public static void showHelpListHomes(CommandSender sender){
		sender.sendMessage(ChatColor.GOLD + "/listhomes [player]");
	}
	public static void showHelpDeleteHome(CommandSender sender){
		sender.sendMessage(ChatColor.GOLD + "/deletehome [name]");
	}
	private void establishConnection(){
		String mysqlHostName= main.getConfig().getString("mysqlHostName");
		String mysqlPort	= main.getConfig().getString("mysqlPort");
		String mysqlUsername= main.getConfig().getString("mysqlUsername");
		String mysqlPassword= main.getConfig().getString("mysqlPassword");
		String mysqlDatabase= main.getConfig().getString("mysqlDatabase");
		String dburl = "jdbc:mysql://" + mysqlHostName + ":" + mysqlPort + "/" + mysqlDatabase;
		try{
			connection = DriverManager.getConnection(dburl, mysqlUsername, mysqlPassword);
		}catch(Exception exception){
			main.getLogger().info("[ERROR] Could not connect to the database -- disabling EtherBoosts");
			Bukkit.getPluginManager().disablePlugin(main);
		}
	}
	private void checkTables(){
		String createQuery = "CREATE TABLE IF NOT EXISTS `etherhomes_homes` ( `uuid` VARCHAR(36) NOT NULL , `homename` VARCHAR(1000) NOT NULL , `x` DOUBLE NOT NULL , `y` DOUBLE NOT NULL , `z` DOUBLE NOT NULL , `pitch` DOUBLE NOT NULL , `yaw` DOUBLE NOT NULL , `world` VARCHAR(100) NOT NULL ) ENGINE = InnoDB; ";
		try{
			java.sql.PreparedStatement sql = connection.prepareStatement(createQuery);
			sql.executeUpdate();
		}catch(SQLException e){
			main.getLogger().info("[ERROR] Could not check tables");
			e.printStackTrace();
		}
	}
}
