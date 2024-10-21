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
package enginemods.engine.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import enginemods.data.ConfigData;
import enginemods.engine.AbstractMods;
import enginemods.enums.ExpSpType;
import enginemods.enums.ItemDropType;
import enginemods.enums.TeamType;
import enginemods.holders.RewardHolder;
import enginemods.instances.NpcDropsInstance;
import enginemods.instances.NpcExpInstance;
import enginemods.util.Util;
import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.skills.Stats;

/**
 * @author fissban
 */
public class Champions extends AbstractMods
{
	private enum ChampionType
	{
		WEAK_CHAMPION,
		SUPER_CHAMPION,
		HARD_CHAMPION,
	}
	
	private class ChampionInfoHolder
	{
		public ChampionType type;
		public int chanceToSpawn;
		public Map<Stats, Double> allStats = new HashMap<>();
		public List<RewardHolder> rewards = new ArrayList<>();
	}
	
	// champions info
	private static final Map<ChampionType, ChampionInfoHolder> CHAMPIONS_INFO_STATS = new HashMap<>(3);
	{
		ChampionInfoHolder cih = null;
		
		cih = new ChampionInfoHolder();
		cih.type = ChampionType.WEAK_CHAMPION;
		cih.chanceToSpawn = ConfigData.CHANCE_SPAWN_WEAK;
		cih.allStats.putAll(ConfigData.CHAMPION_STAT_WEAK);
		cih.rewards.addAll(ConfigData.CHAMPION_REWARD_WEAK);
		CHAMPIONS_INFO_STATS.put(ChampionType.WEAK_CHAMPION, cih);
		
		cih = new ChampionInfoHolder();
		cih.type = ChampionType.SUPER_CHAMPION;
		cih.chanceToSpawn = ConfigData.CHANCE_SPAWN_SUPER;
		cih.allStats.putAll(ConfigData.CHAMPION_STAT_SUPER);
		cih.rewards.addAll(ConfigData.CHAMPION_REWARD_SUPER);
		CHAMPIONS_INFO_STATS.put(ChampionType.SUPER_CHAMPION, cih);
		
		cih = new ChampionInfoHolder();
		cih.type = ChampionType.HARD_CHAMPION;
		cih.chanceToSpawn = ConfigData.CHANCE_SPAWN_HARD;
		cih.allStats.putAll(ConfigData.CHAMPION_STAT_HARD);
		cih.rewards.addAll(ConfigData.CHAMPION_REWARD_HARD);
		CHAMPIONS_INFO_STATS.put(ChampionType.HARD_CHAMPION, cih);
	}
	
	// champions
	private static final Map<Integer, ChampionInfoHolder> _champions = new ConcurrentHashMap<>();
	
	public Champions()
	{
		registerMod(ConfigData.ENABLE_Champions, ConfigData.CHAMPION_ENABLE_DAY);
	}
	
	@SuppressWarnings("unlikely-arg-type")
	@Override
	public void onModState()
	{
		switch (getState())
		{
			case START:
				//
				break;
			case END:
				for (int objId : _champions.keySet())
				{
					if (World.getInstance().getObjects().contains(objId))
					{
						Npc npc = (Npc) World.getInstance().getObject(objId);
						if (npc != null)
						{
							// volvemos el npc a su estado estado original (sin team)
							npc.setTeam(TeamType.NONE.ordinal());
							// borramos el spawn y dejamos que vuelva a hacer su spawn normal
							npc.deleteMe();
						}
					}
				}
				// limpiamos la lista de champions
				_champions.clear();
				break;
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (!checkNpcType(npc))
		{
			return;
		}
		
		for (ChampionInfoHolder info : CHAMPIONS_INFO_STATS.values())
		{
			if (Rnd.get(100) < info.chanceToSpawn)
			{
				_champions.put(npc.getObjectId(), info);
				
				// definimos un efecto custom para diferencialos
				npc.setTeam(TeamType.RED.ordinal());
				// lo curamos completamente
				npc.setCurrentHpMp(npc.getMaxHp() * info.allStats.get(Stats.MAX_HP), npc.getMaxMp() * info.allStats.get(Stats.MAX_MP));
				return;
			}
		}
	}
	
	@Override
	public void onKill(Creature killer, Creature victim, boolean isPet)
	{
		if (_champions.containsKey(victim.getObjectId()))
		{
			for (RewardHolder reward : _champions.get(victim.getObjectId()).rewards)
			{
				if (Rnd.get(100) <= reward.getRewardChance())
				{
					if (victim.isRaid() && Config.AUTO_LOOT_RAID || !victim.isRaid() && Config.AUTO_LOOT)
					{
						killer.getActingPlayer().doAutoLoot((Attackable) victim, new IntIntHolder(reward.getRewardId(), reward.getRewardCount()));
					}
					else
					{
						((Attackable) victim).dropItem((Player) killer, new IntIntHolder(reward.getRewardId(), reward.getRewardCount()));
					}
				}
			}
			
			_champions.remove(victim.getObjectId());
			victim.setTeam(TeamType.NONE.ordinal());
		}
	}
	
	@Override
	public String onSeeNpcTitle(int objectId)
	{
		if (_champions.containsKey(objectId))
		{
			return _champions.get(objectId).type.name().replace("_", " ");
		}
		
		return null;
	}
	
	@Override
	public double onStats(Stats stat, Creature character, double value)
	{
		if (_champions.containsKey(character.getObjectId()))
		{
			ChampionInfoHolder cih = _champions.get(character.getObjectId());
			
			if (cih.allStats.containsKey(stat))
			{
				return value *= cih.allStats.get(stat);
			}
		}
		return value;
	}
	
	@Override
	public void onNpcExpSp(Player killer, Attackable npc, NpcExpInstance instance)
	{
	    if (_champions.containsKey(npc.getObjectId()))
	    {
	        ChampionInfoHolder cih = _champions.get(npc.getObjectId());
	        
	        // Aquí aplicamos los bonus dependiendo del tipo de campeón
	        switch (cih.type)
	        {
	            case WEAK_CHAMPION:
	                instance.increaseRate(ExpSpType.EXP, ConfigData.CHAMPION_WEAK_BONUS_RATE_EXP);
	                instance.increaseRate(ExpSpType.SP, ConfigData.CHAMPION_WEAK_BONUS_RATE_SP);
	                break;
	            case SUPER_CHAMPION:
	                instance.increaseRate(ExpSpType.EXP, ConfigData.CHAMPION_SUPER_BONUS_RATE_EXP);
	                instance.increaseRate(ExpSpType.SP, ConfigData.CHAMPION_SUPER_BONUS_RATE_SP);
	                break;
	            case HARD_CHAMPION:
	                instance.increaseRate(ExpSpType.EXP, ConfigData.CHAMPION_HARD_BONUS_RATE_EXP);
	                instance.increaseRate(ExpSpType.SP, ConfigData.CHAMPION_HARD_BONUS_RATE_SP);
	                break;
	        }
	    }
	}
	
	@Override
	public void onNpcDrop(Player killer, Attackable npc, NpcDropsInstance instance)
	{
	    // DropBonusHolder (dropType, amountBonus, chanceBonus)
	    // Example: 110 -> 110%
	    // if you use 100% drop will be normal, to earn bonus use values greater than 100%.
	    
	    if (_champions.containsKey(npc.getObjectId()))
	    {
	        ChampionInfoHolder cih = _champions.get(npc.getObjectId());
	        
	        // Aplicar bonificaciones de drop según el tipo de campeón
	        switch (cih.type)
	        {
	            case WEAK_CHAMPION:
	                // Aumentar drop para Weak Champion
	                instance.increaseDrop(ItemDropType.NORMAL, ConfigData.CHAMPION_WEAK_BONUS_DROP, ConfigData.CHAMPION_WEAK_BONUS_DROP);
	                instance.increaseDrop(ItemDropType.SPOIL, ConfigData.CHAMPION_WEAK_BONUS_SPOIL, ConfigData.CHAMPION_WEAK_BONUS_SPOIL);
	                instance.increaseDrop(ItemDropType.NORMAL, ConfigData.CHAMPION_WEAK_SEAL_STONES,ConfigData.CHAMPION_WEAK_SEAL_STONES+ Config.RATE_DROP_SEAL_STONES);
	                break;
	                
	            case SUPER_CHAMPION:
	                // Aumentar drop para Super Champion
	                instance.increaseDrop(ItemDropType.NORMAL, ConfigData.CHAMPION_SUPER_BONUS_DROP, ConfigData.CHAMPION_SUPER_BONUS_DROP);
	                instance.increaseDrop(ItemDropType.SPOIL, ConfigData.CHAMPION_SUPER_BONUS_SPOIL, ConfigData.CHAMPION_SUPER_BONUS_SPOIL);
	                instance.increaseDrop(ItemDropType.NORMAL, ConfigData.CHAMPION_SUPER_SEAL_STONES,ConfigData.CHAMPION_SUPER_SEAL_STONES+ Config.RATE_DROP_SEAL_STONES);
	                break;
	                
	            case HARD_CHAMPION:
	                // Aumentar drop para Hard Champion
	                instance.increaseDrop(ItemDropType.NORMAL, ConfigData.CHAMPION_HARD_BONUS_DROP, ConfigData.CHAMPION_HARD_BONUS_DROP);
	                instance.increaseDrop(ItemDropType.SPOIL, ConfigData.CHAMPION_HARD_BONUS_SPOIL, ConfigData.CHAMPION_HARD_BONUS_SPOIL);
	                instance.increaseDrop(ItemDropType.NORMAL, ConfigData.CHAMPION_HARD_SEAL_STONES,ConfigData.CHAMPION_HARD_SEAL_STONES+ Config.RATE_DROP_SEAL_STONES);
	                break;
	        }
	        
	        // Bonificaciones comunes para todos los tipos de campeones
	        instance.increaseDrop(ItemDropType.HERB, ConfigData.CHAMPION_BONUS_HERB, ConfigData.CHAMPION_BONUS_HERB);
	        instance.increaseDrop(ItemDropType.SEED, ConfigData.CHAMPION_BONUS_SEED, ConfigData.CHAMPION_BONUS_SEED);
	        
	     
	    }
	}

	
	/**
	 * Se chequea el tipo de npc habilitados para ser "Champion".
	 * <li>L2RaidBossInstance -> NO!
	 * <li>L2GrandBossInstance -> NO!
	 * <li>L2MonsterInstance -> SI!
	 * @param obj
	 * @return
	 */
	private static boolean checkNpcType(WorldObject obj)
	{
		if (Util.areObjectType(RaidBoss.class, obj))
		{
			return false;
		}
		
		if (Util.areObjectType(GrandBoss.class, obj))
		{
			return false;
		}
		
		if (Util.areObjectType(Monster.class, obj))
		{
			return true;
		}
		
		return false;
	}
}
