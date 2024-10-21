package net.sf.l2j.gameserver.events.dynamiczones;

import net.sf.l2j.commons.random.Rnd;

/**
 * @author Anarchy
 *
 */
public class PvpZone
{
	private String name;
	private int[] gkLoc;
	private int[][] respLocs;
	
	public PvpZone(String name, int[] gkLoc, int[][] respLocs)
	{
		this.name = name;
		this.gkLoc = gkLoc;
		this.respLocs = respLocs;
	}
	
	public int[] getGkSpawnLocation()
	{
		return gkLoc;
	}
	
	public int[] getRandomRespawnLocation()
	{
		return respLocs[Rnd.get(respLocs.length)];
	}
	
	public String getName()
	{
		return name;
	}
}