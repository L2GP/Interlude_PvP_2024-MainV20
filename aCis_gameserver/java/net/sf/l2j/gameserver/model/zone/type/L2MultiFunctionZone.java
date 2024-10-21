package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.L2SpawnZone;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;


public class L2MultiFunctionZone extends L2SpawnZone
{	
	public L2MultiFunctionZone(int id)
	{
		super(id);		
	}
		
	L2Skill noblesse = SkillTable.getInstance().getInfo(1323, 1);
	
	static String[] gradeNames =
	{
		"",
		"D",
		"C",
		"B",
		"A",
		"S"
	};
	
	@Override
	protected void onEnter(Creature character)
	{
		if (character instanceof Player) {
			((Player) character).sendPacket(SystemMessageId.ENTERED_COMBAT_ZONE);
			
			Player activeChar = ((Player) character);
			if (Config.CLASSES != null && Config.CLASSES.contains(""+activeChar.getClassId().getId()))
			{				
				activeChar.teleToLocation(new Location(83597, 147888, -3405), 0);
				activeChar.sendMessage("Your class is not allowed in the MultiFunction zone.");
				return;
			}
			
			for (ItemInstance o : activeChar.getInventory().getItems())
			{
				if (o.isEquipable() && o.isEquipped() && !checkItem(o))
				{
					int slot = activeChar.getInventory().getSlotFromItem(o);
					activeChar.getInventory().unEquipItemInBodySlotAndRecord(slot);
					activeChar.sendMessage(o.getItemName()+ " unequiped because is not allowed inside this zone.");
				}
			}
			
			if (Config.GIVE_NOBLES)
				noblesse.getEffects(activeChar, activeChar);
			if (Config.PVP_ENABLED)
				activeChar.updatePvPFlag(1);
			
			activeChar.mikadoPlayerUpdate();
			activeChar.sendMessage("You entered in a MultiFunction zone.");
			clear(activeChar);
		}
		
//		character.setInsideZone(ZoneId.PVP, true);
		character.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true);
		character.setInsideZone(ZoneId.MULTI_FUNCTION, true);
	}
	
	@Override
	protected void onExit(Creature character)
	{
//		character.setInsideZone(ZoneId.PVP, false);
		character.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
		character.setInsideZone(ZoneId.MULTI_FUNCTION, false);
		
		if (character instanceof Player) {
			((Player) character).sendPacket(SystemMessageId.LEFT_COMBAT_ZONE);
			
			Player activeChar = ((Player) character);
			activeChar.sendMessage("You left from a MultiFunction zone.");
			
			if (Config.PVP_ENABLED)
				activeChar.updatePvPFlag(0);
	
		}
	}
	
	@Override
	public void onDieInside(final Creature character)
	{
		if (character instanceof Player)
		{
			final Player activeChar = ((Player) character);
			if (Config.REVIVE)
			{
				ThreadPool.schedule(new Runnable()
				{
					@Override
					public void run()
					{
						activeChar.doRevive();
						heal(activeChar);
						int[] loc = Config.SPAWN_LOC[Rnd.get(Config.SPAWN_LOC.length)];
						activeChar.teleToLocation(new Location(loc[0]+Rnd.get(-Config.RADIUS,Config.RADIUS), loc[1]+Rnd.get(-Config.RADIUS,Config.RADIUS), loc[2]),0);
					}
				},Config.REVIVE_DELAY*1000);
			}
		}
	}
	
	@Override
	public void onReviveInside(Creature character)
	{
		if (character instanceof Player)
		{
			Player activeChar = ((Player) character);
			if (Config.REVIVE_NOBLES)
				noblesse.getEffects(activeChar, activeChar);
			if (Config.REVIVE_HEAL)
				heal(activeChar);
		}
	}
	
	static void heal(Player activeChar)
	{
		activeChar.setCurrentHp(activeChar.getMaxHp());
		activeChar.setCurrentCp(activeChar.getMaxCp());
		activeChar.setCurrentMp(activeChar.getMaxMp());
	}
	
	private static void clear (Player player)
	{
		if (Config.REMOVE_BUFFS)
		{
			player.stopAllEffects();
			
			if (Config.REMOVE_PETS)
			{
				Summon pet = player.getPet();
				if (pet!= null)
				{
					pet.stopAllEffects();
					pet.unSummon(player);
				}
			}
		}
		else
		{
			if (Config.REMOVE_PETS)
			{
				Summon pet = player.getPet();
				if (pet!= null)
					pet.unSummon(player);
			}
		}
	}
	
	public static void givereward(Player player)
	{
		if (player.isInsideZone(ZoneId.MULTI_FUNCTION)) // Was checked on class Player
		{
			for (int reward : Config.REWARDS.keySet())
			{	
				player.addItem("PvP Zone", reward, Config.REWARDS.get(reward), null, true);					
			}
		}		
		player.mikadoPlayerUpdate();
	}
	
	public static boolean checkItem (ItemInstance item)
	{
		int o = item.getItem().getCrystalType().getId(); // .getCrystalType();
		int e = item.getEnchantLevel();
		
		if (Config.ENCHANT != 0 && e >= Config.ENCHANT)
			return false;
		
		if (Config.GRADES.contains(gradeNames[o]))
			return false;
		
		if (Config.ITEMS != null && Config.ITEMS.contains(""+item.getItemId()))
			return false;
		
		return true;
	}
}