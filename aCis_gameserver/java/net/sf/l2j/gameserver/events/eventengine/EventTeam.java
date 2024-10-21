package net.sf.l2j.gameserver.events.eventengine;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;


/**
 * @author Anarchy
 */
public class EventTeam
{
	private String name;
	private int color;
	private Location location;
	private List<Player> players;
	private int score = 0;
	
	public EventTeam(String name, int color, Location location)
	{
		this.name = name;
		this.color = color;
		this.location = location;
		players = new ArrayList<>();
	}
	
	public void clear()
	{
		score = 0;
		players.clear();
	}
	
	public void reward(int id, int count)
	{
		for (Player player : players) {
			player.addItem("Event reward.", id, count, null, true);
			
//			for(Achievement _achievement : AchievementsManager.getInstance().getAchievementList().values())
//			{
//				// Possible if admin will do some shit on xml achievements...
//				if(_achievement.getConditions().size() == 0)
//					continue;
//
//				if(_achievement.getConditions().get(0).getName().equals("Min Eventer"))
//				{
//					int votes = Integer.parseInt(player.getAchievementsValues().get(_achievement.getID()));
//
//					player.getAchievementsValues().set(_achievement.getID(), String.valueOf(votes + 1));
//					player.saveAchievementsValues();
//				}
//			}
		}
	}
	
	public void teleportTeam()
	{
		for (Player player : players)
			player.teleToLocation(location,0);
	}
	
	public Location getLocation()
	{
		return location;
	}
	
	public int getScore()
	{
		return score;
	}
	
	public void increaseScore(int count)
	{
		score += count;
	}
	
	public void removePlayer(Player player)
	{
		players.remove(player);
	}
	
	public void addPlayer(Player player)
	{
		players.add(player);
		player.getAppearance().setNameColor(color);
		player.broadcastUserInfo();
	}
	
	public boolean inTeam(Player player)
	{
		return players.contains(player);
	}
	
	public String getName()
	{
		return name;
	}
}