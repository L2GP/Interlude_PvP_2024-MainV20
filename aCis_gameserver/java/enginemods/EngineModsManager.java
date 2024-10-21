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
package enginemods;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import enginemods.data.ConfigData;
import enginemods.data.FakePlayerData;
import enginemods.data.IconData;
import enginemods.data.ModsData;
import enginemods.data.PlayerData;
import enginemods.data.SchemeBuffData;
import enginemods.data.SchemeBuffData2;
import enginemods.data.SchemeBuffData3;
import enginemods.data.SkillData;
import enginemods.engine.AbstractMods;
import enginemods.engine.community.ClanCommunityBoard;
import enginemods.engine.community.FavoriteCommunityBoard;
import enginemods.engine.community.MemoCommunityBoard;
import enginemods.engine.community.RegionComunityBoard;
import enginemods.engine.events.BonusWeekend;
import enginemods.engine.events.Champions;
import enginemods.engine.events.CityElpys;
import enginemods.engine.events.RandomBossSpawn;
import enginemods.engine.mods.AnnounceKillBoss;
import enginemods.engine.mods.AntiBot;
import enginemods.engine.mods.ColorAccordingAmountPvPorPk;
import enginemods.engine.mods.EnchantAbnormalEffectArmor;
import enginemods.engine.mods.FakePlayer;
import enginemods.engine.mods.NewCharacterCreated;
import enginemods.engine.mods.OfflineShop;
import enginemods.engine.mods.PvpReward;
import enginemods.engine.mods.SellBuffs;
import enginemods.engine.mods.SpreeKills;
import enginemods.engine.mods.SubClassAcumulatives;
import enginemods.engine.mods.SystemAio;
import enginemods.engine.mods.SystemVip;
import enginemods.engine.npc.NpcBufferScheme;
import enginemods.engine.npc.NpcBufferScheme2;
import enginemods.engine.npc.NpcBufferScheme3;
import enginemods.engine.npc.NpcClassMaster;
import enginemods.engine.npc.NpcRanking;
import enginemods.engine.npc.NpcTeleporter;
import enginemods.engine.stats.StatsFake;
import enginemods.engine.stats.StatsNpc;
import enginemods.engine.stats.StatsPlayer;
import enginemods.instances.NpcDropsInstance;
import enginemods.instances.NpcExpInstance;
import enginemods.util.Util;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.skills.Stats;

/**
 * @author fissban
 */
public class EngineModsManager
{
	private static final Logger LOG = Logger.getLogger(AbstractMods.class.getName());
	
	private static final Map<Integer, AbstractMods> ENGINES_MODS = new LinkedHashMap<>();
	
	public EngineModsManager()
	{
		//
	}
	
	public static void init() throws IllegalArgumentException, InvocationTargetException, SecurityException, NoSuchMethodException
	{
		//@formatter:off
		LOG.config(Util.SEPARATOR);
		try{    loadCredits();          }catch(Exception e){e.printStackTrace();}
		LOG.config(Util.SEPARATOR);
		try{    ModsData.load();        }catch(Exception e){e.printStackTrace();}
		try{    PlayerData.load();      }catch(Exception e){e.printStackTrace();}
		try{    IconData.load();        }catch(Exception e){e.printStackTrace();}
		try{    SkillData.load();       }catch(Exception e){e.printStackTrace();}
		try{    ConfigData.load();      }catch(Exception e){e.printStackTrace();}
		try{    FakePlayerData.load();  }catch(Exception e){e.printStackTrace();}
		try{    SchemeBuffData.load();  }catch(Exception e){e.printStackTrace();}
		try{    SchemeBuffData2.load();  }catch(Exception e){e.printStackTrace();}
		try{    SchemeBuffData3.load();  }catch(Exception e){e.printStackTrace();}
		LOG.config(Util.SEPARATOR);
		loadModsAndEvents();
		//@formatter:on
	}
	
	private static void loadCredits()
	{
		LOG.config("");
		LOG.config("L2J_EngineMods designed by Fissban");
		LOG.config("    Adpated by Mikado    ");
		LOG.config("");
	}
	
	/**
	 * Cargamos todos los mods y eventos.
	 * @throws SecurityException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws NoSuchMethodException 
	 */
	private static void loadModsAndEvents() throws IllegalArgumentException, InvocationTargetException, SecurityException, NoSuchMethodException
	{
		try
		{
			// mods
			ColorAccordingAmountPvPorPk.class.getDeclaredConstructor().newInstance();
			ColorAccordingAmountPvPorPk.class.getDeclaredConstructor().newInstance();
			EnchantAbnormalEffectArmor.class.getDeclaredConstructor().newInstance();
			SpreeKills.class.getDeclaredConstructor().newInstance();
			SubClassAcumulatives.class.getDeclaredConstructor().newInstance();
			PvpReward.class.getDeclaredConstructor().newInstance();
			AnnounceKillBoss.class.getDeclaredConstructor().newInstance();
			SellBuffs.class.getDeclaredConstructor().newInstance();			
			AntiBot.class.getDeclaredConstructor().newInstance();
			NewCharacterCreated.class.getDeclaredConstructor().newInstance();
			SystemAio.class.getDeclaredConstructor().newInstance();
			SystemVip.class.getDeclaredConstructor().newInstance();
			OfflineShop.class.getDeclaredConstructor().newInstance();
			
			// events
			BonusWeekend.class.getDeclaredConstructor().newInstance();
			Champions.class.getDeclaredConstructor().newInstance();
			RandomBossSpawn.class.getDeclaredConstructor().newInstance();
			CityElpys.class.getDeclaredConstructor().newInstance();
			FakePlayer.class.getDeclaredConstructor().newInstance();
			
			// npc
			NpcRanking.class.getDeclaredConstructor().newInstance();
			NpcClassMaster.class.getDeclaredConstructor().newInstance();
			NpcTeleporter.class.getDeclaredConstructor().newInstance();
			NpcBufferScheme.class.getDeclaredConstructor().newInstance();
			NpcBufferScheme2.class.getDeclaredConstructor().newInstance();
			NpcBufferScheme3.class.getDeclaredConstructor().newInstance();
			
			// community
			RegionComunityBoard.class.getDeclaredConstructor().newInstance();
			//HomeComunityBoard.class.getDeclaredConstructor().newInstance();
			FavoriteCommunityBoard.class.getDeclaredConstructor().newInstance();
			ClanCommunityBoard.class.getDeclaredConstructor().newInstance();
			MemoCommunityBoard.class.getDeclaredConstructor().newInstance();
			
			// stats
			StatsFake.class.getDeclaredConstructor().newInstance();
			StatsPlayer.class.getDeclaredConstructor().newInstance();
			StatsNpc.class.getDeclaredConstructor().newInstance();
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void registerMod(AbstractMods type)
	{
		ENGINES_MODS.put(type.hashCode(), type);
	}
	
	public static Collection<AbstractMods> getAllMods()
	{
		return ENGINES_MODS.values();
	}
	
	public static AbstractMods getMod(int type)
	{
		return ENGINES_MODS.get(type);
	}
	
	/** MISC ---------------------------------------------------------------------------------------------- */
	
	/** LISTENERS ----------------------------------------------------------------------------------------- 
	 * @param player 
	 * @param command 
	 * @return */
	public static synchronized boolean onCommunityBoard(Player player, String command)
	{
		for (AbstractMods mod : ENGINES_MODS.values())
		{
			try
			{
				if (!mod.isStarting())
				{
					continue;
				}
				if (mod.onCommunityBoard(player, command))
				{
					return true;
				}
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		} 
		
		return false;
	}
	
	public static synchronized void onShutDown()
	{
		ENGINES_MODS.values().stream().filter(mod -> mod.isStarting()).forEach(mod ->
		{
			try
			{
				mod.onShutDown();
				mod.endMod();
				mod.cancelScheduledState();
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		});
	}
	
	public static synchronized boolean onExitWorld(Player player)
	{
		boolean exitPlayer = false;
		for (AbstractMods mod : ENGINES_MODS.values())
		{
			try
			{
				if (!mod.isStarting())
				{
					continue;
				}
				if (mod.onExitWorld(player))
				{
					exitPlayer = true;
				}
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		}
		
		return exitPlayer;
	}
	
	public static synchronized boolean onNpcExpSp(Attackable npc, Creature character)
	{
		if (character == null)
		{
			return false;
		}
		
		Player killer = character.getActingPlayer();
		
		if (killer == null)
		{
			return false;
		}
		
		NpcExpInstance instance = new NpcExpInstance();
		
		for (AbstractMods mod : ENGINES_MODS.values())
		{
			try
			{
				if (mod.isStarting())
				{
					mod.onNpcExpSp(killer, npc, instance);
				}
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		}
		
		if (instance.hasSettings())
		{
			instance.init(npc, character);
			return true;
		}
		
		return false;
	}
	
	public static synchronized boolean onNpcDrop(Attackable npc, Creature character)
	{
		if (character == null)
		{
			return false;
		}
		
		Player killer = character.getActingPlayer();
		
		if (killer == null)
		{
			return false;
		}
		
		NpcDropsInstance instance = new NpcDropsInstance();
		
		for (AbstractMods mod : ENGINES_MODS.values())
		{
			try
			{
				if (mod.isStarting())
				{
					mod.onNpcDrop(killer, npc, instance);
				}
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		}
		
		if (instance.hasSettings())
		{
			instance.init(npc, character);
			return true;
		}
		
		return false;
	}
	
	public static synchronized void onEnterZone(Creature player, L2ZoneType zone)
	{
		ENGINES_MODS.values().stream().filter(mod -> mod.isStarting()).forEach(mod ->
		{
			try
			{
				mod.onEnterZone(player, zone);
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		});
	}
	
	public static synchronized void onExitZone(Creature player, L2ZoneType zone)
	{
		ENGINES_MODS.values().stream().filter(mod -> mod.isStarting()).forEach(mod ->
		{
			try
			{
				mod.onExitZone(player, zone);
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		});
	}
	
	public static synchronized void onCreateCharacter(Player player)
	{
		ENGINES_MODS.values().stream().filter(mod -> mod.isStarting()).forEach(mod ->
		{
			try
			{
				mod.onCreateCharacter(player);
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		});
	}
	
	public static synchronized boolean onVoiced(Player player, String chat)
	{
		for (AbstractMods mod : ENGINES_MODS.values())
		{
			if (!mod.isStarting())
			{
				continue;
			}
			
			try
			{
				if (chat.startsWith("admin_"))
				{
					if (player.getAccessLevel().getLevel() < 1)
					{
						return false;
					}
					
					if (mod.onAdminCommand(player, chat.replace("admin_", "")))
					{
						return true;
					}
				}
				else if (chat.startsWith("."))
				{
					if (mod.onVoicedCommand(player, chat.replace(".", "")))
					{
						return true;
					}
				}
				else
				{
					if (mod.onChat(player, chat))
					{
						return true;
					}
				}
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
	public static synchronized boolean onInteract(Player player, Creature character)
	{
		for (AbstractMods mod : ENGINES_MODS.values())
		{
			if (!mod.isStarting())
			{
				continue;
			}
			
			try
			{
				if (!mod.onInteract(player, character))
				{
					continue;
				}
				return true;
				
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
	/**
	 * Todos los bypass tienen que tener el formato "bypass -h Engine modName bypassName", pero al mod solo llegara "bypassName".
	 * @param player	 
	 * @param command
	 */
	
	public static synchronized void onEvent(Player player, String command)
	{
		ENGINES_MODS.values().stream().filter(mod -> command.startsWith(mod.getClass().getSimpleName()) && mod.isStarting()).forEach(mod ->
		{
			WorldObject obj = player.getTarget();
			
			if (!(obj instanceof Creature))
			{
				return;
			}
			
			if (!player.isInsideRadius(obj, Npc.INTERACTION_DISTANCE, false, false))
			{
				return;
			}
			
			try
			{
				mod.onEvent(player, (Creature) obj, command.replace(mod.getClass().getSimpleName() + " ", ""));
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		});
	}
	
	public static synchronized String onSeeNpcTitle(int objectId)
	{
		String title = null;
		
		for (AbstractMods mod : ENGINES_MODS.values())
		{
			if (!mod.isStarting())
			{
				continue;
			}
			try
			{
				String aux = mod.onSeeNpcTitle(objectId);
				if (aux != null)
				{
					title = aux;
				}
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		}
		
		return title;
	}
	
	public static synchronized void onSpawn(Npc obj)
	{
		ENGINES_MODS.values().stream().filter(mod -> mod.isStarting()).forEach(mod ->
		{
			try
			{
				mod.onSpawn(obj);
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		});
	}
	
	public static synchronized void onEnterWorld(Player player)
	{
		ENGINES_MODS.values().stream().filter(mod -> mod.isStarting()).forEach(mod ->
		{
			try
			{
				mod.onEnterWorld(player);
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		});
	}
	
	public static synchronized void onKill(Creature killer, Creature victim, boolean isPet)
	{
		ENGINES_MODS.values().stream().filter(mod -> mod.isStarting()).forEach(mod ->
		{
			try
			{
				mod.onKill(killer, victim, isPet);
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		});
	}
	
	public static synchronized void onDeath(Creature player)
	{
		try
		{
			ENGINES_MODS.values().stream().filter(mod -> mod.isStarting()).forEach(mod -> mod.onDeath(player));
		}
		catch (Exception e)
		{
			LOG.log(Level.SEVERE, e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static synchronized void onEnchant(Creature player)
	{
		ENGINES_MODS.values().stream().filter(mod -> mod.isStarting()).forEach(mod ->
		{
			try
			{
				mod.onEnchant(player);
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		});
	}
	
	public static synchronized void onEquip(Creature player)
	{
		ENGINES_MODS.values().stream().filter(mod -> mod.isStarting()).forEach(mod ->
		{
			try
			{
				mod.onEquip(player);
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		});
	}
	
	public static synchronized void onUnequip(Creature player)
	{
		ENGINES_MODS.values().stream().filter(mod -> mod.isStarting()).forEach(mod ->
		{
			try
			{
				mod.onUnequip(player);
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		});
	}
	
	public static synchronized boolean onRestoreSkills(Player player)
	{
		ENGINES_MODS.values().stream().filter(mod -> mod.isStarting()).forEach(mod ->
		{
			try
			{
				mod.onRestoreSkills(player);
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		});
		
		return false;
	}
	
	public static synchronized double onStats(Stats stat, Creature character, double value)
	{
		for (AbstractMods mod : ENGINES_MODS.values())
		{
			if (!mod.isStarting())
			{
				continue;
			}
			
			if (character == null)
			{
				continue;
			}
			
			try
			{
				value += mod.onStats(stat, character, value) - value;
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}
		}
		
		return value;
	}
}
