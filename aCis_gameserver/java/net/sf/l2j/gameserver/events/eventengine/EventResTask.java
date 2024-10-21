package net.sf.l2j.gameserver.events.eventengine;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.model.actor.Player;



/**
 * @author Anarchy
 *
 */
public class EventResTask implements Runnable
{
	private AbstractEvent event;
	private List<Player> players;
	
	public EventResTask(AbstractEvent event)
	{
		this.event = event;
		players = new ArrayList<>();
	}
	
	public void addPlayer(Player player)
	{
		players.add(player);
		player.sendMessage("You have been added to the ressurection task.");
	}
	
	@Override
	public void run()
	{
		if (event.getState() != EventState.RUNNING)
			return;
		
		for (Player player : players)
		{
			if (!player.isDead())
				continue;
			
			player.doRevive();
			player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			if (event.getTeam(player) == null)
			{
				player.teleToLocation(event.getRandomLocation(),0);
			}
			else
			{
				player.teleToLocation(event.getTeam(player).getLocation(),0);
			}
		}
	}
}