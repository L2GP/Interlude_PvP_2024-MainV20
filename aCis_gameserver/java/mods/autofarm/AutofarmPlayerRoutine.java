package mods.autofarm;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedAutoFarm;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.WorldRegion;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.ai.CtrlEvent;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.ai.NextAction;
import net.sf.l2j.gameserver.model.actor.instance.Chest;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;


public class AutofarmPlayerRoutine
{
	private final Player player;
	private ScheduledFuture<?> _task;
	private Creature committedTarget = null;

	public AutofarmPlayerRoutine(Player player)
	{
		this.player = player;
	}

	public void start()
	{
		if (_task == null)
		{
			_task = ThreadPool.scheduleAtFixedRate(() -> executeRoutine(), 450, 450);
			
			player.sendPacket(new ExShowScreenMessage("Auto Farming Actived...", 5*1000, SMPOS.TOP_CENTER, false));
			player.sendPacket(new SystemMessage(SystemMessageId.AUTO_FARM_ACTIVATED));
		    
		
		}
	}
	
	public void stop()
	{
		if (_task != null)
		{
			_task.cancel(false);
			_task = null;
			
			player.sendPacket(new ExShowScreenMessage("Auto Farming Deactivated...", 5*1000, SMPOS.TOP_CENTER, false));
			player.sendPacket(new SystemMessage(SystemMessageId.AUTO_FARM_DESACTIVATED));
		   
		}
	}
	
	public void executeRoutine()
	{
		if (player.isNoBuffProtected() && player.getAllEffects().length <= 8)
		{
			player.sendMessage("You don't have buffs to use autofarm.");
			player.broadcastUserInfo();
			stop();	
			player.setAutoFarm(false);
			VoicedAutoFarm.showAutoFarm(player);
			return;
		}
		
		calculatePotions();
		checkSpoil();
		targetEligibleCreature();
	    if (player.isMageClass()) 
	    {
	        useAppropriateSpell(); 
	    }
	    else if (shotcutsContainAttack())
	    {
	        attack(); 
	    }
	    else
	    {
	        useAppropriateSpell(); 
	    }
		checkSpoil();
		useAppropriateSpell();
	}

	private void attack() 
	{
		Boolean shortcutsContainAttack = shotcutsContainAttack();
		
	    if (shortcutsContainAttack) 
	        physicalAttack();
	}

	private void useAppropriateSpell() 
	{
		L2Skill chanceSkill = nextAvailableSkill(getChanceSpells(), AutofarmSpellType.Chance);

		if (chanceSkill != null)
		{
			useMagicSkill(chanceSkill, false);
			return;
		}

		L2Skill lowLifeSkill = nextAvailableSkill(getLowLifeSpells(), AutofarmSpellType.LowLife);

		if (lowLifeSkill != null) 
		{
			useMagicSkill(lowLifeSkill, true);
			return;
		}

		L2Skill attackSkill = nextAvailableSkill(getAttackSpells(), AutofarmSpellType.Attack);

		if (attackSkill != null) 
		{
			useMagicSkill(attackSkill, false);
			return;
		}
	}

	public L2Skill nextAvailableSkill(List<Integer> skillIds, AutofarmSpellType spellType) 
	{
		for (Integer skillId : skillIds) 
		{
			L2Skill skill = player.getSkill(skillId);

			if (skill == null) 
				continue;
			
			if (skill.getSkillType() == L2SkillType.SIGNET || skill.getSkillType() == L2SkillType.SIGNET_CASTTIME)
				continue;

			if (!player.checkDoCastConditions(skill)) 
				continue;

			if (isSpoil(skillId))
			{
				if (monsterIsAlreadySpoiled())
				{
					continue;
				}
				return skill;
			}
			
			if (spellType == AutofarmSpellType.Chance && getMonsterTarget() != null)
			{
				if (getMonsterTarget().getFirstEffect(skillId) == null) 
					return skill;
				continue;
			}

			if (spellType == AutofarmSpellType.LowLife && getHpPercentage() > player.getHealPercent()) 
				break;

			return skill;
		}

		return null;
	}
	
	private void checkSpoil() 
	{
		if (canBeSweepedByMe() && getMonsterTarget().isDead()) 
		{
			L2Skill sweeper = player.getSkill(42);
			if (sweeper == null) 
				return;
			
			useMagicSkill(sweeper, false);
		}
	}

	private Double getHpPercentage() 
	{
		return player.getCurrentHp() * 100.0f / player.getMaxHp();
	}

	private Double percentageMpIsLessThan() 
	{
		return player.getCurrentMp() * 100.0f / player.getMaxMp();
	}

	private Double percentageHpIsLessThan()
	{
		return player.getCurrentHp() * 100.0f / player.getMaxHp();
	}

	private List<Integer> getAttackSpells()
	{
		return getSpellsInSlots(AutofarmConstants.attackSlots);
	}

	private List<Integer> getSpellsInSlots(List<Integer> attackSlots) 
	{
		return Arrays.stream(player.getAllShortCuts()).filter(shortcut -> shortcut.getPage() == player.getPage() && shortcut.getType() == L2ShortCut.TYPE_SKILL && attackSlots.contains(shortcut.getSlot())).map(L2ShortCut::getId).collect(Collectors.toList());
	}

	private List<Integer> getChanceSpells()
	{
		return getSpellsInSlots(AutofarmConstants.chanceSlots);
	}

	private List<Integer> getLowLifeSpells()
	{
		return getSpellsInSlots(AutofarmConstants.lowLifeSlots);
	}

	private boolean shotcutsContainAttack() 
	{
		return Arrays.stream(player.getAllShortCuts()).anyMatch(shortcut -> shortcut.getPage() == player.getPage() && shortcut.getType() == L2ShortCut.TYPE_ACTION && (shortcut.getId() == 2 || player.isSummonAttack() && shortcut.getId() == 22));
	}
	
	private boolean monsterIsAlreadySpoiled()
	{
		return getMonsterTarget() != null && getMonsterTarget().getSpoilerId() != 0;
	}
	
	private static boolean isSpoil(Integer skillId)
	{
		return skillId == 254 || skillId == 302;
	}
	
	private boolean canBeSweepedByMe() 
	{
	    return getMonsterTarget() != null && getMonsterTarget().isDead() && getMonsterTarget().getSpoilerId() == player.getObjectId();
	}
	
	private void castSpellWithAppropriateTarget(L2Skill skill, Boolean forceOnSelf)
	{
		if (forceOnSelf) 
		{
			WorldObject oldTarget = player.getTarget();
			player.setTarget(player);
			player.useMagic(skill, false, false);
			player.setTarget(oldTarget);
			return;
		}

		player.useMagic(skill, false, false);
	}

	private void physicalAttack()
	{
	    if (!(player.getTarget() instanceof Monster)) 
	        return;

	    Monster target = (Monster) player.getTarget();

	    if (!player.isMageClass()) 
	    {
	        if (target.isAutoAttackable(player) && GeoEngine.getInstance().canSeeTarget(player, target))
	        {
	            if (GeoEngine.getInstance().canSeeTarget(player, target))
	            {
	                player.getAI().setIntention(CtrlIntention.ATTACK, target);
	                player.onActionRequest();

	                if (player.isSummonAttack() && player.getPet() != null)
	                {
	                    // Siege Golem's
	                    if (player.getPet().getNpcId() >= 14702 && player.getPet().getNpcId() <= 14798 || player.getPet().getNpcId() >= 14839 && player.getPet().getNpcId() <= 14869)
	                        return;

	                    Summon activeSummon = player.getPet();
	                    activeSummon.setTarget(target);
	                    activeSummon.getAI().setIntention(CtrlIntention.ATTACK, target);

	                    int[] summonAttackSkills = {4261, 4068, 4137, 4260, 4708, 4709, 4710, 4712, 5135, 5138, 5141, 5442, 5444, 6095, 6096, 6041, 6044};
	                    if (Rnd.get(100) < player.getSummonSkillPercent())
	                    {
	                        for (int skillId : summonAttackSkills)
	                        {
	                            useMagicSkillBySummon(skillId, target);
	                        }
	                    }
	                }
	            }
	        }
	        else
	        {
	            if (target.isAutoAttackable(player) && GeoEngine.getInstance().canSeeTarget(player, target))
	            if (GeoEngine.getInstance().canSeeTarget(player, target))
	                player.getAI().setIntention(CtrlIntention.FOLLOW, target);
	        }
	    }
	    else
	    {
	        if (player.isSummonAttack() && player.getPet() != null)
	        {
	            // Siege Golem's
	            if (player.getPet().getNpcId() >= 14702 && player.getPet().getNpcId() <= 14798 || player.getPet().getNpcId() >= 14839 && player.getPet().getNpcId() <= 14869)
	                return;

	            Summon activeSummon = player.getPet();
	            activeSummon.setTarget(target);
	            activeSummon.getAI().setIntention(CtrlIntention.ATTACK, target);

	            int[] summonAttackSkills = {4261, 4068, 4137, 4260, 4708, 4709, 4710, 4712, 5135, 5138, 5141, 5442, 5444, 6095, 6096, 6041, 6044};
	            if (Rnd.get(100) < player.getSummonSkillPercent())
	            {
	                for (int skillId : summonAttackSkills)
	                {
	                    useMagicSkillBySummon(skillId, target);
	                }
	            }
	        }
	    }
	}


	public void targetEligibleCreature() {
	    if (player.getTarget() == null) {
	        selectNewTarget(); 
	        return;
	    }

	    if (committedTarget != null) {
	      if (!committedTarget.isDead() && GeoEngine.getInstance().canSeeTarget(player, committedTarget)) {
	            attack();
	            return;
	        } else if (!GeoEngine.getInstance().canSeeTarget(player, committedTarget)) {
	            committedTarget = null;
	            selectNewTarget(); 
	            return;
	        }
	        player.getAI().setIntention(CtrlIntention.FOLLOW, committedTarget);
	        committedTarget = null;
	        player.setTarget(null);
	    }
	    
	    if (committedTarget instanceof Summon) 
	        return;
	        
	    List<Monster> targets = getKnownMonstersInRadius(player, player.getRadius(), creature -> GeoEngine.getInstance().canMoveToTarget(player.getX(), player.getY(), player.getZ(), creature.getX(), creature.getY(), creature.getZ()) && !player.ignoredMonsterContain(creature.getNpcId()) && !creature.isRaidMinion() && !creature.isRaid() && !creature.isDead() && !(creature instanceof Chest) && !(player.isAntiKsProtected() && creature.getTarget() != null && creature.getTarget() != player && creature.getTarget() != player.getPet()) );

	    if (targets.isEmpty())
	        return;

	    Monster closestTarget = targets.stream().min((o1, o2) -> Integer.compare((int) Math.sqrt(player.getDistanceSq(o1)), (int) Math.sqrt(player.getDistanceSq(o2)))).get();

	    committedTarget = closestTarget;
	    player.setTarget(closestTarget);
	}


	


	
	private void selectNewTarget() 
	{
	    List<Monster> targets = getKnownMonstersInRadius(player, player.getRadius(), creature -> GeoEngine.getInstance().canMoveToTarget(player.getX(), player.getY(), player.getZ(), creature.getX(), creature.getY(), creature.getZ()) && !player.ignoredMonsterContain(creature.getNpcId()) && !creature.isRaidMinion() && !creature.isRaid() && !creature.isDead() && !(creature instanceof Chest) && !(player.isAntiKsProtected() && creature.getTarget() != null && creature.getTarget() != player && creature.getTarget() != player.getPet()) );

	    if (targets.isEmpty())
	        return;

	    Monster closestTarget = targets.stream().min((o1, o2) -> Integer.compare((int) Math.sqrt(player.getDistanceSq(o1)), (int) Math.sqrt(player.getDistanceSq(o2)))).get();

	    committedTarget = closestTarget;
	    player.setTarget(closestTarget);
	}




	public final static List<Monster> getKnownMonstersInRadius(Player player, int radius, Function<Monster, Boolean> condition)
	{
		final WorldRegion region = player.getRegion();
		if (region == null)
			return Collections.emptyList();

		final List<Monster> result = new ArrayList<>();

		for (WorldRegion reg : region.getSurroundingRegions())
		{
			for (WorldObject obj : reg.getObjects())
			{
				if (!(obj instanceof Monster) || !MathUtil.checkIfInRange(radius, player, obj, true) || !condition.apply((Monster) obj))
					continue;

				result.add((Monster) obj);
			}
		}

		return result;
	}

	public Monster getMonsterTarget()
	{
		if(!(player.getTarget() instanceof Monster)) 
		{
			return null;
		}

		return (Monster)player.getTarget();
	}

	private void useMagicSkill(L2Skill skill, Boolean forceOnSelf)
	{
		if (skill.getSkillType() == L2SkillType.RECALL && !Config.KARMA_PLAYER_CAN_TELEPORT && player.getKarma() > 0)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (skill.isToggle() && player.isMounted())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (player.isOutOfControl())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (player.isAttackingNow())
			player.getAI().setNextAction(new NextAction(CtrlEvent.EVT_READY_TO_ACT, CtrlIntention.CAST, () -> castSpellWithAppropriateTarget(skill, forceOnSelf)));
		else 
			castSpellWithAppropriateTarget(skill, forceOnSelf);
	}
	
	private boolean useMagicSkillBySummon(int skillId, WorldObject target)
	{
		// No owner, or owner in shop mode.
		if (player == null || player.isInStoreMode())
			return false;
		
		final Summon activeSummon = player.getPet();
		if (activeSummon == null)
			return false;
		
		// Pet which is 20 levels higher than owner.
		if (activeSummon instanceof Pet && activeSummon.getLevel() - player.getLevel() > 20)
		{
			player.sendPacket(SystemMessageId.PET_TOO_HIGH_TO_CONTROL);
			return false;
		}
		
		// Out of control pet.
		if (activeSummon.isOutOfControl())
		{
			player.sendPacket(SystemMessageId.PET_REFUSING_ORDER);
			return false;
		}
		
		// Verify if the launched skill is mastered by the summon.
		final L2Skill skill = activeSummon.getSkill(skillId);
		if (skill == null)
			return false;
		
		// Can't launch offensive skills on owner.
		if (skill.isOffensive() && player == target)
			return false;
		
		activeSummon.setTarget(target);
		return activeSummon.useMagic(skill, false, false);
	}

	private void calculatePotions()
	{
		if (percentageHpIsLessThan() < player.getHpPotionPercentage())
			forceUseItem(1539); 

		if (percentageMpIsLessThan() < player.getMpPotionPercentage())
			forceUseItem(728); 
	}	

	private void forceUseItem(int itemId)
	{
		final ItemInstance potion = player.getInventory().getItemByItemId(itemId);
		if (potion == null)
			return; 

		final IItemHandler handler = ItemHandler.getInstance().getItemHandler(potion.getEtcItem());
		if (handler != null)
			handler.useItem(player, potion, false); 
	}	
}