package net.sf.l2j.gameserver.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.NpcTable;
import net.sf.l2j.gameserver.data.SpawnTable;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedBossEventCMD;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

public class BossEvent
{
	
	   public L2Spawn bossSpawn;
	   public List<Location> locList = new ArrayList<>();
	   public Location loc;
	   public List<Integer> bossList = new ArrayList<>();
	   public int bossId;
	   public int objectId;
	   public List<Player> eventPlayers = new ArrayList<>();
	   protected static final Logger _log = Logger.getLogger(BossEvent.class.getName());
	   private EventState state = EventState.INACTIVE;
	   public boolean started = false;
	   public boolean aborted = false;
	   private Player lastAttacker = null;
	   private Map<Integer, Integer> generalRewards = new HashMap<>();
	   @SuppressWarnings("unused")
	   private Map<Integer, Integer> lastAttackerRewards = new HashMap<>();
	   private Map<Integer, Integer> mainDamageDealerRewards = new HashMap<>();
	   public ScheduledFuture<?> despawnBoss = null;
	   public ScheduledFuture<?> countDownTask = null;
	   private String bossName = "";
	   public boolean bossKilled = false;
	   public L2Spawn eventNpc = null;
	   public long startTime;

	   public enum EventState
	   {
	       REGISTRATION, TELEPORTING, WAITING, FIGHTING, FINISHING, INACTIVE
	   }

	   public BossEvent()
	   {
	   	VoicedCommandHandler.getInstance().registerHandler(new VoicedBossEventCMD());
	  	 NextBossEvent.getInstance().startCalculationOfNextEventTime();
	  	 _log.info("Boss Event loaded.");
	   }

	   public boolean addPlayer(Player player)
	   {
	       return eventPlayers.add(player);
	   }

	   public boolean removePlayer(Player player)
	   {
	       return eventPlayers.remove(player);
	   }

	   public boolean isRegistered(Player player)
	   {
	       return eventPlayers.contains(player);
	   }

	   class Registration implements Runnable
	   {
	       @Override
	       public void run()
	       {
	           startRegistration();

	       }

	   }

	   public void teleToTown()
	   {
	       for (Player p : eventPlayers)
	       {
	           p.teleToLocation(new Location(83374, 148081, -3407), 300);
	    	   // p.teleToLocation(new Location(Config.BOSS_EVENT_PLAYER_LOCATION));
	       }
	       setState(EventState.INACTIVE);
	   }

	   public void delay(int delay)
	   {
	       try
	       {
	           Thread.sleep(delay);
	       }
	       catch (InterruptedException e)
	       {
	           // TODO Auto-generated catch block
	           e.printStackTrace();
	       }
	   }

	   class Teleporting implements Runnable
	   {
	       Location teleTo;
	       List<Player> toTeleport = new ArrayList<>();

	       public Teleporting(List<Player> toTeleport, Location teleTo)
	       {
	           this.teleTo = teleTo;
	           this.toTeleport = toTeleport;
	       }

	       @Override
	       public void run()
	       {
	           if (eventPlayers.size() >= Config.BOSS_EVENT_MIN_PLAYERS)
	           {
	               despawnNpc(eventNpc);
	               setState(EventState.TELEPORTING);
	               announce("Event Started!", false);
	               startCountDown(Config.BOSS_EVENT_TIME_TO_TELEPORT_PLAYERS, true);

	               for (Player p : toTeleport)
	               {
	                   ThreadPool.schedule(new Runnable()
	                   {

	                       @Override
	                       public void run()
	                       {
	                    	   p.teleToLocation(teleTo, 300);

	                       }
	                   }, Config.BOSS_EVENT_TIME_TO_TELEPORT_PLAYERS * 1000);

	               }
	               delay(Config.BOSS_EVENT_TIME_TO_TELEPORT_PLAYERS * 1000);
	               setState(EventState.WAITING);
	               startCountDown(Config.BOSS_EVENT_TIME_TO_WAIT, true);
	               ThreadPool.schedule(new Fighting(bossId, teleTo), Config.BOSS_EVENT_TIME_TO_WAIT * 1000);

	           }
	           else
	           {
	               announce("Event was cancelled due to lack of participation!", false);
	               setState(EventState.INACTIVE);
	               despawnNpc(eventNpc);
	               eventPlayers.clear();
	               objectId = 0;

	           }

	       }

	   }

	   public void reward(Player p, Map<Integer, Integer> rewardType)
	   {

	       for (Map.Entry<Integer, Integer> entry : rewardType.entrySet())
	       {
	           p.addItem("BossEventReward", entry.getKey(), entry.getValue(), null, true);
	       }

	   }

	   public void rewardPlayers()
	   {
	       for (Player p : eventPlayers)
	       {
	           if (p.getBossEventDamage() > Config.BOSS_EVENT_MIN_DAMAGE_TO_OBTAIN_REWARD)
	           {
	               reward(p, generalRewards);
	           }
	           else
	           {
	               p.sendPacket(new ExShowScreenMessage("You didn't caused min damage to receive rewards!", 5000));
	               p.sendMessage("You didn't caused min damage to receive rewards! Min. Damage: " + Config.BOSS_EVENT_MIN_DAMAGE_TO_OBTAIN_REWARD + ". Your Damage: " + p.getBossEventDamage());
	           }
	       }

	       if (Config.BOSS_EVENT_REWARD_MAIN_DAMAGE_DEALER)
	       {
	           if (getMainDamageDealer() != null)
	           {
	               reward(getMainDamageDealer(), mainDamageDealerRewards);
	               getMainDamageDealer().sendChatMessage(0, Say2.CRITICAL_ANNOUNCE, "[Boss Event]", "Congratulations, you was the damage dealer! So you will receive wonderful rewards.");
	           }

	       }
	       
	       
	   }

	   public void finishEvent()
	   {
	       started = false;
	       // if (!AdminBossEvent.manual)
	       // {
	       NextBossEvent.getInstance().startCalculationOfNextEventTime();
	       // }
	       // else
	       // {
	       // AdminBossEvent.manual = false;
	       // }
	       rewardPlayers();
	       if (bossKilled) announce(bossName + " has been defeated!", false);
	       if (Config.BOSS_EVENT_REWARD_LAST_ATTACKER)
	       {
	           if (lastAttacker != null)
	           {
	               announce("LastAttacker: " + lastAttacker.getName(), false);
	           }
	       }

	       if (Config.BOSS_EVENT_REWARD_MAIN_DAMAGE_DEALER)
	       {
	           if (getMainDamageDealer() != null)
	           {
	               announce("Main Damage Dealer: " + getMainDamageDealer().getName() + ". Total Damage = " + getMainDamageDealer().getBossEventDamage(), false);
	           }
	       }
	       ThreadPool.schedule(new Runnable()
	       {

	           @Override
	           public void run()
	           {
	               teleToTown();
	               eventPlayers.clear();

	           }
	       }, Config.BOSS_EVENT_TIME_TO_TELEPORT_PLAYERS * 1000);

	       setState(EventState.FINISHING);
	       startCountDown(Config.BOSS_EVENT_TIME_TO_TELEPORT_PLAYERS, true);
	       if (despawnBoss != null)
	       {
	           despawnBoss.cancel(true);
	           despawnBoss = null;
	       }
	       objectId = 0;

	   }

	   class Fighting implements Runnable
	   {
	       int bossId;
	       Location spawnLoc;

	       public Fighting(int bossId, Location spawnLoc)
	       {
	           this.bossId = bossId;
	           this.spawnLoc = spawnLoc;
	       }

	       @Override
	       public void run()
	       {
	           if (spawnNpc(bossId, loc.getX(), loc.getY(), loc.getZ()))
	           {
	               setState(EventState.FIGHTING);
	               if (Config.BOSS_EVENT_TIME_ON_SCREEN)
	               {
	                   startCountDown(Config.BOSS_EVENT_TIME_TO_DESPAWN_BOSS, true);
	               }
	               despawnBoss = ThreadPool.schedule(new DespawnBossTask(bossSpawn), Config.BOSS_EVENT_TIME_TO_DESPAWN_BOSS * 1000);
	               objectId = bossSpawn.getNpc().getObjectId();
	               for (Player p : eventPlayers)
	               {
	                   p.sendPacket(new ExShowScreenMessage("Boss " + bossSpawn.getNpc().getName() + " has been spawned. Go and Defeat him!", 5000));
	               }

	           }

	       }

	   }

	   public void despawnNpc(L2Spawn spawn)
	   {
	       if (spawn != null)
	       {
	           spawn.getNpc().deleteMe();
	           spawn.setRespawnState(false);
	           SpawnTable.getInstance().deleteSpawn(spawn, true);
	       }

	   }

	   class DespawnBossTask implements Runnable
	   {
	       L2Spawn spawn;

	       public DespawnBossTask(L2Spawn spawn)
	       {
	           this.spawn = spawn;
	       }

	       @Override
	       public void run()
	       {
	           if (spawn != null)
	           {
	               announceScreen("Your time is over " + spawn.getNpc().getName() + " returned to his home!", true);
	               announce("Your time is over " + spawn.getNpc().getName() + " returned to his home!", true);
	               announce("You will be teleported to town.", true);
	               despawnNpc(spawn);
	               ThreadPool.schedule(new Runnable()
	               {

	                   @Override
	                   public void run()
	                   {
	                       teleToTown();
	                       eventPlayers.clear();
	                       setState(EventState.INACTIVE);
	                       objectId = 0;

	                   }
	               }, 10000);
	           }
	       }

	   }

	   public void startRegistration()
	   {
	       try
	       {
	           resetPlayersDamage();
	           bossKilled = false;
	           bossList = Config.BOSS_EVENT_ID;
	           bossId = bossList.get(Rnd.get(bossList.size()));
	           locList = Config.BOSS_EVENT_LOCATION;
	           loc = locList.get(Rnd.get(locList.size()));
	           if (NpcTable.getInstance().getTemplate(bossId) != null)
	           {
	               startTime = System.currentTimeMillis() + Config.BOSS_EVENT_REGISTRATION_TIME * 1000;
	               eventNpc = spawnEventNpc(Config.BOSS_EVENT_NPC_REGISTER_LOC._x, Config.BOSS_EVENT_NPC_REGISTER_LOC._y, Config.BOSS_EVENT_NPC_REGISTER_LOC._z);
	               generalRewards = Config.BOSS_EVENT_GENERAL_REWARDS;
	               lastAttackerRewards = Config.BOSS_EVENT_LAST_ATTACKER_REWARDS;
	               mainDamageDealerRewards = Config.BOSS_EVENT_MAIN_DAMAGE_DEALER_REWARDS;
	               started = true;
	               aborted = false;
	               bossName = NpcTable.getInstance().getTemplate(bossId).getName();
	               setState(EventState.REGISTRATION);
	               announce("[Boss Event] Registration started!", false);
	               announce("[Boss Event] Joinable in giran or use command \".bossevent\" to register to event", false);
	               startCountDown(Config.BOSS_EVENT_REGISTRATION_TIME, false);

	               ThreadPool.schedule(new Teleporting(eventPlayers, loc), Config.BOSS_EVENT_REGISTRATION_TIME * 1000);

	           }
	           else
	           {
	               _log.warning(getClass().getName() + ": cannot be started. Invalid BossId: " + bossList);
	               return;
	           }
	       }
	       catch (Exception e)
	       {
	           _log.warning("[Boss Event]: Couldn't be started");
	           e.printStackTrace();
	       }

	   }

	   public int timeInMillisToStart()
	   {
	       return (int) (startTime - System.currentTimeMillis()) / 1000;
	   }

	   public void startCountDownEnterWorld(Player player)
	   {
	       if (getState() == EventState.REGISTRATION)
	       {
	           ThreadPool.schedule(new Countdown(player, timeInMillisToStart(), getState()), 0);
	       }
	   }

	   public boolean spawnNpc(int npcId, int x, int y, int z)
	   {
	       NpcTemplate tmpl = NpcTable.getInstance().getTemplate(npcId);
	       try
	       {
	           bossSpawn = new L2Spawn(tmpl);

	           bossSpawn.setLoc(x, y, z, Rnd.get(65535));
	           bossSpawn.setRespawnDelay(1);

	           SpawnTable.getInstance().addNewSpawn(bossSpawn, false);

	           bossSpawn.setRespawnState(false);
	           bossSpawn.doSpawn(false);
	           bossSpawn.getNpc().isAggressive();
	           bossSpawn.getNpc().decayMe();
	           bossSpawn.getNpc().spawnMe(bossSpawn.getNpc().getX(), bossSpawn.getNpc().getY(), bossSpawn.getNpc().getZ());
	           bossSpawn.getNpc().broadcastPacket(new MagicSkillUse(bossSpawn.getNpc(), bossSpawn.getNpc(), 1034, 1, 1, 1));
	           return true;
	       }
	       catch (Exception e)
	       {
	           e.printStackTrace();
	           return false;
	       }
	   }

	   public void resetPlayersDamage()
	   {
	       for (Player p : World.getInstance().getPlayers())
	       {
	           p.setBossEventDamage(0);
	       }
	   }

	   public L2Spawn spawnEventNpc(int x, int y, int z)
	   {
	       L2Spawn spawn = null;
	       NpcTemplate tmpl = NpcTable.getInstance().getTemplate(Config.BOSS_EVENT_REGISTRATION_NPC_ID);
	       try
	       {
	           spawn = new L2Spawn(tmpl);

	           spawn.setLoc(x, y, z, Rnd.get(65535));
	           spawn.setRespawnDelay(1);

	           SpawnTable.getInstance().addNewSpawn(spawn, false);

	           spawn.setRespawnState(false);
	           spawn.doSpawn(false);
	           spawn.getNpc().isAggressive();
	           spawn.getNpc().decayMe();
	           spawn.getNpc().spawnMe(spawn.getNpc().getX(), spawn.getNpc().getY(), spawn.getNpc().getZ());
	           spawn.getNpc().broadcastPacket(new MagicSkillUse(spawn.getNpc(), spawn.getNpc(), 1034, 1, 1, 1));
	           return spawn;
	       }
	       catch (Exception e)
	       {
	           e.printStackTrace();
	           return spawn;
	       }
	   }

	   public final Player getMainDamageDealer()
	   {
	       int dmg = 0;
	       Player mainDamageDealer = null;
	       for (Player p : eventPlayers)
	       {
	           if (p.getBossEventDamage() > dmg)
	           {
	               dmg = p.getBossEventDamage();
	               mainDamageDealer = p;
	           }
	       }
	       return mainDamageDealer;
	   }

	   public static BossEvent getInstance()
	   {
	       return SingleTonHolder._instance;
	   }

	   private static class SingleTonHolder
	   {
	       protected static final BossEvent _instance = new BossEvent();
	   }

	   public void startCountDown(int time, boolean eventOnly)
	   {
	       Collection<Player> players = new ArrayList<>();
	       players = eventOnly ? eventPlayers : World.getInstance().getPlayers();
	       for (Player player : players)
	       {
	           ThreadPool.schedule(new Countdown(player, time, getState()), 0L);
	       }

	   }

	   public void announce(String text, boolean eventOnly)
	   {
	       Collection<Player> players = new ArrayList<>();
	       players = eventOnly ? eventPlayers : World.getInstance().getPlayers();
	       for (Player player : players)
	       {
	           player.sendPacket(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, "[Boss Event]", text));
	       }
	   }

	   public void announceScreen(String text, boolean eventOnly)
	   {
	       Collection<Player> players = new ArrayList<>();
	       players = eventOnly ? eventPlayers : World.getInstance().getPlayers();
	       for (Player player : players)
	       {
	           player.sendPacket(new ExShowScreenMessage(text, 4000));
	       }
	   }

	   /**
	    * @return the state
	    */
	   public EventState getState()
	   {
	       return state;
	   }

	   /**
	    * @param state the state to set
	    */
	   public void setState(EventState state)
	   {
	       this.state = state;
	   }

	   /**
	    * @return the lastAttacker
	    */
	   public Player getLastAttacker()
	   {
	       return lastAttacker;
	   }

	   /**
	    * @param lastAttacker the lastAttacker to set
	    */
	   public void setLastAttacker(Player lastAttacker)
	   {
	       this.lastAttacker = lastAttacker;
	   }

	   protected class Countdown implements Runnable
	   {
	       private final Player _player;
	       private final int _time;
	       private String text = "";
	       EventState evtState;

	       public Countdown(Player player, int time, EventState evtState)
	       {
	           _time = time;
	           _player = player;
	           switch (evtState)
	           {
	           case REGISTRATION:
	               text = "[Boss Event] registration ends in: ";
	               break;
	           case TELEPORTING:
	               text = "[Boss Event] You will be teleported in: ";
	               break;
	           case WAITING:
	               text = "[Boss Event] Boss will spawn in: ";
	               break;
	           case FINISHING:
	               text = "[Boss Event] You will be teleported to City in: ";
	               break;
	           case FIGHTING: // Agrega este caso
	               text = "[Boss Event] Fight has started!"; // O el mensaje que desees
	               break;
	           default:
	               text = "[Boss Event] Unknown event state."; // Manejo de casos no esperados
	               break;    
	           }
	           this.evtState = evtState;
	       }

	       @Override
	       public void run()
	       {
	           if (getState() == EventState.INACTIVE)
	           {
	               return;
	           }
	           if (_player.isOnline())
	           {
	               switch (evtState)
	               {
	               case INACTIVE:
	               case REGISTRATION:
	               case TELEPORTING:
	               case WAITING:
	               case FINISHING:
	                   switch (_time)
	                   {

	                   case 60:
	                   case 120:
	                   case 180:
	                   case 240:
	                   case 300:
	                       _player.sendPacket(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, "[Boss Event]", text + _time / 60 + " minute(s)"));
	                       break;
	                   case 45:
	                   case 30:
	                   case 15:
	                   case 10:
	                   case 5:
	                   case 4:
	                   case 3:
	                   case 2:
	                   case 1:
	                       _player.sendPacket(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, "[Boss Event]", text + _time + " second(s)"));
	                       break;

	                   }
	                   if (_time > 1)
	                   {
	                       ThreadPool.schedule(new Countdown(_player, _time - 1, evtState), 1000L);
	                   }
	                   break;
	               case FIGHTING:
	                   int minutes = _time / 60;
	                   int second = _time % 60;
	                   String timing = ((minutes < 10) ? ("0" + minutes) : minutes) + ":" + ((second < 10) ? ("0" + second) : second);

	                   _player.sendPacket(new ExShowScreenMessage("Time Left: " + timing, 1100, SMPOS.BOTTOM_RIGHT, true));
	                   if (_time > 1)
	                   {
	                       ThreadPool.schedule(new Countdown(_player, _time - 1, evtState), 1000L);
	                   }
	                   break;
	               }

	           }
	       }
	   }
}
