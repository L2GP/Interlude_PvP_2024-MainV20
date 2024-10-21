package net.sf.l2j.gameserver.scripting.scripts.ai.individual;


/**
 * @author caio
 *
 */
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
 
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.zone.type.L2BossZone;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.scripts.ai.L2AttackableAIScript;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.util.Broadcast;
 
public class Zaken extends L2AttackableAIScript
{
	protected static final Logger log = Logger.getLogger(Zaken.class.getName());
	private static final L2BossZone _zakenLair = ZoneManager.getInstance().getZoneById(110000, L2BossZone.class);
	private int _1001 = 0;
	private int _ai0 = 0;
	private int _ai1 = 0;
	private int _ai2 = 0;
	private int _ai3 = 0;
	private int _ai4 = 0;
	private int _quest0 = 0;
	private int _quest1 = 0;
	private int _quest2 = 0;
	private Player c_quest0 = null;
	private Player c_quest1 = null;
	private Player c_quest2 = null;
	private Player c_quest3 = null;
	private Player c_quest4 = null;
	private static final int ZAKEN = 29022;
	private static final int doll_blader_b = 29023;
	private static final int vale_master_b = 29024;
	private static final int pirates_zombie_captain_b = 29026;
	private static final int pirates_zombie_b = 29027;
	private static final int[] Xcoords =
	{
		53950,
		55980,
		54950,
		55970,
		53930,
		55970,
		55980,
		54960,
		53950,
		53930,
		55970,
		55980,
		54960,
		53950,
		53930
	};
	private static final int[] Ycoords =
	{
		219860,
		219820,
		218790,
		217770,
		217760,
		217770,
		219920,
		218790,
		219860,
		217760,
		217770,
		219920,
		218790,
		219860,
		217760
	};
	private static final int[] Zcoords =
	{
		-3488,
		-3488,
		-3488,
		-3488,
		-3488,
		-3216,
		-3216,
		-3216,
		-3216,
		-3216,
		-2944,
		-2944,
		-2944,
		-2944,
		-2944
	};
	private static final byte ALIVE = 0;
	private static final byte DEAD = 1;
 
	public Zaken()
	{
		super("ai/individual");
		ThreadPool.scheduleAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					int time = GameTimeTaskManager.getInstance().getGameTime();
					int hour = (time / 60) % 24;
					if (hour  == Config.ZAKEN_ENABLE_DOOR)
					{
						_log.info("Zaken door id 21240006 opened, game time 00.00.");
 
						DoorData.getInstance().getDoor(21240006).openMe();
						
						Broadcast.announceToOnlinePlayers("Devil's Isle door is now opened for 2 minutes!");
						
						ThreadPool.schedule(new Runnable()
						{
							@Override
							public void run()
							{
								try
								{
									Broadcast.announceToOnlinePlayers("Devil's Isle door is now closed!");
									DoorData.getInstance().getDoor(21240006).closeMe();
								}
							catch (Throwable e)
								{
									log.warning("Cannot close door ID: 21240006 " + e);
								}
							}
						}, 300000L);
					}
				}
				catch (Throwable e)
				{
					log.warning("Cannot open door ID: 21240006 " + e);
				}
			}
		}, 2000L, 600000L);
		StatsSet info = GrandBossManager.getInstance().getStatsSet(ZAKEN);
		int status = GrandBossManager.getInstance().getBossStatus(ZAKEN);
		if (status == DEAD)
		{
			long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			if (temp > 0)
			{
				startQuestTimer("zaken_unlock", temp, null, null, false);
			}
			else
			{
			final GrandBoss zaken = (GrandBoss) addSpawn(ZAKEN, 55276, 219176, -3227, 0, false, 0,false);
				GrandBossManager.getInstance().setBossStatus(ZAKEN, ALIVE);
				spawnBoss(zaken);
				if (Config.ANNOUNCE_BOSS_ALIVE)
				{
					Broadcast.announceToOnlinePlayers("Epic Boss: " + zaken.getName() + " spawned in world.");
				}
			}
		}
		else
		{
			info.getInteger("loc_x");
			info.getInteger("loc_y");
			info.getInteger("loc_z");
			info.getInteger("heading");
			int hp = info.getInteger("currentHP");
			int mp = info.getInteger("currentMP");
			final GrandBoss zaken = (GrandBoss) addSpawn(ZAKEN, 55276, 219176, -3227, 0, false, 0,false);
			zaken.setCurrentHpMp(hp, mp);
			zaken.setCurrentHp(zaken.getMaxHp());
            zaken.setCurrentMp(zaken.getMaxMp());
			spawnBoss(zaken);
			if (Config.ANNOUNCE_BOSS_ALIVE)
			{
				Broadcast.announceToOnlinePlayers("Epic Boss: " + zaken.getName() + " spawned in world.");
			}
		}
	}
 
	@Override
	protected void registerNpcs()
	{
		addEventIds(ZAKEN, EventType.ON_ATTACK, EventType.ON_KILL, EventType.ON_SPAWN, EventType.ON_SPELL_FINISHED, EventType.ON_SKILL_SEE, EventType.ON_FACTION_CALL, EventType.ON_AGGRO);
		addEventIds(doll_blader_b, EventType.ON_ATTACK, EventType.ON_KILL, EventType.ON_SPAWN, EventType.ON_SPELL_FINISHED, EventType.ON_SKILL_SEE, EventType.ON_FACTION_CALL, EventType.ON_AGGRO);
		addEventIds(vale_master_b, EventType.ON_ATTACK, EventType.ON_KILL, EventType.ON_SPAWN, EventType.ON_SPELL_FINISHED, EventType.ON_SKILL_SEE, EventType.ON_FACTION_CALL, EventType.ON_AGGRO);
		addEventIds(pirates_zombie_captain_b, EventType.ON_ATTACK, EventType.ON_KILL, EventType.ON_SPAWN, EventType.ON_SPELL_FINISHED, EventType.ON_SKILL_SEE, EventType.ON_FACTION_CALL, EventType.ON_AGGRO);
		addEventIds(pirates_zombie_captain_b, EventType.ON_ATTACK, EventType.ON_KILL, EventType.ON_SPAWN, EventType.ON_SPELL_FINISHED, EventType.ON_SKILL_SEE, EventType.ON_FACTION_CALL, EventType.ON_AGGRO);
	}
 
	public void spawnBoss(GrandBoss npc)
	{
		if (npc == null)
		{
			log.warning("Zaken AI failed to load, missing Zaken in grandboss_data.sql");
			return;
		}
 
		GrandBossManager.getInstance().addBoss(npc);
		npc.broadcastPacket(new PlaySound(1, "BS01_A", npc));
		_ai0 = 0;
		_ai1 = npc.getX();
		_ai2 = npc.getY();
		_ai3 = npc.getZ();
		_quest0 = 0;
		_quest1 = 0;
		_quest2 = 3;
		if (_zakenLair == null)
		{
			log.warning("Zaken AI failed to load, missing zone for Zaken");
			return;
		}
 
		if (_zakenLair.isInsideZone(npc))
		{
			_ai4 = 1;
			startQuestTimer("1003", 1700, null, null, false);
		}
		_1001 = 1;
		startQuestTimer("1001", 1000, npc, null, false);
	}
 
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		int status = GrandBossManager.getInstance().getBossStatus(ZAKEN);
		if (status == DEAD && !event.equalsIgnoreCase("zaken_unlock"))
		{
			return super.onAdvEvent(event, npc, player);
		}
 
		if (event.equalsIgnoreCase("1001"))
		{
			if (_1001 == 1)
			{
				_1001 = 0;
				cancelQuestTimer("1001", npc, null);
			}
 
			int sk_4223 = 0;
			int sk_4227 = 0;
			L2Effect[] effects = npc.getAllEffects();
			if (effects.length != 0)
			{
				for (L2Effect e : effects)
				{
					if (e.getSkill().getId() == 4227)
					{
						sk_4227 = 1;
					}
 
					if (e.getSkill().getId() == 4223)
					{
						sk_4223 = 1;
					}
				}
			}
 
			if (getTimeHour() < 5)
			{
				if (sk_4223 == 1)
				{
					npc.setTarget(npc);
					npc.doCast(SkillTable.getInstance().getInfo(4224, 1));
					_ai1 = npc.getX();
					_ai2 = npc.getY();
					_ai3 = npc.getZ();
				}
 
				if (sk_4227 == 0)
				{
					npc.setTarget(npc);
					npc.doCast(SkillTable.getInstance().getInfo(4227, 1));
				}
 
				if (npc.getAI().getIntention() == CtrlIntention.ATTACK && _ai0 == 0)
				{
					int i0 = 0;
					int i1 = 1;
					if (((Attackable) npc).getMostHated() != null)
					{
						if ((((((Attackable) npc).getMostHated().getX() - _ai1) * (((Attackable) npc).getMostHated().getX() - _ai1)) + ((((Attackable) npc).getMostHated().getY() - _ai2) * (((Attackable) npc).getMostHated().getY() - _ai2))) > (1500 * 1500))
						{
							i0 = 1;
						}
						else
						{
							i0 = 0;
						}
 
						if (i0 == 0)
						{
							i1 = 0;
						}
 
						if (_quest0 > 0)
						{
							if (c_quest0 == null)
							{
								i0 = 0;
							}
							else if ((((c_quest0.getX() - _ai1) * (c_quest0.getX() - _ai1)) + ((c_quest0.getY() - _ai2) * (c_quest0.getY() - _ai2))) > (1500 * 1500))
							{
								i0 = 1;
							}
							else
							{
								i0 = 0;
							}
 
							if (i0 == 0)
							{
								i1 = 0;
							}
						}
 
						if (_quest0 > 1)
						{
							if (c_quest1 == null)
							{
								i0 = 0;
							}
							else if ((((c_quest1.getX() - _ai1) * (c_quest1.getX() - _ai1)) + ((c_quest1.getY() - _ai2) * (c_quest1.getY() - _ai2))) > (1500 * 1500))
							{
								i0 = 1;
							}
							else
							{
								i0 = 0;
							}
 
							if (i0 == 0)
							{
								i1 = 0;
							}
						}
 
						if (_quest0 > 2)
						{
							if (c_quest2 == null)
							{
								i0 = 0;
							}
							else if ((((c_quest2.getX() - _ai1) * (c_quest2.getX() - _ai1)) + ((c_quest2.getY() - _ai2) * (c_quest2.getY() - _ai2))) > (1500 * 1500))
							{
								i0 = 1;
							}
							else
							{
								i0 = 0;
							}
 
							if (i0 == 0)
							{
								i1 = 0;
							}
						}
 
						if (_quest0 > 3)
						{
							if (c_quest3 == null)
							{
								i0 = 0;
							}
							else if ((((c_quest3.getX() - _ai1) * (c_quest3.getX() - _ai1)) + ((c_quest3.getY() - _ai2) * (c_quest3.getY() - _ai2))) > (1500 * 1500))
							{
								i0 = 1;
							}
							else
							{
								i0 = 0;
							}
 
							if (i0 == 0)
							{
								i1 = 0;
							}
						}
 
						if (_quest0 > 4)
						{
							if (c_quest4 == null)
							{
								i0 = 0;
							}
							else if ((((c_quest4.getX() - _ai1) * (c_quest4.getX() - _ai1)) + ((c_quest4.getY() - _ai2) * (c_quest4.getY() - _ai2))) > (1500 * 1500))
							{
								i0 = 1;
							}
							else
							{
								i0 = 0;
							}
 
							if (i0 == 0)
							{
								i1 = 0;
							}
						}
 
						if (i1 == 1)
						{
							_quest0 = 0;
							int i2 = Rnd.get(15);
							_ai1 = Xcoords[i2] + Rnd.get(650);
							_ai2 = Ycoords[i2] + Rnd.get(650);
							_ai3 = Zcoords[i2];
							npc.setTarget(npc);
							npc.doCast(SkillTable.getInstance().getInfo(4222, 1));
						}
					}
				}
 
				if (Rnd.get(20) < 1 && _ai0 == 0)
				{
					_ai1 = npc.getX();
					_ai2 = npc.getY();
					_ai3 = npc.getZ();
				}
 
				Creature c_ai0 = null;
				if (npc.getAI().getIntention() == CtrlIntention.ATTACK && _quest1 == 0)
				{
					if (((Attackable) npc).getMostHated() != null)
					{
						c_ai0 = ((Attackable) npc).getMostHated();
						_quest1 = 1;
					}
				}
				else if (npc.getAI().getIntention() == CtrlIntention.ATTACK && _quest1 != 0)
				{
				if (((Attackable) npc).getMostHated() != null)
					{
						if (c_ai0 == ((Attackable) npc).getMostHated())
						{
							_quest1 = (_quest1 + 1);
						}
						else
						{
							_quest1 = 1;
							c_ai0 = ((Attackable) npc).getMostHated();
						}
					}
				}
 
				if (npc.getAI().getIntention() == CtrlIntention.IDLE)
				{
					_quest1 = 0;
				}
 
				if (_quest1 > 5)
				{
					((Attackable) npc).stopHating(c_ai0);
					Creature nextTarget = ((Attackable) npc).getMostHated();
					if (nextTarget != null)
					{
						npc.getAI().setIntention(CtrlIntention.ATTACK, nextTarget);
					}
					_quest1 = 0;
				}
			}
			else if (sk_4223 == 0)
			{
				npc.setTarget(npc);
				npc.doCast(SkillTable.getInstance().getInfo(4223, 1));
				_quest2 = 3;
			}
 
			if (sk_4227 == 1)
			{
				npc.setTarget(npc);
				npc.doCast(SkillTable.getInstance().getInfo(4242, 1));
			}
 
			if (Rnd.get(40) < 1)
			{
				int i2 = Rnd.get(15);
				_ai1 = Xcoords[i2] + Rnd.get(650);
				_ai2 = Ycoords[i2] + Rnd.get(650);
				_ai3 = Zcoords[i2];
				npc.setTarget(npc);
				npc.doCast(SkillTable.getInstance().getInfo(4222, 1));
			}
			startQuestTimer("1001", 30000, npc, null, false);
		}
 
		if (event.equalsIgnoreCase("1002"))
		{
			_quest0 = 0;
			npc.doCast(SkillTable.getInstance().getInfo(4222, 1));
			_ai0 = 0;
		}
 
		if (event.equalsIgnoreCase("1003"))
		{
			if (_ai4 == 1)
			{
				int rr = Rnd.get(15);
				addSpawn(pirates_zombie_captain_b, Xcoords[rr] + Rnd.get(650), Ycoords[rr] + Rnd.get(650), Zcoords[rr], Rnd.get(65536), false, 0, true).isRaidMinion();
				_ai4 = 2;
			}
			else if (_ai4 == 2)
			{
				int rr = Rnd.get(15);
				addSpawn(doll_blader_b, Xcoords[rr] + Rnd.get(650), Ycoords[rr] + Rnd.get(650), Zcoords[rr], Rnd.get(65536), false, 0, true).isRaidMinion();
				_ai4 = 3;
			}
			else if (_ai4 == 3)
			{
				addSpawn(vale_master_b, Xcoords[Rnd.get(15)] + Rnd.get(650), Ycoords[Rnd.get(15)] + Rnd.get(650), Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, Xcoords[Rnd.get(15)] + Rnd.get(650), Ycoords[Rnd.get(15)] + Rnd.get(650), Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0, true).isRaidMinion();
				_ai4 = 4;
			}
			else if (_ai4 == 4)
			{
				addSpawn(pirates_zombie_b, Xcoords[Rnd.get(15)] + Rnd.get(650), Ycoords[Rnd.get(15)] + Rnd.get(650), Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, Xcoords[Rnd.get(15)] + Rnd.get(650), Ycoords[Rnd.get(15)] + Rnd.get(650), Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, Xcoords[Rnd.get(15)] + Rnd.get(650), Ycoords[Rnd.get(15)] + Rnd.get(650), Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, Xcoords[Rnd.get(15)] + Rnd.get(650), Ycoords[Rnd.get(15)] + Rnd.get(650), Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, Xcoords[Rnd.get(15)] + Rnd.get(650), Ycoords[Rnd.get(15)] + Rnd.get(650), Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0, true).isRaidMinion();
				_ai4 = 5;
			}
			else if (_ai4 == 5)
			{
				addSpawn(doll_blader_b, 52675, 219371, -3290, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 52687, 219596, -3368, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 52672, 219740, -3418, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 52857, 219992, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 52959, 219997, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 53381, 220151, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 54236, 220948, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 54885, 220144, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 55264, 219860, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 55399, 220263, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 55679, 220129, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 56276, 220783, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 57173, 220234, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 56267, 218826, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 56294, 219482, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 56094, 219113, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 56364, 218967, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 57113, 218079, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 56186, 217153, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 55440, 218081, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 55202, 217940, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 55225, 218236, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 54973, 218075, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 53412, 218077, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 54226, 218797, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 54394, 219067, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 54139, 219253, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 54262, 219480, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				_ai4 = 6;
			}
		else if (_ai4 == 6)
			{
				addSpawn(pirates_zombie_b, 53412, 218077, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 54413, 217132, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 54841, 217132, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 55372, 217128, -3343, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 55893, 217122, -3488, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 56282, 217237, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 56963, 218080, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 56267, 218826, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 56294, 219482, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 56094, 219113, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 56364, 218967, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 56276, 220783, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 57173, 220234, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 54885, 220144, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 55264, 219860, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 55399, 220263, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 55679, 220129, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 54236, 220948, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 54464, 219095, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 54226, 218797, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 54394, 219067, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 54139, 219253, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 54262, 219480, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 53412, 218077, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 55440, 218081, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 55202, 217940, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 55225, 218236, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 54973, 218075, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				_ai4 = 7;
			}
			else if (_ai4 == 7)
			{
				addSpawn(pirates_zombie_b, 54228, 217504, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 54181, 217168, -3216, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 54714, 217123, -3168, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 55298, 217127, -3073, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 55787, 217130, -2993, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 56284, 217216, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 56963, 218080, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 56267, 218826, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 56294, 219482, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 56094, 219113, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 56364, 218967, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 56276, 220783, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 57173, 220234, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 54885, 220144, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 55264, 219860, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 55399, 220263, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 55679, 220129, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 54236, 220948, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 54464, 219095, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 54226, 218797, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(vale_master_b, 54394, 219067, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 54139, 219253, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(doll_blader_b, 54262, 219480, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 53412, 218077, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 54280, 217200, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 55440, 218081, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_captain_b, 55202, 217940, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 55225, 218236, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				addSpawn(pirates_zombie_b, 54973, 218075, -2944, Rnd.get(65536), false, 0, true).isRaidMinion();
				_ai4 = 8;
				cancelQuestTimer("1003", null, null);
			}
		}
		else if (event.equalsIgnoreCase("zaken_unlock"))
		{
			final GrandBoss zaken = (GrandBoss) addSpawn(ZAKEN, 55276, 219176, -3227, 0, false, 0,false);
			GrandBossManager.getInstance().setBossStatus(ZAKEN, ALIVE);
			spawnBoss(zaken);
			if (Config.ANNOUNCE_BOSS_ALIVE)
			{
				Broadcast.announceToOnlinePlayers("Epic Boss: " + zaken.getName() + " spawned in world.");
			}
		}
		else if (event.equalsIgnoreCase("CreateOnePrivateEx"))
		{
			addSpawn(npc.getNpcId(), npc.getX(), npc.getY(), npc.getZ(), 0, false, 0, true).isRaidMinion();
		}
 
		return super.onAdvEvent(event, npc, player);
	}
 
	@Override
	public String onFactionCall(Npc npc, Npc caller, Player attacker, boolean isPet)
	{
		if (caller == null || npc == null)
		{
			return super.onFactionCall(npc, caller, attacker, isPet);
		}
 
		int npcId = npc.getNpcId();
		int callerId = caller.getNpcId();
		if (getTimeHour() < 5 && callerId != ZAKEN && npcId == ZAKEN)
		{
			int damage = 0;
			if (npc.getAI().getIntention() == CtrlIntention.IDLE && _ai0 == 0 && damage < 10 && Rnd.get((30 * 15)) < 1)
			{
				_ai0 = 1;
				_ai1 = caller.getX();
				_ai2 = caller.getY();
				_ai3 = caller.getZ();
				startQuestTimer("1002", 300, caller, null, false);
			}
		}
 
		return super.onFactionCall(npc, caller, attacker, isPet);
	}
 
	@Override
	public String onSpellFinished(Npc npc, Player player, L2Skill skill)
	{
		if (npc.getNpcId() == ZAKEN)
		{
			int skillId = skill.getId();
			if (skillId == 4222)
			{
				npc.teleToLocation(_ai1, _ai2, _ai3, 0);
				npc.getAI().setIntention(CtrlIntention.IDLE);
			}
			else if (skillId == 4216)
			{
				int i1 = Rnd.get(15);
				player.teleToLocation(Xcoords[i1] + Rnd.get(650), Ycoords[i1] + Rnd.get(650), Zcoords[i1], 0);
				((Attackable) npc).stopHating(player);
				Creature nextTarget = ((Attackable) npc).getMostHated();
				if (nextTarget != null)
				{
					npc.getAI().setIntention(CtrlIntention.ATTACK, nextTarget);
				}
			}
			else if (skillId == 4217)
			{
				int i0 = 0;
				int i1 = Rnd.get(15);
				player.teleToLocation(Xcoords[i1] + Rnd.get(650), Ycoords[i1] + Rnd.get(650), Zcoords[i1], 0);
				((Attackable) npc).stopHating(player);
				if (c_quest0 != null && _quest0 > 0 && c_quest0 != player && c_quest0.getZ() > (player.getZ() - 100) && c_quest0.getZ() < (player.getZ() + 100))
				{
					if ((((c_quest0.getX() - player.getX()) * (c_quest0.getX() - player.getX())) + ((c_quest0.getY() - player.getY()) * (c_quest0.getY() - player.getY()))) > (250 * 250))
					{
						i0 = 1;
					}
					else
					{
						i0 = 0;
					}
 
					if (i0 == 0)
					{
						i1 = Rnd.get(15);
						c_quest0.teleToLocation(Xcoords[i1] + Rnd.get(650), Ycoords[i1] + Rnd.get(650), Zcoords[i1], 0);
						((Attackable) npc).stopHating(c_quest0);
					}
				}
 
				if (c_quest1 != null && _quest0 > 1 && c_quest1 != player && c_quest1.getZ() > (player.getZ() - 100) && c_quest1.getZ() < (player.getZ() + 100))
				{
					if ((((c_quest1.getX() - player.getX()) * (c_quest1.getX() - player.getX())) + ((c_quest1.getY() - player.getY()) * (c_quest1.getY() - player.getY()))) > (250 * 250))
					{
						i0 = 1;
					}
					else
					{
						i0 = 0;
					}
 
					if (i0 == 0)
				{
						i1 = Rnd.get(15);
						c_quest1.teleToLocation(Xcoords[i1] + Rnd.get(650), Ycoords[i1] + Rnd.get(650), Zcoords[i1], 0);
						((Attackable) npc).stopHating(c_quest1);
					}
				}
 
				if (c_quest2 != null && _quest0 > 2 && c_quest2 != player && c_quest2.getZ() > (player.getZ() - 100) && c_quest2.getZ() < (player.getZ() + 100))
				{
					if ((((c_quest2.getX() - player.getX()) * (c_quest2.getX() - player.getX())) + ((c_quest2.getY() - player.getY()) * (c_quest2.getY() - player.getY()))) > (250 * 250))
					{
						i0 = 1;
					}
					else
					{
						i0 = 0;
					}
 
					if (i0 == 0)
					{
						i1 = Rnd.get(15);
						c_quest2.teleToLocation(Xcoords[i1] + Rnd.get(650), Ycoords[i1] + Rnd.get(650), Zcoords[i1], 0);
						((Attackable) npc).stopHating(c_quest2);
					}
				}
 
				if (c_quest3 != null && _quest0 > 3 && c_quest3 != player && c_quest3.getZ() > (player.getZ() - 100) && c_quest3.getZ() < (player.getZ() + 100))
				{
					if ((((c_quest3.getX() - player.getX()) * (c_quest3.getX() - player.getX())) + ((c_quest3.getY() - player.getY()) * (c_quest3.getY() - player.getY()))) > (250 * 250))
					{
						i0 = 1;
					}
					else
					{
						i0 = 0;
					}
 
					if (i0 == 0)
					{
					i1 = Rnd.get(15);
						c_quest3.teleToLocation(Xcoords[i1] + Rnd.get(650), Ycoords[i1] + Rnd.get(650), Zcoords[i1], 0);
						((Attackable) npc).stopHating(c_quest3);
					}
				}
 
				if (c_quest4 != null && _quest0 > 4 && c_quest4 != player && c_quest4.getZ() > (player.getZ() - 100) && c_quest4.getZ() < (player.getZ() + 100))
				{
					if ((((c_quest4.getX() - player.getX()) * (c_quest4.getX() - player.getX())) + ((c_quest4.getY() - player.getY()) * (c_quest4.getY() - player.getY()))) > (250 * 250))
					{
						i0 = 1;
					}
					else
					{
						i0 = 0;
					}
 
					if (i0 == 0)
					{
						i1 = Rnd.get(15);
						c_quest4.teleToLocation(Xcoords[i1] + Rnd.get(650), Ycoords[i1] + Rnd.get(650), Zcoords[i1], 0);
						((Attackable) npc).stopHating(c_quest4);
					}
				}
 
				Creature nextTarget = ((Attackable) npc).getMostHated();
				if (nextTarget != null)
				{
					npc.getAI().setIntention(CtrlIntention.ATTACK, nextTarget);
				}
			}
		}
 
		return super.onSpellFinished(npc, player, skill);
	}
 
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isPet, L2Skill skill)
	{
		int npcId = npc.getNpcId();
		if (npcId == ZAKEN)
		{
			if (attacker.getMountType() == 1)
			{
				int sk_4258 = 0;
				L2Effect[] effects = attacker.getAllEffects();
				if (effects.length != 0)
				{
					for (L2Effect e : effects)
					{
						if (e.getSkill().getId() == 4258)
						{
							sk_4258 = 1;
						}
					}
				}
 
				if (sk_4258 == 0)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillTable.getInstance().getInfo(4258, 1));
				}
			}
 
			Creature originalAttacker = isPet ? attacker.getPet() : attacker;
			int hate = (int) (((damage / npc.getMaxHp()) / 0.05) * 20000);
			((Attackable) npc).addDamageHate(originalAttacker, 0, hate);
			if (Rnd.get(10) < 1)
			{
				int i0 = Rnd.get((15 * 15));
				if (i0 < 1)
			{
					npc.setTarget(attacker);
				npc.doCast(SkillTable.getInstance().getInfo(4216, 1));
				}
				else if (i0 < 2)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillTable.getInstance().getInfo(4217, 1));
				}
				else if (i0 < 4)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillTable.getInstance().getInfo(4219, 1));
				}
				else if (i0 < 8)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillTable.getInstance().getInfo(4218, 1));
				}
				else if (i0 < 15)
				{
					for (Creature character : npc.getKnownTypeInRadius(Creature.class, 100))
					{
						if (character != attacker)
						{
							continue;
						}
 
						if (attacker != ((Attackable) npc).getMostHated())
						{
							npc.setTarget(attacker);
							npc.doCast(SkillTable.getInstance().getInfo(4221, 1));
						}
					}
				}
 
				if (Rnd.get(2) < 1)
				{
					if (attacker == ((Attackable) npc).getMostHated())
					{
						npc.setTarget(attacker);
						npc.doCast(SkillTable.getInstance().getInfo(4220, 1));
					}
				}
			}
 
			if (getTimeHour() < 5)
			{
			}
			else if (npc.getCurrentHp() < ((npc.getMaxHp() * _quest2) / 4.0))
			{
				_quest2 = (_quest2 - 1);
				int i2 = Rnd.get(15);
				_ai1 = Xcoords[i2] + Rnd.get(650);
				_ai2 = Ycoords[i2] + Rnd.get(650);
				_ai3 = Zcoords[i2];
				npc.setTarget(npc);
				npc.doCast(SkillTable.getInstance().getInfo(4222, 1));
			}
		}
 
		return super.onAttack(npc, attacker, damage, isPet, skill);
	}
 
	@Override
	public String onKill(Npc npc, Player killer, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if (npcId == ZAKEN)
		{
 
			npc.broadcastPacket(new PlaySound(1, "BS02_D", npc));
			GrandBossManager.getInstance().setBossStatus(ZAKEN, DEAD);
			long respawnTime = (long) Config.SPAWN_INTERVAL_ZAKEN + Rnd.get(-Config.RANDOM_SPAWN_TIME_ZAKEN, Config.RANDOM_SPAWN_TIME_ZAKEN);
			respawnTime *= 3600000;
			startQuestTimer("zaken_unlock", respawnTime, null, null, false);
			cancelQuestTimer("1001", npc, null);
			cancelQuestTimer("1003", npc, null);
			StatsSet info = GrandBossManager.getInstance().getStatsSet(ZAKEN);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatsSet(ZAKEN, info);
 
		}
		else if (GrandBossManager.getInstance().getBossStatus(ZAKEN) == ALIVE)
		{
			if (npcId != ZAKEN)
			{
				startQuestTimer("CreateOnePrivateEx", ((30 + Rnd.get(60)) * 1000), npc, null, false);
			}
		}
 
		return super.onKill(npc, killer, isPet);
	}
 
	@Override
	public String onSkillSee(Npc npc, Player caster, L2Skill skill, WorldObject[] targets, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if (npcId == ZAKEN)
		{
			if (skill.getAggroPoints() > 0)
			{
				((Attackable) npc).addDamageHate(caster, 0, (((skill.getAggroPoints() / npc.getMaxHp()) * 10) * 150));
			}
 
			if (Rnd.get(12) < 1)
			{
				int i0 = Rnd.get((15 * 15));
				if (i0 < 1)
				{
					npc.setTarget(caster);
					npc.doCast(SkillTable.getInstance().getInfo(4216, 1));
			}
				else if (i0 < 2)
				{
					npc.setTarget(caster);
					npc.doCast(SkillTable.getInstance().getInfo(4217, 1));
				}
			else if (i0 < 4)
				{
					npc.setTarget(caster);
					npc.doCast(SkillTable.getInstance().getInfo(4219, 1));
				}
				else if (i0 < 8)
				{
					npc.setTarget(caster);
					npc.doCast(SkillTable.getInstance().getInfo(4218, 1));
				}
				else if (i0 < 15)
				{
					for (Creature character : npc.getKnownTypeInRadius(Creature.class, 100))
					{
						if (character != caster)
						{
							continue;
						}
 
						if (caster != ((Attackable) npc).getMostHated())
						{
							npc.setTarget(caster);
							npc.doCast(SkillTable.getInstance().getInfo(4221, 1));
						}
					}
				}
 
				if (Rnd.get(2) < 1)
				{
					if (caster == ((Attackable) npc).getMostHated())
					{
						npc.setTarget(caster);
						npc.doCast(SkillTable.getInstance().getInfo(4220, 1));
					}
			}
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isPet);
	}
 
	@Override
	public String onAggro(Npc npc, Player player, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if (npcId == ZAKEN)
		{
			if (_zakenLair.isInsideZone(npc))
			{
				Creature target = isPet ? player.getPet() : player;
				((Attackable) npc).addDamageHate(target, 1, 200);
			}
 
			if (player.getZ() > (npc.getZ() - 100) && player.getZ() < (npc.getZ() + 100))
			{
				if (_quest0 < 5 && Rnd.get(3) < 1)
				{
					if (_quest0 == 0)
					{
						c_quest0 = player;
					}
					else if (_quest0 == 1)
					{
						c_quest1 = player;
					}
					else if (_quest0 == 2)
					{
						c_quest2 = player;
					}
					else if (_quest0 == 3)
					{
						c_quest3 = player;
					}
					else if (_quest0 == 4)
					{
						c_quest4 = player;
					}
					_quest0++;
				}
 
				if (Rnd.get(15) < 1)
				{
					int i0 = Rnd.get((15 * 15));
					if (i0 < 1)
					{
						npc.setTarget(player);
						npc.doCast(SkillTable.getInstance().getInfo(4216, 1));
					}
					else if (i0 < 2)
					{
						npc.setTarget(player);
						npc.doCast(SkillTable.getInstance().getInfo(4217, 1));
					}
					else if (i0 < 4)
					{
						npc.setTarget(player);
						npc.doCast(SkillTable.getInstance().getInfo(4219, 1));
					}
					else if (i0 < 8)
					{
						npc.setTarget(player);
						npc.doCast(SkillTable.getInstance().getInfo(4218, 1));
					}
					else if (i0 < 15)
					{
						for (Creature character : npc.getKnownTypeInRadius(Creature.class, 100))
						{
							if (character != player)
							{
								continue;
							}
 
							if (player != ((Attackable) npc).getMostHated())
							{
								npc.setTarget(player);
								npc.doCast(SkillTable.getInstance().getInfo(4221, 1));
							}
						}
					}
 
					if (Rnd.get(2) < 1)
					{
						if (player == ((Attackable) npc).getMostHated())
						{
							npc.setTarget(player);
							npc.doCast(SkillTable.getInstance().getInfo(4220, 1));
						}
					}
			}
			}
		}
 
		return super.onAggro(npc, player, isPet);
	}
 
	public int getTimeHour()
	{
		return (GameTimeTaskManager.getInstance().getGameTime() / 60) % 24;
	}
 
}
