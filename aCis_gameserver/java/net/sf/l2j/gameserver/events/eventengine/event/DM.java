package net.sf.l2j.gameserver.events.eventengine.event;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.events.eventengine.AbstractEvent;
import net.sf.l2j.gameserver.events.eventengine.EventInformation;
import net.sf.l2j.gameserver.events.eventengine.EventManager;
import net.sf.l2j.gameserver.events.eventengine.EventResTask;
import net.sf.l2j.gameserver.events.eventengine.EventState;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;

/**
 * @author Anarchy
 *
 */
public class DM extends AbstractEvent
{
	public DM()
	{
		super("DeathMatch", 1, Config.DM_RUNNING_TIME);
		for (Location location : Config.DM_RESPAWN_SPOTS)
			addTeleportLocation(location);
		eventRes = new EventResTask(this);
		eventInfo = new EventInformation(this, "Top kills: %top%");
		eventInfo.addReplacement("%top%", getTopScore());
	}

	@Override
	public void run()
	{
		EventManager.getInstance().setActiveEvent(this);
		openRegistrations();
		schedule(() -> start(), (Config.EVENT_REGISTRATION_TIME*60)+1);
	}
	
	@Override
	protected void start()
	{
		if (!enoughRegistered(Config.DM_MIN_PLAYERS))
		{
			abort();
			return;
		}
		
		super.start();
	}
	
	@Override
	protected void end()
	{
		announceTop(1);
		rewardTop(1, Config.DM_WINNER_REWARDS);
		super.end();
	}
	
	@Override
	protected void preparePlayers()
	{
		super.preparePlayers();
		for (Player player : players)
			player.setTitle("Kills: 0");
	}
	
	@Override
	protected void increaseScore(Player player, int count)
	{
		super.increaseScore(player, count);
		for (int itemId : Config.DM_ON_KILL_REWARDS.keySet())
			player.addItem("Event reward.", itemId, Config.DM_ON_KILL_REWARDS.get(itemId), null, true);
		player.setTitle("Kills: "+getScore(player));
		player.broadcastUserInfo();
		eventInfo.addReplacement("%top%", getTopScore());
	}
	
	@Override
	public boolean isAutoAttackable(Player attacker, Player target)
	{
		return true;
	}
	
	@Override
	public void onKill(Player killer, Player victim)
	{
		increaseScore(killer, 1);
		eventRes.addPlayer(victim);
	}
	
	@Override
	public boolean canHeal(Player healer, Player target)
	{
		return healer == target;
	}
	
	@Override
	public boolean canAttack(Player attacker, Player target)
	{
		return getState() == EventState.RUNNING;
	}
	
	@Override
	public boolean allowDiePacket(Player player)
	{
		return false;
	}
	
	@Override
	public boolean isDisguisedEvent()
	{
		return true;
	}
}