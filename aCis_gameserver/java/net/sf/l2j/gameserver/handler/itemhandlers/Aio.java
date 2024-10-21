package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import enginemods.data.ConfigData;
import enginemods.data.PlayerData;
import enginemods.data.SkillData;
import enginemods.engine.mods.SystemAio;
import enginemods.util.builders.html.Html;
import enginemods.util.builders.html.HtmlBuilder;
import enginemods.util.builders.html.HtmlBuilder.HtmlType;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class Aio implements IItemHandler 
{
    @Override
    public void useItem(Playable playable, ItemInstance item, boolean forceUse)
    {
        if (!(playable instanceof Player))
            return;

        Player activeChar = (Player) playable;

        // Verifica si el jugador ya es AIO
        if (PlayerData.get(activeChar).isAio())
        {
            activeChar.sendMessage("You are already AIO.");
            return;
        }

        int itemId = item.getItemId();
        int aioDays = 0;

        // Verificamos qué ítem se utilizó para definir los días de AIO
        if (itemId == ConfigData.AIO_ITEM_ID_7_DAY)
            aioDays = 7;
        else if (itemId == ConfigData.AIO_ITEM_ID_15_DAY)
            aioDays = 15;
        else if (itemId == ConfigData.AIO_ITEM_ID_30_DAY)
            aioDays = 30;
        else
        {
            activeChar.sendMessage("This item cannot be used for AIO.");
            return;
        }

        // Calcula la fecha de expiración
        Calendar time = new GregorianCalendar();
        time.add(Calendar.DAY_OF_YEAR, aioDays);
        long expireTime = time.getTimeInMillis(); // Fecha de expiración en milisegundos

        // Guarda el estado AIO en la base de datos
        SystemAio.getInstance().setValueDB(activeChar, "aio", String.valueOf(expireTime));

        // Actualiza el estado AIO en memoria
        PlayerData.get(activeChar).setAio(true);
        PlayerData.get(activeChar).setAioExpireDate(expireTime);

        // Añade el AIO a la lista de activos y programa la expiración
        addAio(activeChar, expireTime);

        // Informa al administrador (asumiendo que 'player' es un administrador)
        // Si 'player' no está definido, deberías definir quién es el administrador que recibe este mensaje
        // Por ejemplo, si 'activeChar' es el administrador que otorga AIO, usa 'activeChar.getName()'
        // Aquí, asumiremos que deseas informar al propio jugador
        activeChar.sendPacket(new ExShowScreenMessage("Player: " + activeChar.getName() + " is now AIO", 10000, SMPOS.TOP_CENTER, false));

        // Informa al jugador
        activeChar.sendPacket(new ExShowScreenMessage("Dear " + activeChar.getName() + ", you are now AIO for " + aioDays + " days!", 10000, SMPOS.TOP_CENTER, false));

        // Informar la expiración AIO (opcional, ya se envió el mensaje anterior)
        informeExpireAio(activeChar, 1);

        // Otorga los duals (ítems especiales para AIO)
        ItemInstance newItem = ItemTable.getInstance().createItem("aio", ConfigData.AIO_ITEM_ID, 1, activeChar, activeChar);
        activeChar.addItem("aio", newItem, activeChar, true);

        // Destruye el ítem utilizado
        activeChar.destroyItem("Consume", item.getObjectId(), 1, null, true);
    }  
    
    public void addAio(Player player, long expireTime)
    {
        // Calcula el tiempo restante hasta la expiración
        long delay = expireTime - System.currentTimeMillis();
        if (delay <= 0)
        {
            // Si el tiempo ya ha expirado, elimina el AIO inmediatamente
            removeAio(player);
            return;
        }

        // Programa la tarea para eliminar AIO cuando expire
        ThreadPool.schedule(() ->
        {
            if (player == null)
            {
                return;
            }
            
            informeExpireAio(player, 1);
            removeAio(player);
            
            // Actualiza la base de datos para eliminar el estado AIO
            SystemAio.getInstance().setValueDB(player, "aio", "0");
            
        }, delay);

        // Set Max Lvl
        if (ConfigData.AIO_SET_MAX_LVL)
        {
            player.getStat().addExp(player.getStat().getExpForLevel(81));
        }

        // Clear karma
        if (player.getKarma() > 0)
        {
            player.setKarma(0);
        }

        // Teleport a la ciudad si no está en una zona de paz
        if (!player.isInsideZone(ZoneId.PEACE))
        {
            player.teleToLocation(TeleportType.TOWN);
        }
        
        // Establece el título personalizado
        player.setTitle(ConfigData.AIO_TITLE);

        // Añade las habilidades para AIO
        for (IntIntHolder bh : ConfigData.AIO_LIST_SKILLS)
        {
            player.addSkill(bh.getSkill(), false);
        }
        
        // Configura el color del nombre si está permitido
        if (ConfigData.ALLOW_AIO_NCOLOR)
        {
            player.getAppearance().setNameColor(ConfigData.AIO_NCOLOR);
        }
        
        // Configura el color del título si está permitido
        if (ConfigData.ALLOW_AIO_TCOLOR)
        {
            player.getAppearance().setTitleColor(ConfigData.AIO_TCOLOR);
        }
        
        // Actualiza la información del usuario
        player.broadcastUserInfo();
    }
    
    public void removeAio(Player player)
    {
        if (player == null)
            return;
        
        // Elimina el estado AIO en memoria
        PlayerData.get(player).setAio(false);
        PlayerData.get(player).setAioExpireDate(0);

        // Reinicia el título del jugador
        player.setTitle("");
        
        // Elimina las habilidades de AIO
        for (IntIntHolder bh : ConfigData.AIO_LIST_SKILLS)
        {
        	player.removeSkill(bh.getId(), false);
        }
        
        // Restablece el color del nombre si está permitido
        if (ConfigData.ALLOW_AIO_NCOLOR)
        {
            player.getAppearance().setNameColor(0xFFFFFF); // Color por defecto, ajusta según sea necesario
        }
        
        // Restablece el color del título si está permitido
        if (ConfigData.ALLOW_AIO_TCOLOR)
        {
            player.getAppearance().setTitleColor(0xFFFFFF); // Color por defecto, ajusta según sea necesario
        }
        
        // Actualiza la información del usuario
        player.broadcastUserInfo();
    }
    
    /**
     * Envía al jugador un HTML informando la fecha de expiración de AIO.
     * Formato: dd-MMM-yyyy
     * @param player
     * @param page 
     */
    private void informeExpireAio(Player player, int page)
    {
        HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
        
        hb.append("<html><body>");
        hb.append("<br>");
        hb.append(Html.headHtml("AIO"));
        hb.append("<br>");
        
        hb.append("<font color=9900CC>AIO Expire Date: </font>", PlayerData.get(player).getAioExpireDateFormat(), "<br>");
        hb.append("<font color=9900CC>The AIO has the following skills:</font><br>");
        
        hb.append("<table>");
        int MAX_PER_PAGE = 12;
        int searchPage = MAX_PER_PAGE * (page - 1);
        int count = 0;
        for (IntIntHolder bh : ConfigData.AIO_LIST_SKILLS)
        {
            // Salta las habilidades de páginas anteriores
            if (count < searchPage)
            {
                count++;
                continue;
            }
            // No muestra más de MAX_PER_PAGE habilidades
            if (count >= searchPage + MAX_PER_PAGE)
            {
                continue;
            }
            
            hb.append("<tr>");
            hb.append("<td width=32><img src=", SkillData.getSkillIcon(bh.getId()), " width=32 height=32></td>");
            hb.append("<td width=200><font color=LEVEL>", bh.getSkill().getName(), "</font></td>");
            hb.append("</tr>");
            count++;
        }
        hb.append("</table>");
        
        hb.append("<center>");
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        hb.append("<table bgcolor=CC99FF>");
        hb.append("<tr>");
        
        int currentPage = 1;
        for (int i = 0; i < ConfigData.AIO_LIST_SKILLS.size(); i++)
        {
            if (i % MAX_PER_PAGE == 0)
            {
                hb.append("<td width=18 align=center><a action=\"bypass -h Aio aioInfo ", currentPage, "\">" + currentPage, "</a></td>");
                currentPage++;
            }
        }
        
        hb.append("</tr>");
        hb.append("</table>");
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        hb.append("</center>");
        
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

