package net.sf.l2j.gameserver.model.actor.instance;


import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MoveToPawn;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;

import net.sf.l2j.gameserver.events.BossEvent;
import net.sf.l2j.gameserver.events.BossEvent.EventState;

/**
 * @author Zaun
 */
public class BossEventNpc extends Folk
{
   /**
    * @param objectId
    * @param template
    */
   public BossEventNpc(int objectId, NpcTemplate template)
   {
       super(objectId, template);
       // TODO Auto-generated constructor stub
   }
   
   @Override
   public void onAction(Player player)
   {
       if (this != player.getTarget())
       {
           player.setTarget(this);
           player.sendPacket(new MyTargetSelected(getObjectId(), 0));
           player.sendPacket(new ValidateLocation(this));
       }
       else
       {
           if (!canInteract(player))
               player.getAI().setIntention(CtrlIntention.INTERACT, this);
           else
           {
               // Rotate the player to face the instance
               player.sendPacket(new MoveToPawn(player, this, Npc.INTERACTION_DISTANCE));
               
               if (hasRandomAnimation())
                   onRandomAnimation(Rnd.get(8));
               
               showMainWindow(player);
               
               // Send ActionFailed to the player in order to avoid he stucks
               player.sendPacket(ActionFailed.STATIC_PACKET);
           }
       }
   }
   
   private void showMainWindow(Player player)
   {
       
       NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
       html.setFile("data/html/mods/BossEvent/BossEventJoin.htm");
       html.replace("%objectId%", String.valueOf(getObjectId()));
       html.replace("%npcname%", getName());
       html.replace("%regCount%", String.valueOf(BossEvent.getInstance().eventPlayers.size()));
       player.sendPacket(html);
       
       if (BossEvent.getInstance().isRegistered(player))
       {
       html.setFile("data/html/mods/BossEvent/BossEventCancel.htm");
       html.replace("%objectId%", String.valueOf(getObjectId()));
       html.replace("%npcname%", getName());
       html.replace("%regCount%", String.valueOf(BossEvent.getInstance().eventPlayers.size()));
       player.sendPacket(html);
       }
   }
   
   @Override
   public void onBypassFeedback(Player activeChar, String command)
   {
       
       super.onBypassFeedback(activeChar, command);
       if (command.startsWith("register"))
       {
           if (BossEvent.getInstance().getState() != EventState.REGISTRATION)
           {
               activeChar.sendMessage("Boss Event is not running!");
               return;
           }
           if (!BossEvent.getInstance().isRegistered(activeChar))
           {
               if (BossEvent.getInstance().addPlayer(activeChar))
               {
                   activeChar.sendMessage("You have been successfully registered in Boss Event!");
               }
               
           }
           else
           {
               if (BossEvent.getInstance().removePlayer(activeChar))
               {
                   activeChar.sendMessage("You have been successfully removed of Boss Event!");
               }
           }
       }
   }
   
}