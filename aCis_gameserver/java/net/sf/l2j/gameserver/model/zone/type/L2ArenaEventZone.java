package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.zone.L2SpawnZone;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;

public class L2ArenaEventZone extends L2SpawnZone
{	
	public L2ArenaEventZone(int id)
	{
		super(id);
	}

	@Override
	protected void onEnter(Creature character)
	{		
		if (character instanceof Player)
		{
			Player player = (Player) character;
			if (player.isArenaProtection())
			{
				if (player.getPvpFlag() > 0)
					PvpFlagTaskManager.getInstance().remove(player);
				player.updatePvPFlag(1);
				player.broadcastUserInfo();
			}
			((Player) character).sendPacket(SystemMessageId.ENTERED_COMBAT_ZONE);
		}
		
		character.setInsideZone(ZoneId.ARENA_EVENT, true);
		character.setInsideZone(ZoneId.PVP, true);
	}

	@Override
	protected void onExit(Creature character)
	{		
		if (character instanceof Player)
		{
			Player player = (Player) character;

			player.updatePvPFlag(0);
			player.broadcastUserInfo();

			((Player) character).sendPacket(SystemMessageId.LEFT_COMBAT_ZONE);
		}
		
		character.setInsideZone(ZoneId.ARENA_EVENT, false);
		character.setInsideZone(ZoneId.PVP, false);
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.model.zone.ZoneType#onDieInside(net.sf.l2j.gameserver.model.actor.Creature)
	 */
	@Override
	public void onDieInside(Creature character)
	{
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.model.zone.ZoneType#onReviveInside(net.sf.l2j.gameserver.model.actor.Creature)
	 */
	@Override
	public void onReviveInside(Creature character)
	{
		// TODO Auto-generated method stub
		
	}	
}