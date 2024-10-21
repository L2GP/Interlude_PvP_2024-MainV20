package net.sf.l2j.gameserver.model.actor.instance;

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.RaidBossPointsManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager.StatusEnum;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class manages all RaidBoss. In a group mob, there are one master called RaidBoss and several slaves called Minions.
 */
public class RaidBoss extends Monster
{
	private StatusEnum _raidStatus;
	private ScheduledFuture<?> _maintenanceTask;
	
	/**
	 * Constructor of L2RaidBossInstance (use Creature and L2NpcInstance constructor).
	 * <ul>
	 * <li>Call the Creature constructor to set the _template of the L2RaidBossInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR)</li>
	 * <li>Set the name of the L2RaidBossInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it</li>
	 * </ul>
	 * @param objectId Identifier of the object to initialized
	 * @param template L2NpcTemplate to apply to the NPC
	 */
	public RaidBoss(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setIsRaid(true);
	}
	
	@Override
	public void onSpawn()
	{
		setIsNoRndWalk(true);
		super.onSpawn();
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
			return false;
		
		if (_maintenanceTask != null)
		{
			_maintenanceTask.cancel(false);
			_maintenanceTask = null;
		}
		
		if (killer != null)
		{
			final Player player = killer.getActingPlayer();
			if (player != null)
			{
				broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.RAID_WAS_SUCCESSFUL));
				broadcastPacket(new PlaySound("systemmsg_e.1209"));
				
				 // Comprobar si se permite el estatus de Noble automático
	            if (Config.ALLOW_AUTO_NOBLESS_FROM_BOSS && getNpcId() == Config.BOSS_ID) {
	                if (player.getParty() != null) {
	                    for (Player member : player.getParty().getMembers()) {
	                        if (member.isNoble()) {
	                            member.sendMessage("Your party gained nobless status for defeating " + getName() + "!");
	                        } else if (member.isInsideRadius(getX(), getY(), getZ(), Config.RADIUS_TO_RAID, false, false)) {
	                            member.setNoble(true, true);
	                            member.sendMessage("Your party gained nobless status for defeating " + getName() + "!");

	                            // Enviar mensaje HTML cuando el miembro se convierte en Noble
	                            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
	                            html.setHtml("<html><body><title>Congratulations</title><br><center><img src=l2font-e.replay_logo-e width=255 height=60><br><br><br><br>" +
	                                         "<font color=\"LEVEL\">Congratulations,</font><br><br>You acquired all<br1>status from a Noblesse...<br><br>" +
	                                         "<table><tr><td><img src=icon.skill1323 width=32 height=32></td><td width=5></td>" +
	                                         "<td><img src=icon.skill1324 width=32 height=32></td><td width=5></td>" +
	                                         "<td><img src=icon.skill1325 width=32 height=32></td><td width=5></td>" +
	                                         "<td><img src=icon.skill1326 width=32 height=32></td></tr>" +
	                                         "<tr><td><img src=icon.skill1327 width=32 height=32></td><td width=5></td>" +
	                                         "<td><img src=icon.skill0325 width=32 height=32></td><td width=5></td>" +
	                                         "<td><img src=icon.skill0326 width=32 height=32></td><td width=5></td>" +
	                                         "<td><img src=icon.skill0327 width=32 height=32></td></tr></table></center></body></html>");
	                            member.sendPacket(html); // Envía el HTML al miembro
	                        } else {
	                            member.sendMessage("Your party killed " + getName() + "! But you were too far away and earned nothing...");
	                        }
	                    }
	                } else if (!player.isNoble()) {
	                    player.setNoble(true, true);
	                    player.sendMessage("You gained nobless status for defeating " + getName() + "!");

	                    // Enviar mensaje HTML al jugador
	                    NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
	                    html.setHtml("<html><body><title>Congratulations</title><br><center><img src=l2font-e.replay_logo-e width=255 height=60><br><br><br><br>" +
	                                 "<font color=\"LEVEL\">Congratulations,</font><br><br>You acquired all<br1>status from a Noblesse...<br><br>" +
	                                 "<table><tr><td><img src=icon.skill1323 width=32 height=32></td><td width=5></td>" +
	                                 "<td><img src=icon.skill1324 width=32 height=32></td><td width=5></td>" +
	                                 "<td><img src=icon.skill1325 width=32 height=32></td><td width=5></td>" +
	                                 "<td><img src=icon.skill1326 width=32 height=32></td></tr>" +
	                                 "<tr><td><img src=icon.skill1327 width=32 height=32></td><td width=5></td>" +
	                                 "<td><img src=icon.skill0325 width=32 height=32></td><td width=5></td>" +
	                                 "<td><img src=icon.skill0326 width=32 height=32></td><td width=5></td>" +
	                                 "<td><img src=icon.skill0327 width=32 height=32></td></tr></table></center></body></html>");
	                    player.sendPacket(html); // Envía el HTML al jugador
	                }
	            }

	            if (!getSpawn().is_customBossInstance()) {
	                RaidBossSpawnManager.getInstance().updateStatus(this, true);
	            }
	            
				final Party party = player.getParty();
				if (party != null)
				{
					for (Player member : party.getMembers())
					{
						RaidBossPointsManager.getInstance().addPoints(member, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));
						if (member.isNoble())
							Hero.getInstance().setRBkilled(member.getObjectId(), getNpcId());
					}
				}
				else
				{
					RaidBossPointsManager.getInstance().addPoints(player, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));
					if (player.isNoble())
						Hero.getInstance().setRBkilled(player.getObjectId(), getNpcId());
				}
			}
		}
		
		RaidBossSpawnManager.getInstance().updateStatus(this, true);
		return true;
	}
	
	@Override
	public void deleteMe()
	{
		if (_maintenanceTask != null)
		{
			_maintenanceTask.cancel(false);
			_maintenanceTask = null;
		}
		
		super.deleteMe();
	}
	
	/**
	 * Spawn minions.<br>
	 * Also if boss is too far from home location at the time of this check, teleport it to home.
	 */
	@Override
	protected void startMaintenanceTask()
	{
		super.startMaintenanceTask();
		
		_maintenanceTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			// If the boss is dead, movement disabled, is Gordon or is in combat, return.
			if (isDead() || isMovementDisabled() || getNpcId() == 29095 || isInCombat())
				return;
			
			// Spawn must exist.
			final L2Spawn spawn = getSpawn();
			if (spawn == null)
				return;
			
			// If the boss is above drift range (or 200 minimum), teleport him on his spawn.
			if (!isInsideRadius(spawn.getLocX(), spawn.getLocY(), spawn.getLocZ(), Math.max(Config.MAX_DRIFT_RANGE, 200), true, false))
				teleToLocation(spawn.getLoc(), 0);
		}, 60000, 30000);
	}
	
	public StatusEnum getRaidStatus()
	{
		return _raidStatus;
	}
	
	public void setRaidStatus(StatusEnum status)
	{
		_raidStatus = status;
	}
}