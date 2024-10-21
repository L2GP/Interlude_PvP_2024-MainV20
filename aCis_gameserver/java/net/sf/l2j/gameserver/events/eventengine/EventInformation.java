package net.sf.l2j.gameserver.events.eventengine;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author Anarchy
 *
 */
public class EventInformation implements Runnable
{
	private AbstractEvent event;
	private String msg;
	private Map<String, Integer> replacements;
	
	public EventInformation(AbstractEvent event, String msg)
	{
		this.event = event;
		this.msg = msg;
		replacements = new HashMap<>();
	}
	
	public void setReplacements(Map<String, Integer> val)
	{
		replacements = val;
	}
	
	public Map<String, Integer> getReplacements()
	{
		return replacements;
	}
	
	public void addReplacement(String id, int value)
	{
		replacements.put(id, value);
	}
	
	@Override
	public void run()
	{
		String finalMsg = msg;
		for (String r : replacements.keySet())
			finalMsg = finalMsg.replaceAll(r, replacements.get(r)+"");
		ExShowScreenMessage sm = new ExShowScreenMessage(finalMsg, 3000, 2, false);		
		for (Player player : event.getPlayers())
			player.sendPacket(sm);
	}
}