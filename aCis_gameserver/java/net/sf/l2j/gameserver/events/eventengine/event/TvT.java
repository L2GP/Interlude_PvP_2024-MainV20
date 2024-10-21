package net.sf.l2j.gameserver.events.eventengine.event;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.events.eventengine.AbstractEvent;
import net.sf.l2j.gameserver.events.eventengine.EventInformation;
import net.sf.l2j.gameserver.events.eventengine.EventManager;
import net.sf.l2j.gameserver.events.eventengine.EventResTask;
import net.sf.l2j.gameserver.events.eventengine.EventState;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author Anarchy
 *
 */
public class TvT extends AbstractEvent
{
	public TvT()
	{
		super("TvT", 2, Config.TVT_RUNNING_TIME);
		addTeam(Config.TVT_TEAM_1_NAME, Config.TVT_TEAM_1_COLOR, Config.TVT_TEAM_1_LOCATION);
		addTeam(Config.TVT_TEAM_2_NAME, Config.TVT_TEAM_2_COLOR, Config.TVT_TEAM_2_LOCATION);
		eventRes = new EventResTask(this);
		eventInfo = new EventInformation(this, Config.TVT_TEAM_1_NAME+": %team1Score% | "+Config.TVT_TEAM_2_NAME+": %team2Score%");
		eventInfo.addReplacement("%team1Score%", teams.get(0).getName().equals(Config.TVT_TEAM_1_NAME) ? teams.get(0).getScore() : teams.get(1).getScore());
		eventInfo.addReplacement("%team2Score%", teams.get(0).getName().equals(Config.TVT_TEAM_2_NAME) ? teams.get(0).getScore() : teams.get(1).getScore());
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
		if (!enoughRegistered(Config.TVT_MIN_PLAYERS))
		{
			abort();
			return;
		}
		
		super.start();
	}
	
	@Override
	protected void end()
	{
		if (!draw())
		{
			announceTopTeams(1);
			rewardTopTeams(1, Config.TVT_WINNER_REWARDS);
		}
		else
		{
			announce("The event ended in a draw.", true);
			rewardTopInDraw(Config.TVT_DRAW_REWARDS);
		}
		
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
		player.setTitle("Kills: "+getScore(player));
		player.broadcastUserInfo();
		eventInfo.addReplacement("%team1Score%", teams.get(0).getName().equals(Config.TVT_TEAM_1_NAME) ? teams.get(0).getScore() : teams.get(1).getScore());
		eventInfo.addReplacement("%team2Score%", teams.get(0).getName().equals(Config.TVT_TEAM_2_NAME) ? teams.get(0).getScore() : teams.get(1).getScore());
	}
	
	@Override
	public boolean isAutoAttackable(Player attacker, Player target)
	{
		return getTeam(attacker) != getTeam(target);
	}
	
	@Override
	public void onKill(Player killer, Player victim)
	{
		if (getTeam(killer) != getTeam(victim))
			increaseScore(killer, 1);
		
		eventRes.addPlayer(victim);
	}
	
	@Override
	public boolean canHeal(Player healer, Player target)
	{
		return getTeam(healer) == getTeam(target);
	}
	
	@Override
	public boolean canAttack(Player attacker, Player target)
	{
		return getTeam(attacker) != getTeam(target) && getState() == EventState.RUNNING;
	}
	
	@Override
	public boolean allowDiePacket(Player player)
	{
		return false;
	}
}