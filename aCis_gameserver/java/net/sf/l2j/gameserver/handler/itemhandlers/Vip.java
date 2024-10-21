package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import enginemods.data.ConfigData;
import enginemods.data.PlayerData;
import enginemods.engine.mods.SystemVip;
import enginemods.util.builders.html.Html;
import enginemods.util.builders.html.HtmlBuilder;
import enginemods.util.builders.html.HtmlBuilder.HtmlType;

public class Vip implements IItemHandler 
{
    @Override
    public void useItem(Playable playable, ItemInstance item, boolean forceUse)
    {
        if (!(playable instanceof Player))
            return;

        Player activeChar = (Player) playable;

        // Verifica si el jugador ya es VIP
        if (PlayerData.get(activeChar).isVip())
        {
            activeChar.sendMessage("You are already VIP.");
            return;
        }

        int itemId = item.getItemId();
        int vipDays = 0;

        // Verificamos qué ítem se utilizó para definir los días de VIP
        if (itemId == ConfigData.VIP_ITEM_ID_1_DAY)
            vipDays = 1;
        else if (itemId == ConfigData.VIP_ITEM_ID_7_DAY)
            vipDays = 7;
        else if (itemId == ConfigData.VIP_ITEM_ID_15_DAY)
            vipDays = 15;
        else if (itemId == ConfigData.VIP_ITEM_ID_30_DAY)
            vipDays = 30;
        else
        {
            activeChar.sendMessage("This item cannot be used for VIP.");
            return;
        }

        // Calcula la fecha de expiración
        long currentTime = System.currentTimeMillis();
        long expireTime = currentTime + (vipDays * 24L * 60L * 60L * 1000L); // vipDays en milisegundos

        // Guarda el estado VIP en la base de datos y en memoria
        SystemVip.getInstance().setVip(activeChar, expireTime);
        activeChar.sendMessage("Congratulations! You have become VIP for " + vipDays + " days!");

        // Actualizamos los estados del jugador
        activeChar.broadcastUserInfo();

        // Destruimos el ítem utilizado
        activeChar.destroyItem("Consume", item.getObjectId(), 1, null, true);
        
        // Llamar al método para informar la expiración VIP
        informeExpireVip(activeChar);
    }    
    
    private void informeExpireVip(Player player) {
        HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
        
        hb.append("<html><body>");
        hb.append(Html.headHtml("VIP"));
        hb.append("<br>");
        
        // Fecha de expiración
        hb.append("<font color=9900CC>VIP Expire Date: </font>", PlayerData.get(player).getVipExpireDateFormat(), "<br>");
        
        hb.append("<font color=LEVEL>The VIP have exp/sp rate:</font><br>");
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        hb.append("<table fixwidth=264 border=0 cellspacing=0 cellpadding=0 bgcolor=CC99FF>");
        hb.append("<tr>");
        hb.append("<td fixwidth=100 align=center><button value=\"Type\" action=\"\" width=100 height=19 back=L2UI_CH3.amountbox2 fore=L2UI_CH3.amountbox2></td>");
        hb.append("<td fixwidth=164 align=center><button value=\"Bonus\" action=\"\" width=164 height=19 back=L2UI_CH3.amountbox2 fore=L2UI_CH3.amountbox2></td>");
        hb.append("</tr>");
        hb.append("</table>");
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        hb.append("<table fixwidth=264>");
        hb.append("<tr>");
        hb.append("<td fixwidth=100 align=center><font color=3366FF>EXP:</font></td>");
        hb.append("<td fixwidth=164 align=center><font color=LEVEL>", (int) ((ConfigData.VIP_BONUS_XP - 1) * 100), "%</font></td>");
        hb.append("</tr>");
        hb.append("</table>");
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        hb.append("<table fixwidth=264>");
        hb.append("<tr>");
        hb.append("<td fixwidth=100 align=center><font color=3366FF>SP:</font></td>");
        hb.append("<td fixwidth=164 align=center><font color=LEVEL>", (int) ((ConfigData.VIP_BONUS_SP - 1) * 100), "%</font></td>");
        hb.append("</tr>");
        hb.append("</table>");
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        
        hb.append("<br><br><font color=LEVEL>The VIP have drop rate:</font><br>");
        
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        hb.append("<table fixwidth=264 border=0 cellspacing=0 cellpadding=0 bgcolor=CC99FF>");
        hb.append("<tr>");
        hb.append("<td fixwidth=100><button value=\"Type\" action=\"\" width=100 height=19 back=L2UI_CH3.amountbox2 fore=L2UI_CH3.amountbox2></td>");
        hb.append("<td fixwidth=82><button value=\"Bonus Amount\" action=\"\" width=82 height=19 back=L2UI_CH3.amountbox2 fore=L2UI_CH3.amountbox2></td>");
        hb.append("<td fixwidth=82><button value=\"Bonus Chance\" action=\"\" width=82 height=19 back=L2UI_CH3.amountbox2 fore=L2UI_CH3.amountbox2></td>");
        hb.append("</tr>");
        hb.append("</table>");
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        hb.append("<table fixwidth=264 border=0 cellspacing=0 cellpadding=0>");
        hb.append("<tr>");
        hb.append("<td fixwidth=100 align=center><font color=3366FF>Normal:</font></td>");
        hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ((ConfigData.VIP_BONUS_DROP_NORMAL_AMOUNT - 1) * 100), "%</font></td>");
        hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ((ConfigData.VIP_BONUS_DROP_NORMAL_CHANCE - 1) * 100), "%</font></td>");
        hb.append("</tr>");
        hb.append("</table>");
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        hb.append("<table fixwidth=264 border=0 cellspacing=0 cellpadding=0>");
        hb.append("<tr>");
        hb.append("<td fixwidth=100 align=center><font color=3366FF>Spoil:</font></td>");
        hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ((ConfigData.VIP_BONUS_DROP_SPOIL_AMOUNT  - 1) * 100), "%</font></td>");
        hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ((ConfigData.VIP_BONUS_DROP_SPOIL_CHANCE - 1) * 100), "%</font></td>");
        hb.append("</tr>");
        hb.append("</table>");
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        hb.append("<table fixwidth=264 border=0 cellspacing=0 cellpadding=0>");
        hb.append("<tr>");
        hb.append("<td fixwidth=100 align=center><font color=3366FF>Seed:</font></td>");
        hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ((ConfigData.VIP_BONUS_DROP_SEED_AMOUNT - 1) * 100), "%</font></td>");
        hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ((ConfigData.VIP_BONUS_DROP_SEED_CHANCE - 1) * 100), "%</font></td>");
        hb.append("</tr>");
        hb.append("</table>");
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        hb.append("<table fixwidth=264 border=0 cellspacing=0 cellpadding=0>");
        hb.append("<tr>");
        hb.append("<td fixwidth=100 align=center><font color=3366FF>Herb:</font></td>");
        hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ((ConfigData.VIP_BONUS_DROP_HERB_AMOUNT - 1) * 100), "%</font></td>");
        hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ((ConfigData.VIP_BONUS_DROP_HERB_CHANCE - 1) * 100), "%</font></td>");
        hb.append("</tr>");
        hb.append("</table>");
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        
        hb.append("</body></html>");
        
        // Enviar el HTML al jugador
        sendHtml(player, null, hb);
    }   
    
    // Método para enviar el HTML al jugador
    private void sendHtml(Player player, String fileName, HtmlBuilder html) {
        NpcHtmlMessage msg = new NpcHtmlMessage(0); // Cambia el constructor aquí según la API
        msg.setHtml(html.toString());
        player.sendPacket(msg);
    }    
}






