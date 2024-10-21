/*
 * L2J_EngineMods
 * Engine developed by Fissban.
 *
 * This software is not free and you do not have permission
 * to distribute without the permission of its owner.
 *
 * This software is distributed only under the rule
 * of www.devsadmins.com.
 * 
 * Contact us with any questions by the media
 * provided by our web or email marco.faccio@gmail.com
 */
package enginemods.engine.mods;

//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;

import enginemods.data.ConfigData;
import enginemods.engine.AbstractMods;
import enginemods.holders.RewardHolder;
import enginemods.util.Util;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

/**
 * @author fissban
 */
public class PvpReward extends AbstractMods
{
	public class PvPHolder
	{
		public int _victim;
		public long _time;
		
		public PvPHolder(int victim, long time)
		{
			_victim = victim;
			_time = time;
		}
	}
	
	/**
	 * Constructor
	 */
	public PvpReward()
	{
		registerMod(ConfigData.ENABLE_PvpReward);
	}
	
	@Override
	public void onModState()
	{
		//
	}
	
	@Override
	public void onKill(Creature killer, Creature victim, boolean isPet)
	{
		if (!Util.areObjectType(Player.class, victim) || killer.getActingPlayer() == null)
		{
			return;
		}
		
		Player killerPc = killer.getActingPlayer();
		
		giveRewards(killerPc, (Player) victim);
	}
	
	/**
	 * Se entregan los premios y se envia un mensaje custom por cada premio.
	 * @param killer
	 * @param victim
	 */
	private static void giveRewards(Player killer, Player victim)
	{
		for (RewardHolder reward : ConfigData.PVP_REWARDS)
		{
			if (Rnd.get(100) <= reward.getRewardChance())
			{
				killer.sendPacket(new CreatureSay(0, Say2.TELL, "", "Have won " + reward.getRewardCount() + " " + ItemTable.getInstance().getTemplate(reward.getRewardId()).getName()));
				killer.getActingPlayer().getInventory().addItem("PvpReward", reward.getRewardId(), reward.getRewardCount(), killer, victim);
			}
		}
	}
}
