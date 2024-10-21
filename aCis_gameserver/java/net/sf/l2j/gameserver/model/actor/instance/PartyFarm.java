package net.sf.l2j.gameserver.model.actor.instance;

import java.util.List;
import java.util.Map.Entry;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.MinionList;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;



/**
 * A monster extends {@link Attackable} class.<br>
 * <br>
 * It is an attackable {@link Creature}, with the capability to hold minions/master.
 */
public class PartyFarm extends Monster
{
	private PartyFarm _master;
	private MinionList _minionList;
	
	/**
	 * Constructor of L2MonsterInstance (use Creature and L2NpcInstance constructor).
	 * <ul>
	 * <li>Call the Creature constructor to set the _template of the L2MonsterInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR)</li>
	 * <li>Set the name of the L2MonsterInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it</li>
	 * </ul>
	 * @param objectId Identifier of the object to initialized
	 * @param template L2NpcTemplate to apply to the NPC
	 */
	public PartyFarm(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		// FIXME: to test to allow monsters hit others monsters
		if (attacker instanceof PartyFarm)
			return false;
		
		return true;
	}
	
	@Override
	public boolean isAggressive()
	{
		return getTemplate().getAggroRange() > 0;
	}
	
	@Override
	public void onSpawn()
	{
		if (!isTeleporting())
		{
			if (_master != null)
			{
				setIsNoRndWalk(true);
				setIsRaidMinion(_master.isRaid());
				_master.getMinionList().onMinionSpawn(this);
			}
			// delete spawned minions before dynamic minions spawned by script
			else if (_minionList != null)
				getMinionList().deleteSpawnedMinions();
			
			startMaintenanceTask();
		}
		
		// dynamic script-based minions spawned here, after all preparations.
		super.onSpawn();
	}
	
	@Override
	public void onTeleported()
	{
		super.onTeleported();
		
		if (_minionList != null)
			getMinionList().onMasterTeleported();
	}
	
	/**
	 * Spawn minions.
	 */
	@Override
	protected void startMaintenanceTask()
	{
		if (!getTemplate().getMinionData().isEmpty())
			getMinionList().spawnMinions();
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
			return false;
		
		if (_master != null)
			_master.getMinionList().onMinionDie(this, _master.getSpawn().getRespawnDelay() * 1000 / 2);
		
		return true;
	}
	
	@Override
	public void deleteMe()
	{
		if (_minionList != null)
			getMinionList().onMasterDie(true);
		else if (_master != null)
			_master.getMinionList().onMinionDie(this, 0);
		
		super.deleteMe();
	}
	
	@Override
	public PartyFarm getLeader()
	{
		return _master;
	}
	
	public void setLeader(PartyFarm leader)
	{
		_master = leader;
	}
	
	@Override
	public boolean hasMinions()
	{
		return _minionList != null;
	}
	
	@Override
	public MinionList getMinionList()
	{
		if (_minionList == null)
			_minionList = new MinionList(this);
		
		return _minionList;
	}
	
	@Override
	public void doItemDrop(NpcTemplate npcTemplate, Creature mainDamageDealer)
	{
		if (mainDamageDealer == null)
			return;
		
		// Don't drop anything if the last attacker or owner isn't Player
		Player player = mainDamageDealer.getActingPlayer();
		if (player == null)
			return;
						
		if (Config.ENABLE_DROP_PARTYFARM)
		{		
			if(player.isInParty()) {
				for (Entry<Integer, List<Integer>> entry : Config.PARTY_DROP_LIST.entrySet())
				{
					int rewardItem = Rnd.get(entry.getValue().get(1), entry.getValue().get(2));
					int dropChance = entry.getValue().get(0);
										
					if (Rnd.get(100) < dropChance)
					{
						final IntIntHolder item = new IntIntHolder(entry.getKey(), rewardItem);
						if (Config.AUTO_LOOT) {														
							for (Player p : player.getParty().getMembers())
							{
								if (p.isInsideRadius(player, 9000, false, false))
								{
									p.addItem("dropCustom", item.getId(), item.getValue(), this, true);									
								}
							}							
						}	//player.addItem("dropCustom", item.getId(), item.getValue(), this, true);
						else {
							
							for (Player p : player.getParty().getMembers())
							{
								if (p.isInsideRadius(player, 9000, false, false))
								{
									dropItem(p, item);
								}
							}
							//dropItem(player, item);							
						}														
					}
				}
			}
			else {
			player.sendMessage("You are not in a party! or Party Farm is disabled."); }
		}
		
		player.sendMessage("Party Farm event is not active. You donn't get any drop !!");
	}
}