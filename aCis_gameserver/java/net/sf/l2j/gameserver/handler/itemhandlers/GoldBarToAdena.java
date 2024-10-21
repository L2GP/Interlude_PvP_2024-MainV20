package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

public class GoldBarToAdena implements IItemHandler
{
    @Override
    public void useItem(Playable playable, ItemInstance item, boolean forceUse)
    {    
        if (!(playable instanceof Player))
            return;
    
        Player player = (Player) playable;
        
        if(player.getInventory().getAdena() >= 2100000000 ) {
        	player.sendMessage("You exceed the adena limit in the inventory.");
        	player.sendPacket(ActionFailed.STATIC_PACKET);
        	return;
        }

        if(player.getInventory().getAllItemsByItemId(3470) != null) {
        	player.getInventory().addItem("GoldBarToAdena", 57, Config.ADENA_TO_GOLDBAR, player, player);
        	player.sendPacket(new ExShowScreenMessage("Now you have " + Config.ADENA_TO_GOLDBAR +" adena !" , 1500, 0x02, true));
        	player.destroyItem("", item.getObjectId(), 1, null, true);}
    	else {            
        	player.sendMessage("Don´t have GoldBars.");
        	player.sendPacket(ActionFailed.STATIC_PACKET);
        	return;}
    }
}