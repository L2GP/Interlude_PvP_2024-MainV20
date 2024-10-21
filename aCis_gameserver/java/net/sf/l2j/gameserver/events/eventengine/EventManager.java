package net.sf.l2j.gameserver.events.eventengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.events.eventengine.event.CTF;
import net.sf.l2j.gameserver.events.eventengine.event.DM;
import net.sf.l2j.gameserver.events.eventengine.event.TvT;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.util.Broadcast;


/**
 * @author Anarchy
 *
 * This class should manage the creation and scheduling of
 * the events, as well as hold information of the participants.
 */
public final class EventManager
{
	private Map<Integer, AbstractEvent> events;
	private List<PlayerData> playersData;
	private List<Integer> availableEvents;
	private int nextEvent;
	private int pasaEvento;
	private AbstractEvent activeEvent = null;
	
	protected EventManager()
	{
		if (!Config.ENABLE_EVENT_ENGINE)
		{
			System.out.println("Event Manager: Event Engine is disabled");
			return;
		}
		
		events = new HashMap<>();
		playersData = new ArrayList<>();
		availableEvents = new ArrayList<>();
		
		if (Config.ALLOW_DM_EVENT)
			registerEvent(new DM());
		if (Config.ALLOW_TVT_EVENT)
			registerEvent(new TvT());
		if (Config.ALLOW_CTF_EVENT)
			registerEvent(new CTF());		
		
		pasaEvento = 0;
		availableEvents.addAll(events.keySet());
		nextEvent = availableEvents.get(pasaEvento);		
		
		System.out.println("Event Manager: Loaded "+events.size()+" event(s)");
		
		scheduleNextEvent(true);
	}
	
	public void setActiveEvent(AbstractEvent event)
	{
		activeEvent = event;
		if (event != null)
			announce("EventManager", "The next event will be "+event.getName()+"!");
	}
	
	public void scheduleNextEvent(boolean first)
	{		
		if (first)
			ThreadPool.schedule(events.get(nextEvent), (Config.TIME_BETWEEN_EVENTS * 1000 * 60)/2);
		else
			ThreadPool.schedule(events.get(nextEvent), Config.TIME_BETWEEN_EVENTS * 1000 * 60);
				
		if(availableEvents.size() == 1) {
			nextEvent = availableEvents.get(0);
		}
		
		else if(availableEvents.size() == 2) {				
			pasaEvento++;
			
			if(pasaEvento == 2) { 
				pasaEvento = 0; 
			}
			
			nextEvent = availableEvents.get(pasaEvento);			
		}
		
		else if(availableEvents.size() == 3) {			
			
			pasaEvento++;
			
			if(pasaEvento == 3) { 
				pasaEvento = 0; 
			}
			
			nextEvent = availableEvents.get(pasaEvento);						
		}		
	}
	
	public int getTotalParticipants()
	{
		return activeEvent.getPlayers().size();
	}
	
	public void removePlayer(Player player)
	{
		if (activeEvent == null || activeEvent.getState() != EventState.REGISTERING)
		{
			player.sendMessage("You cannot unregister now.");
			return;
		}
		if (!activeEvent.isInEvent(player))
		{
			player.sendMessage("You are not registered to the event.");
			return;
		}
		
		activeEvent.removePlayer(player);
		player.sendMessage("You have successfully unregistered from the event.");
	}
	
	public void registerPlayer(Player player)
	{
		if (activeEvent == null || activeEvent.getState() != EventState.REGISTERING)
		{
			player.sendMessage("You cannot register now.");
			return;
		}
		if (activeEvent.isInEvent(player))
		{
			player.sendMessage("You are already registered to the event.");
			return;
		}
		
		activeEvent.registerPlayer(player);
		player.sendMessage("You have successfully registered to the event.");
	}
	
	public void storePlayersData(List<Player> players)
	{
		for (Player player : players)
			playersData.add(new PlayerData(player));
	}
	
	public void restorePlayer(Player player)
	{
		PlayerData playerData = null;
		for (PlayerData pd : playersData)
		{
			if (pd.getPlayerId() == player.getObjectId())
			{
				playerData = pd;
				pd.restore(player);
				break;
			}
		}
		
		if (playerData != null)
			playersData.remove(playerData);
	}
	
	public AbstractEvent getActiveEvent()
	{
		return activeEvent;
	}
	
	private void registerEvent(AbstractEvent event)
	{
		events.put(event.getId(), event);
	}
	
	public static void announce(String from, String msg, List<Player> to)
	{
		CreatureSay cs = new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, from, from + ": " +msg);
		for (Player player : to)
			player.sendPacket(cs);
	}
	
	public static void announce(String from, String msg)
	{
		CreatureSay cs = new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, from, from + ": " +msg);
		Broadcast.toAllOnlinePlayers(cs);
	}
	
	public static EventManager getInstance()
	{
		return SingletonHolder.instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EventManager instance = new EventManager();
	}
}