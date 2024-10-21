package net.sf.l2j.gameserver.events.eventengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.NpcTable;
import net.sf.l2j.gameserver.data.SpawnTable;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.skills.AbnormalEffect;

/**
 * @author Anarchy
 *
 */
public abstract class AbstractEvent implements Runnable
{
	private String name;
	private int id;
	private EventState state;
	protected List<Player> players;
	private Map<Player, Integer> playerScores;
	private List<Location> teleportLocations;
	protected List<EventTeam> teams;
	private List<Npc> spawnedNpcs;
	private ScheduledFuture<?> endTask = null;
	protected EventResTask eventRes = null;
	private ScheduledFuture<?> resTask = null;
	protected EventInformation eventInfo = null;
	private ScheduledFuture<?> infoTask = null;
	
	private ScheduledFuture<?> checkSayTask = null;
	private int runningMinutes;
	
	protected AbstractEvent(String name, int id, int runningMinutes)
	{
		this.name = name;
		this.id = id;
		state = EventState.INACTIVE;
		players = new ArrayList<>();
		teleportLocations = new ArrayList<>();
		teams = new ArrayList<>();
		playerScores = new HashMap<>();
		spawnedNpcs = new ArrayList<>();
		this.runningMinutes = runningMinutes;
	}
	
	protected int getTopScore()
	{
		int topScore = 0;
		for (Player player : playerScores.keySet())
			if (playerScores.get(player) > topScore)
				topScore = playerScores.get(player);
		
		return topScore;
	}
	
	protected void rewardTopInDraw(Map<Integer, Integer> rewards)
	{
		int topScore = 0;
		for (EventTeam et : teams)
			if (et.getScore() > topScore)
				topScore = et.getScore();
		
		List<EventTeam> topTeams = new ArrayList<>();
		for (EventTeam et : teams)
			if (et.getScore() == topScore)
				topTeams.add(et);
		
		for (EventTeam et : topTeams)
			for (int id : rewards.keySet())
				et.reward(id, rewards.get(id));
	}
	
	protected void rewardTopInDraw(int id, int count)
	{
		Map<Integer, Integer> rewards = new HashMap<>();
		rewards.put(id, count);
		rewardTopInDraw(rewards);
	}
	
	protected boolean draw()
	{
		int topScore = 0;
		EventTeam topTeam = null;
		for (EventTeam et : teams)
		{
			if (et.getScore() > topScore)
			{
				topScore = et.getScore();
				topTeam = et;
			}
		}
		
		if (topScore == 0 || topTeam == null)
			return false;
		
		for (EventTeam et : teams)
		{
			if (et.getScore() == topScore && et != topTeam)
				return true;
		}
		
		return false;
	}
	
	protected Npc spawnNpc(int npcId, Location location, String title)
	{
		Npc ret = null;
		NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
		try
		{
			L2Spawn spawn = new L2Spawn(template);
			spawn.setLoc(location.getX(), location.getY(), location.getZ(), 0);
			spawn.setRespawnDelay(10);
			SpawnTable.getInstance().addNewSpawn(spawn, false);
			spawn.doSpawn(false);
			spawn.setRespawnState(false);
			spawn.getNpc().setTitle(title);
			spawn.getNpc().broadcastPacket(new NpcInfo(spawn.getNpc(), null));
			
			ret = spawn.getNpc();
		}
		catch (Exception e)
		{
			System.out.println("Event Manager: Unable to spawn npc with id "+npcId+".");
			e.printStackTrace();
		}
		
		if (ret != null)
			spawnedNpcs.add(ret);
		
		return ret;
	}
	
	protected void addTeam(String name, int color, Location location)
	{
		teams.add(new EventTeam(name, color, location));
	}
	
	public Location getRandomLocation()
	{
		return teleportLocations.size() > 1 ? teleportLocations.get(Rnd.get(teleportLocations.size())) : teleportLocations.get(0);
	}
	
	protected void increaseScore(Player player, int count)
	{
		if (playerScores.containsKey(player))
		{
			int old = playerScores.get(player);
			playerScores.put(player, old+count);
		}
		else
			playerScores.put(player, count);
		
		//System.out.println("[AbstractEvent] "+player.getName()+" points: "+playerScores.get(player));
		
		if (!teams.isEmpty())
			getTeam(player).increaseScore(1);
	}
	
	protected void abort()
	{
		announce("The event was canceled due to lack of participation.", true);
		cleanUp();
	}
	
	protected boolean enoughRegistered(int count)
	{
		return players.size() >= count;
	}
	
	protected void rewardTopTeams(int top, Map<Integer, Integer> rewards)
	{
		List<EventTeam> temp = new ArrayList<>();
		temp.addAll(teams);
		
		for (int i = 1; i <= top; i++)
		{
			EventTeam topTeam = null;
			int score = 0;
			
			for (EventTeam team : temp)
			{
				if (team.getScore() > score)
				{
					topTeam = team;
					score = team.getScore();
				}
			}
			
			if (topTeam == null)
				break;
			
			temp.remove(topTeam);
			for (int id : rewards.keySet())
				topTeam.reward(id, rewards.get(id));
			topTeam = null;
			score = 0;
		}
	}
	
	protected void rewardTopTeams(int top, int id, int count)
	{
		Map<Integer, Integer> rewards = new HashMap<>();
		rewards.put(id, count);
		rewardTopTeams(top, rewards);
	}
	
	protected void announceTopTeams(int top)
	{
		List<EventTeam> temp = new ArrayList<>();
		temp.addAll(teams);
		
		announce("The top team(s) of the event:", true);
		
		for (int i = 1; i <= top; i++)
		{
			EventTeam topTeam = null;
			int score = 0;
			
			for (EventTeam team : temp)
			{
				if (team.getScore() > score)
				{
					topTeam = team;
					score = team.getScore();
				}
			}
			
			if (topTeam == null)
				break;
			
			temp.remove(topTeam);
			announce(i+". Team: "+topTeam.getName()+" Score: "+score, true);
			topTeam = null;
			score = 0;
		}
	}
	
	protected void rewardTop(int top, Map<Integer, Integer> rewards)
	{
		List<Player> temp = new ArrayList<>();
		temp.addAll(players);
		
		for (int i = 1; i <= top; i++)
		{
			Player topPlayer = null;
			int score = 0;
			
			for (Player player : temp)
			{
				if (!playerScores.containsKey(player))
					continue;
				
				if (playerScores.get(player) > score)
				{
					topPlayer = player;
					score = playerScores.get(player);
				}
			}
			
			if (topPlayer == null)
				break;
			
			temp.remove(topPlayer);
			for (int id : rewards.keySet()) {
				topPlayer.addItem("Event reward.", id, rewards.get(id), null, true);
			}
			topPlayer = null;
			score = 0;
		}
	}
	
	protected void rewardTop(int top, int id, int count)
	{
		Map<Integer, Integer> rewards = new HashMap<>();
		rewards.put(id, count);
		rewardTop(top, rewards);
	}
	
	protected void announceTop(int top)
	{
		List<Player> temp = new ArrayList<>();
		temp.addAll(players);
		
		announce("The top player(s) of the event:", true);
		
		for (int i = 1; i <= top; i++)
		{
			Player topPlayer = null;
			int score = 0;
			
			for (Player player : temp)
			{
				if (!playerScores.containsKey(player))
					continue;
				
				if (playerScores.get(player) > score)
				{
					topPlayer = player;
					score = playerScores.get(player);
				}
			}
			
			if (topPlayer == null)
				break;
			
			temp.remove(topPlayer);
			announce(i+". Player: "+topPlayer.getName()+" Score: "+score, true);
			topPlayer = null;
			score = 0;
		}
	}
	
	protected void cleanUp()
	{
		state = EventState.INACTIVE;
		players.clear();
		playerScores.clear();
		
		for (EventTeam et : teams)
			et.clear();
		for (Npc spawn : spawnedNpcs)
			spawn.deleteMe();
		spawnedNpcs.clear();
		if (endTask != null)
			endTask.cancel(true);
		endTask = null;
		if (checkSayTask != null)
			checkSayTask.cancel(true);
		checkSayTask = null;
		if (resTask != null)
		{
			resTask.cancel(true);
			resTask = null;
		}
		if (infoTask != null)
		{
			infoTask.cancel(true);
			infoTask = null;
		}
		if (eventInfo != null)
		{
			Map<String, Integer> temp = new HashMap<>();
			for (String key : eventInfo.getReplacements().keySet())
				temp.put(key, 0);
			eventInfo.setReplacements(temp);
		}
		EventManager.getInstance().scheduleNextEvent(false);
	}
	
	protected void restorePlayer(Player player)
	{
		if (player.isOnline())
			EventManager.getInstance().restorePlayer(player);
	}
	
	protected void restorePlayers()
	{
		for (Player player : players)
			if (player.isOnline())
				EventManager.getInstance().restorePlayer(player);
		unparalizePlayers();
	}
	
	protected void end()
	{
		state = EventState.TELEPORTING;
		announce("The event has ended. Players will be teleported back in 10 seconds.", false);
		paralizePlayers();
		schedule(() -> restorePlayers(), 10);
		schedule(() -> cleanUp(), 11);
	}

	protected void onCheck()
	{ }

	protected void start()
	{
		state = EventState.TELEPORTING;
		announce("The registrations have closed. The event has started.", true);
		announce("You will be teleported in 20 seconds. Get ready!", false);
		preparePlayers();
		schedule(() -> teleportPlayers(), 20);
	}
	
	protected void cancelEndTask()
	{
		endTask.cancel(true);
		endTask = null;
	}
	
	protected void cancelCheckTask()
	{
		checkSayTask.cancel(true);
		checkSayTask = null;
	}
	
	protected void teleportPlayers()
	{
		state = EventState.RUNNING;
		
		if (!teams.isEmpty())
			for (EventTeam team : teams)
				team.teleportTeam();
		else
			for (Player player : players)
				player.teleToLocation(getRandomLocation(),0);
		
		paralizePlayers();
		announce("You have been teleported to the event.", false);
		announce("The event will begin in 20 seconds!", false);
		schedule(() -> begin(), 20);
	}
	
	protected void begin()
	{
		if(!getStartingMsg().equals(""))
		{
			for (Player player : players) {
				player.sendPacket(new ExShowScreenMessage(getStartingMsg(), 3000, 2, false));				
				player.sendPacket(new CreatureSay(player.getObjectId(), 1, "Simon Event", getStartingMsg()));
			}
			
			System.out.println(getStartingMsg());
		}
		
		unparalizePlayers();
		announce("The event has begun!", false);
		
		if(getId() == 4) // Simon (without warns to end)
		{
			checkSayTask = ThreadPool.scheduleAtFixedRate(() -> onCheck(), 10*1000, 30*1000);
		}
		else
		{
			warnEventEnd(runningMinutes*60);
			schedule(() -> warnEventEnd((runningMinutes*60)/2), (runningMinutes*60)/2);
			schedule(() -> warnEventEnd(30), (runningMinutes*60)-30);
			schedule(() -> warnEventEnd(5), (runningMinutes*60)-5);
			schedule(() -> warnEventEnd(4), (runningMinutes*60)-4);
			schedule(() -> warnEventEnd(3), (runningMinutes*60)-3);
			schedule(() -> warnEventEnd(2), (runningMinutes*60)-2);
			schedule(() -> warnEventEnd(1), (runningMinutes*60)-1);
			endTask = schedule(() -> end(), runningMinutes*60);
		}

		if (eventRes != null)
			resTask = ThreadPool.scheduleAtFixedRate(eventRes, 8*1000, 8*1000);
		if (eventInfo != null)
			infoTask = ThreadPool.scheduleAtFixedRate(eventInfo, 1*1000, 1*1000);
	}
	
	private void warnEventEnd(int seconds)
	{
		int mins = seconds / 60;
		int secs = seconds % 60;
		
		announce((mins == 0 ? "" : mins+" minute(s)")+(mins > 0 && secs > 0 ? " and " : "")+(secs == 0 ? "" : secs+" second(s)")+" remaining until the event ends.", false);
	}
	
	protected void unparalizePlayers()
	{
		for (Player player : players)
		{
			player.setIsParalyzed(false);
			player.stopAbnormalEffect(AbnormalEffect.ROOT);
		}
	}
	
	protected void paralizePlayers()
	{
		for (Player player : players)
		{
			player.setIsParalyzed(true);
			player.startAbnormalEffect(AbnormalEffect.ROOT);
		}
	}
	
	protected void addTeleportLocation(Location location)
	{
		teleportLocations.add(location);
	}
	
	protected void addTeleportLocation(int x, int y, int z)
	{
		teleportLocations.add(new Location(x, y, z));
	}
	
	protected void preparePlayers()
	{
		EventManager.getInstance().storePlayersData(players);
		if (!teams.isEmpty())
			splitToTeams();
	}
	
	protected void splitToTeams()
	{
		int i = 0;
		for (Player player : players)
		{
			teams.get(i++).addPlayer(player);
			if (i > teams.size()-1)
				i = 0;
		}
	}
	
	protected void openRegistrations()
	{
		state = EventState.REGISTERING;
		warnRegistrations(Config.EVENT_REGISTRATION_TIME*60);
		schedule(() -> warnRegistrations((Config.EVENT_REGISTRATION_TIME*60)/2), (Config.EVENT_REGISTRATION_TIME*60)/2);
		schedule(() -> warnRegistrations(30), (Config.EVENT_REGISTRATION_TIME*60)-30);
		schedule(() -> warnRegistrations(5), (Config.EVENT_REGISTRATION_TIME*60)-5);
	}
	
	private void warnRegistrations(int seconds)
	{
		int mins = seconds / 60;
		int secs = seconds % 60;
		
		announce("The registrations will close in "+(mins == 0 ? "" : mins+" minute(s)")+(mins > 0 && secs > 0 ? " and " : "")+(secs == 0 ? "" : +secs+" second(s)")+".", true);
	}
	
	protected ScheduledFuture<?> schedule(Runnable task, int seconds)
	{
		return ThreadPool.schedule(task, seconds*1000);
	}
	
	protected void announce(String msg, boolean global)
	{
		if (global)
			EventManager.announce(getName(), msg);
		else
			EventManager.announce(getName(), msg, players);
	}
	
	public boolean isAutoAttackable(Player attacker, Player target)
	{
		return false;
	}
	
	public void onKill(Player killer, Player victim)
	{ }
	
	public boolean onSay(Player player, String text)
	{ 
		return true;
	}

	public boolean onInterract(Player player, Npc npc)
	{
		return false;
	}
	
	public boolean canAttack(Player attacker, Player target)
	{
		return true;
	}
	
	public boolean canHeal(Player healer, Player target)
	{
		return true;
	}
	
	public boolean canUseItem(Player player, int itemId)
	{
		return true;
	}
	
	public boolean allowDiePacket(Player player)
	{
		return true;
	}
	
	public boolean isDisguisedEvent()
	{
		return false;
	}
	
	public EventTeam getTeam(Player player)
	{
		EventTeam ret = null;
		for (EventTeam team : teams)
		{
			if (team.inTeam(player))
			{
				ret = team;
				break;
			}
		}
		
		return ret;
	}
	
	protected int getScore(Player player)
	{
		return playerScores.containsKey(player) ? playerScores.get(player) : 0;
	}
	
	public boolean isEventNpc(Npc npc)
	{
		return spawnedNpcs.contains(npc);
	}
	
	public boolean isInEvent(Player player)
	{
		return players.contains(player);
	}
	
	public List<Player> getPlayers()
	{
		return players;
	}
	
	public void registerPlayer(Player player)
	{
		players.add(player);
	}
	
	public void removePlayer(Player player)
	{
		players.remove(player);
	}
	
	public EventState getState()
	{
		return state;
	}
	
	public int getId()
	{
		return id;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getStartingMsg()
	{
		return "";
	}
}