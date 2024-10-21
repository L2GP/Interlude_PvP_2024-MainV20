package net.sf.l2j.gameserver.model.actor.status;

import java.util.Collection;

import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo;

public class AttackableStatus extends NpcStatus
{
	public AttackableStatus(Attackable activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public final void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false);
	}
	
	@Override
	public final void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHpConsumption)
	{
		if (getActiveChar().isDead())
			return;
		
		if (value > 0)
		{
			if (getActiveChar().isOverhit())
				getActiveChar().setOverhitValues(attacker, value);
			else
				getActiveChar().overhitEnabled(false);
		}
		else
			getActiveChar().overhitEnabled(false);
		
		// Add attackers to npc's attacker list
		if (attacker != null)
			getActiveChar().addAttackerToAttackByList(attacker);
		
		super.reduceHp(value, attacker, awake, isDOT, isHpConsumption);
		
		// And the attacker's hit didn't kill the mob, clear the over-hit flag
		if (!getActiveChar().isDead())
			getActiveChar().overhitEnabled(false);
	}
	
	@Override
	public void setCurrentHp(double newHp, boolean broadcastPacket)
	{
		super.setCurrentHp(newHp, broadcastPacket);

		if (getActiveChar().getFakePc() != null)
		{
			Collection<Player> plrs = getActiveChar().getKnownType(Player.class);
			for (Player player : plrs)
			{
				if (player != null)
					player.sendPacket(new AbstractNpcInfo.NpcInfo(getActiveChar(), player));
			}
		}
	}
	
	@Override
	public Attackable getActiveChar()
	{
		return (Attackable) super.getActiveChar();
	}
}