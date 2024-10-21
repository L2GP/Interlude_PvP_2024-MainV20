package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.Config; // Importamos la clase de configuración donde está la variable NOBLESSE_ITEM_ID
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class Nobless implements IItemHandler 
{

	 @Override
	    public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	 {
		 if (!(playable instanceof Player))
			 return;

		 Player activeChar = (Player) playable;

		 // Verifica si el jugador ya es Noblesse
		 if (activeChar.isNoble())
		 {
			 activeChar.sendMessage("You are already Noblesse.");
			 return;
		 }

		 int itemId = item.getItemId();

		 // Verificamos que el ítem sea el correcto para convertir en Noblesse
		 if (itemId == Config.NOBLESSE_ITEM_ID)
		 {
			 // Convertimos al jugador en Noblesse
			 activeChar.setNoble(true, true);
			 activeChar.sendMessage("Congratulations! You have become Noblesse!");
			 
			 // Enviar mensaje HTML cuando el jugador se convierte en Noblesse
			 NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
			 html.setHtml("<html><body><title>Congratulations</title><br><center>" +
					 "<img src=l2font-e.replay_logo-e width=255 height=60><br><br><br><br>" +
					 "<font color=\"LEVEL\">Congratulations,</font><br><br>You acquired all<br1>status from a Noblesse...<br><br>" +
					 "<table><tr><td><img src=icon.skill1323 width=32 height=32></td><td width=5></td>" +
					 "<td><img src=icon.skill1324 width=32 height=32></td><td width=5></td>" +
					 "<td><img src=icon.skill1325 width=32 height=32></td><td width=5></td>" +
					 "<td><img src=icon.skill1326 width=32 height=32></td></tr>" +
					 "<tr><td><img src=icon.skill1327 width=32 height=32></td><td width=5></td>" +
					 "<td><img src=icon.skill0325 width=32 height=32></td><td width=5></td>" +
					 "<td><img src=icon.skill0326 width=32 height=32></td><td width=5></td>" +
					 "<td><img src=icon.skill0327 width=32 height=32></td></tr></table></center></body></html>");
			 activeChar.sendPacket(html); // Envía el HTML al jugador

			 // Actualizamos los estados del jugador
			 activeChar.broadcastUserInfo();
			 
			 // Destruimos el ítem utilizado
			 activeChar.destroyItem("Consume", item.getObjectId(), 1, null, true);
		 } 
		 else
		 {
			 activeChar.sendMessage("This item cannot be used for Noblesse.");
		 }
	 }
}

