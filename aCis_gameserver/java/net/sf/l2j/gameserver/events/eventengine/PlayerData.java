package net.sf.l2j.gameserver.events.eventengine;


import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;


//import net.sf.l2j.gameserver.model.memo.PlayerMemo;


/**
 * @author Anarchy
 *
 * This class should hold all the information of a player
 * before he entered an event. It is used to restore
 * players' status after an event, or after disconnection.
 */
public class PlayerData
{
	private int playerId;
	private int playerColor;
	private String playerTitle;
	private Location playerLocation;
	
	public PlayerData(Player player)
	{
		playerId = player.getObjectId();
		playerColor = player.getAppearance().getNameColor();
		playerTitle = player.getTitle();
		playerLocation = new Location(player.getX(), player.getY(), player.getZ());
	}
	
	public void restore(Player player)
	{
		if (player.isDead())
			player.doRevive();
		player.getAppearance().setNameColor(playerColor);
		player.setTitle(playerTitle);
		player.setTeam(0);//(TeamType.NONE);
		player.teleToLocation(playerLocation,0);
		player.sendMessage("Your status has been restored after leaving an event.");
		
		// Increase the participated in tasks
//		int participates = PlayerMemo.getVarInt(player, "participated_automatic_events");
//		PlayerMemo.setVar(player, "participated_automatic_events", participates+1, -1);
	}
	
	public int getPlayerId()
	{
		return playerId;
	}
	
	public int getPlayerColor()
	{
		return playerColor;
	}
	
	public String getPlayerTitle()
	{
		return playerTitle;
	}
}