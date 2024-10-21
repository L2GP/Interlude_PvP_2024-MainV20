package net.sf.l2j.gameserver.events.dynamiczones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;

import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
//import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;


/**
 * @author Anarchy
 *
 */
public class PvpZoneManager
{
	protected ArrayList<PvpZone> pvpZones;
	private PvpZone currentZone;
	private int nextZone;
	private Map<Player, Integer> kills;
	private long lastChange;
	
	public static PvpZoneManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected PvpZoneManager()
	{
		System.out.println("PvpZoneManager: Started");
		
		nextZone = 0;
		pvpZones = createPvpZones();
		currentZone = pvpZones.get(nextZone);
		nextZone++;
		lastChange = System.currentTimeMillis();
		kills = new HashMap<>();
		System.out.println("Current zone: "+currentZone.getName());
		ThreadPool.schedule(new Update(), 1000*60*55);
	}
	
	public long getTimeLeft()
	{
		return (lastChange + (1000*60*60)) - System.currentTimeMillis();
	}	
	
	public void addKill(Player player)
	{
		if (kills.containsKey(player))
		{
			int curr = kills.get(player);
			kills.put(player, curr+1);
		}
		else
			kills.put(player, 1);
	}
	
	public PvpZone getCurrentZone()
	{
		return currentZone;
	}
	
	protected void updateZone()
	{
		PvpZone newZone = pvpZones.get(nextZone);
		nextZone++;
		lastChange = System.currentTimeMillis();
		
		if (nextZone >= pvpZones.size())
			nextZone = 0;
		
		if (newZone == null)
		{
			System.out.println("There was a problem while updating the pvp zone.");
			return;
		}
		
		currentZone = newZone;
		
		kills.clear();
//		Broadcast.toAllOnlinePlayers(SystemMessage.sendString("The PvP zone was changed to: "+newZone.getName()));
		Broadcast.toAllOnlinePlayers(new CreatureSay(0, Say2.SHOUT, "The PvP zone was changed to: ", newZone.getName()));
		for (Player p : World.getInstance().getPlayers())
		{
			if (p.isInsideZone(ZoneId.MULTI_FUNCTION))
			{
				if (p.isCastingNow())
					p.abortCast();
				if (p.isAttackingNow())
					p.abortAttack();
				
				int[] randLoc = newZone.getRandomRespawnLocation();
				p.teleToLocation(new Location(randLoc[0], randLoc[1], randLoc[2]),0);
			}
		}
	}
	
	protected class Update implements Runnable
	{
		private int phase = 0;
		
		@Override
		public void run()
		{
			if (phase == 0)
			{
//				Broadcast.toAllOnlinePlayers(SystemMessage.sendString(("The PvP zone will be changed in 5 minutes.")));
				Broadcast.toAllOnlinePlayers(new CreatureSay(0, Say2.SHOUT, "\"The PvP zone will be changed in 5 minutes.\"", ""));
				phase = 1;
				ThreadPool.schedule(this, 1000*60*5);
				return;
			}
			
			updateZone();
			phase = 0;
			ThreadPool.schedule(this, 1000*60*55);
		}
	}
	
	private static ArrayList<PvpZone> createPvpZones()
	{
		ArrayList<PvpZone> ret = new ArrayList<>();
		
		for (Map<String, Integer[]> key : Config.PVP_ZONES.keySet())
		{
			String name = "";
			for (String s : key.keySet())
				name = s;
			Integer[] gkLocI = key.get(name);
			int[] gkLoc = new int[gkLocI.length];
			for (int i = 0; i < gkLocI.length; i++)
				gkLoc[i] = gkLocI[i];
			
			int[][] respLocs = new int[Config.PVP_ZONES.get(key).size()][3];
			int j = 0;
			for (Integer[] i : Config.PVP_ZONES.get(key))
			{
				respLocs[j][0] = i[0];
				respLocs[j][1] = i[1];
				respLocs[j][2] = i[2];
				j++;
			}
			
			PvpZone pz = new PvpZone(name, gkLoc, respLocs);
			ret.add(pz);
		}
		
		return ret;
	}
	
	private static class SingletonHolder
	{
		protected static final PvpZoneManager _instance = new PvpZoneManager();
	}
}
