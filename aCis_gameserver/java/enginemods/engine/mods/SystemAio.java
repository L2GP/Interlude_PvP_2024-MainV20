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
package enginemods.engine.mods;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

import enginemods.data.ConfigData;
import enginemods.data.PlayerData;
import enginemods.data.SkillData;
import enginemods.engine.AbstractMods;
import enginemods.holders.PlayerHolder;
import enginemods.util.Util;
import enginemods.util.builders.html.Html;
import enginemods.util.builders.html.HtmlBuilder;
import enginemods.util.builders.html.HtmlBuilder.HtmlType;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Gatekeeper;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.skills.Stats;

/**
 * @author fissban
 */
public class SystemAio extends AbstractMods
{
    private static SystemAio instance;

    public SystemAio() {
        instance = this; // Asigna la instancia actual
        registerMod(true); // TODO missing config
    }

    public static SystemAio getInstance() {
        return instance; // Devuelve la instancia
    }
    
    @Override
    public void onModState()
    {
        switch (getState())
        {
            case START:
                readAllAios();
                break;
            case END:
                //
                break;
        }
    }
    
    @Override
    public boolean onInteract(Player player, Creature npc)
    {
        if (PlayerData.get(player.getObjectId()).isAio() && !ConfigData.INTERACT_NPC)
        {
            if (!Util.areObjectType(Gatekeeper.class, npc))
            {
                return true; // Bloquear interacción si AIO y la variable es false
            }
        }
        
        return false; // Permite la interacción
    }
    
    @Override
    public void onExitZone(Creature player, L2ZoneType zone)
    {
        if (!Util.areObjectType(Player.class, player))
        {
            return;
        }
        
        if (!PlayerData.get(player.getObjectId()).isAio())
        {
            return;
        }
        
        ThreadPool.schedule(() -> new CheckZone((Player) player), 3000);
        
    }
    
    private static class CheckZone implements Runnable
    {
        Player _player;
        
        public CheckZone(Player player)
        {
            _player = player;
        }
        
        @Override
        public void run()
        {
            if (!_player.isInsideZone(ZoneId.PEACE))
            {
                _player.teleToLocation(TeleportType.TOWN);
            }
        }
        
    }
    
    @Override
    public void onEvent(Player player, Creature npc, String command)
    {
        StringTokenizer st = new StringTokenizer(command, " ");
        
        if (!st.hasMoreTokens()) {
            return;
        }
        
        String event = st.nextToken();
        switch (event)
        {
            case "allAio":
            {
                if (player.getAccessLevel().getLevel() < 1)
                {
                    break;
                }
                
                if (st.hasMoreTokens()) {
                    try {
                        int page = Integer.parseInt(st.nextToken());
                        getAllPlayerAios(player, page);
                    } catch (NumberFormatException e) {
                        player.sendMessage("Número de página inválido. Mostrando página 1.");
                        getAllPlayerAios(player, 1);
                    }
                } else {
                    getAllPlayerAios(player, 1);
                }
                break;
            }
            case "aioInfo":
            {
                if (st.hasMoreTokens()) {
                    try {
                        int page = Integer.parseInt(st.nextToken());
                        informeExpireAio(player, page);
                    } catch (NumberFormatException e) {
                        player.sendMessage("Número de página inválido. Mostrando página 1.");
                        informeExpireAio(player, 1);
                    }
                } else {
                    informeExpireAio(player, 1);
                }
                break;
            }
        }
    }
    
    @Override
    public boolean onAdminCommand(Player player, String chat)
    {
        StringTokenizer st = new StringTokenizer(chat, " ");
        
        if (!st.hasMoreTokens()) {
            return false;
        }
        
        String command = st.nextToken().toLowerCase();
        switch (command)
        {
            case "allaio":
            {
                getAllPlayerAios(player, 1);
                return true;
            }
            // only for admins
            // format: setAio days
            case "removeaio":
            {
                if (!checkTarget(player))
                {
                    return true;
                }
                
                removeAio((Player) player.getTarget());
                return true;
            }
            case "setaio":
            {
                if (!checkTarget(player))
                {
                    return true;
                }
                
                if (!st.hasMoreTokens())
                {
                    player.sendMessage("Comando correcto:");
                    player.sendMessage(".setAio días");
                    return true;
                }
                
                String days = st.nextToken();
                
                if (!Util.isNumber(days))
                {
                    player.sendMessage("Comando correcto:");
                    player.sendMessage(".setAio días");
                    return true;
                }
                
                Player aio = (Player) player.getTarget();
                
                // Create calendar
                Calendar time = new GregorianCalendar();
                time.add(Calendar.DAY_OF_YEAR, Integer.parseInt(days));
                // save values in DB
                setValueDB(aio, "aio", time.getTimeInMillis() + "");
                // saved state in memory
                PlayerData.get(aio).setAio(true);
                PlayerData.get(aio).setAioExpireDate(time.getTimeInMillis());
                
                addAio(aio, time.getTimeInMillis());
                
                // Informed admin
                player.sendPacket(new ExShowScreenMessage("Player: " + aio.getName() + " is now AIO", 10000, SMPOS.TOP_CENTER, false));
                // Informed player
                aio.sendPacket(new ExShowScreenMessage("Dear " + aio.getName() + ", you are now AIO", 10000, SMPOS.TOP_CENTER, false));
                informeExpireAio(aio, 1);
                
                // give duals
                ItemInstance item = ItemTable.getInstance().createItem("aio", ConfigData.AIO_ITEM_ID, 1, aio, aio);
                aio.addItem("aio", item, aio, true);
                
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public void onEnterWorld(Player player)
    {
        if (PlayerData.get(player).isAio())
        {
            if (PlayerData.get(player).getAioExpireDate() < System.currentTimeMillis())
            {
                removeAio(player); // Si ha expirado, elimina el estado AIO.
                return;
            }
            
            addAio(player, PlayerData.get(player).getAioExpireDate()); // Añadir el estado AIO.
            informeExpireAio(player, 1); // Informar al jugador del tiempo restante.
        }
    }

    @Override
    public double onStats(Stats stat, Creature character, double value)
    {
        if (!Util.areObjectType(Player.class, character))
        {
            return value;
        }
        
        if (!PlayerData.get(character.getObjectId()).isAio())
        {
            return value;
        }
        
        if (ConfigData.AIO_STATS.containsKey(stat))
        {
            return value *= ConfigData.AIO_STATS.get(stat);
        }
        
        return value;
    }
    
    /**
     * Send the character html informing the time expire AIO. (format: dd-MMM-yyyy)
     * @param player
     * @param page 
     */
    private static void informeExpireAio(Player player, int page)
    {
        HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
        
        hb.append("<html><body>");
        hb.append("<br>");
        hb.append(Html.headHtml("AIO"));
        hb.append("<br>");
        
        hb.append("<font color=9900CC>AIO Expire Date: </font>", PlayerData.get(player).getAioExpireDateFormat(), "<br>");
        hb.append("<font color=9900CC>The AIO have the skills:</font><br>");
        
        hb.append("<table>");
        int MAX_PER_PAGE = 12;
        int searchPage = MAX_PER_PAGE * (page - 1);
        int count = 0;
        int totalSkills = ConfigData.AIO_LIST_SKILLS.size();
        int totalPages = (int) Math.ceil((double) totalSkills / MAX_PER_PAGE);
        
        // Validación de número de página
        if (page < 1) {
            page = 1;
        } else if (page > totalPages && totalPages > 0) {
            page = totalPages;
        }
        
        // Mostrar mensaje si no hay habilidades
        if (totalSkills == 0) {
            hb.append("<br><center><font color=FF0000>No tienes habilidades AIO disponibles.</font></center><br>");
        } else {
            for (IntIntHolder bh : ConfigData.AIO_LIST_SKILLS)
            {
                // min
                if (count < searchPage)
                {
                    count++;
                    continue;
                }
                // max
                if (count >= searchPage + MAX_PER_PAGE)
                {
                    break; // Salir del loop una vez alcanzado el límite
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
            
            // Generar enlaces de paginación basados en totalPages
            for (int i = 1; i <= totalPages; i++)
            {
                hb.append("<td width=18 align=center><a action=\"bypass -h Engine SystemAio aioInfo ", i, "\">", i, "</a></td>");
            }
            
            hb.append("</tr>");
            hb.append("</table>");
            hb.append("<img src=L2UI.SquareGray width=264 height=1>");
            hb.append("</center>");
        }
        
        hb.append("</body></html>");
        sendHtml(player, null, hb);
    }
    
    public void addAio(Player player, long dayTime)
    {
        // Schedule to end AIO
        ThreadPool.schedule(() ->
        {
            // TODO missing onExitWorld
            if (player == null)
            {
                return;
            }
            
            informeExpireAio(player, 1);
            removeAio(player);
            
        }, dayTime - System.currentTimeMillis());
        // Set Max Lvl
        if (ConfigData.AIO_SET_MAX_LVL)
        {
            player.getStat().addExp(player.getStat().getExpForLevel(81));
        }
        // clear karma
        if (player.getKarma() > 0)
        {
            player.setKarma(0);
        }
        // teleport to city
        if (!player.isInsideZone(ZoneId.PEACE))
        {
            player.teleToLocation(TeleportType.TOWN);
        }
        
        // set custom tile
        player.setTitle(ConfigData.AIO_TITLE);
        // add skills for aio
        for (IntIntHolder bh : ConfigData.AIO_LIST_SKILLS)
        {
            player.addSkill(bh.getSkill(), false);
        }
        
        if (ConfigData.ALLOW_AIO_NCOLOR)
        {
            player.getAppearance().setNameColor(ConfigData.AIO_NCOLOR);
        }
        
        if (ConfigData.ALLOW_AIO_TCOLOR)
        {
            player.getAppearance().setTitleColor(ConfigData.AIO_TCOLOR);
        }
        
        player.broadcastUserInfo();
    }
    
    public void removeAio(Player player)
    {
        // remove state in memory
        PlayerData.get(player).setAio(false);
        // remove AIO skills
        for (IntIntHolder bh : ConfigData.AIO_LIST_SKILLS)
        {
        	player.removeSkill(bh.getId(), false);
        }
        // init title
        player.setTitle("");
        player.broadcastUserInfo();
        
        // Opcional: Informar al jugador que su AIO ha expirado
        player.sendPacket(new ExShowScreenMessage(
            "Your AIO status has expired.",
            10000,
            SMPOS.TOP_CENTER,
            false
        ));
    }
    
    public void getAllPlayerAios(Player player, int page)
    {
        HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
        
        hb.append("<html><body>");
        hb.append("<br>");
        hb.append(Html.headHtml("All AIO Players"));
        hb.append("<br>");
        
        hb.append("<table>");
        hb.append("<tr>");
        hb.append("<td width=64><font color=LEVEL>Player:</font></td><td width=200><font color=LEVEL>ExpireDate:</font></td>");
        hb.append("</tr>");
        hb.append("</table>");
        
        int MAX_PER_PAGE = 12;
        int searchPage = MAX_PER_PAGE * (page - 1);
        int count = 0;
        int countAio = 0;
        
        for (PlayerHolder ph : PlayerData.getAllPlayers())
        {
            if (ph.isAio())
            {
                countAio++;
            }
        }
        
        int totalPages = (int) Math.ceil((double) countAio / MAX_PER_PAGE);
        
        // Validación de número de página
        if (page < 1) {
            page = 1;
        } else if (page > totalPages && totalPages > 0) {
            page = totalPages;
        }
        
        // Reiniciar el contador para iterar nuevamente
        count = 0;
        
        for (PlayerHolder ph : PlayerData.getAllPlayers())
        {
            if (ph.isAio())
            {
                // min
                if (count < searchPage)
                {
                    count++;
                    continue;
                }
                // max
                if (count >= searchPage + MAX_PER_PAGE)
                {
                    break; // Salir del loop una vez alcanzado el límite
                }
                
                hb.append("<table", count % 2 == 0 ? " bgcolor=000000>" : ">");
                hb.append("<tr>");
                hb.append("<td width=64>", ph.getName(), "</td><td width=200>", ph.getAioExpireDateFormat(), "</td>");
                hb.append("</tr>");
                hb.append("</table>");
                count++;
            }
        }
        
        hb.append("<center>");
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        hb.append("<table bgcolor=CC99FF>");
        hb.append("<tr>");
        
        // Generar enlaces de paginación basados en totalPages
        for (int i = 1; i <= totalPages; i++)
        {
            hb.append("<td width=18 align=center><a action=\"bypass -h Engine SystemAio allAio ", i, "\">", i, "</a></td>");
        }
        
        hb.append("</tr>");
        hb.append("</table>");
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        hb.append("</center>");
        
        hb.append("</body></html>");
        sendHtml(player, null, hb);
    }
    
    private void readAllAios()
    {
        for (PlayerHolder ph : PlayerData.getAllPlayers())
        {
            String timeInMillis = getValueDB(ph.getObjectId(), "aio");
            // Don't has value in db
            if (timeInMillis == null)
            {
                continue;
            }
            
            long dayTime = Long.parseLong(timeInMillis);
            
            if (dayTime < System.currentTimeMillis())
            {
                continue;
            }
            
            // saved state in memory
            PlayerData.get(ph.getObjectId()).setAio(true);
            PlayerData.get(ph.getObjectId()).setAioExpireDate(dayTime);
        }
    }
    
    public void setAio(Player player, long expireTime) {
        // Guardar el estado AIO en la base de datos
        setValueDB(player, "Aio", String.valueOf(expireTime));

        // Actualizar en memoria
        PlayerData.get(player).setAio(true);
        PlayerData.get(player).setAioExpireDate(expireTime);

        // Añadir AIO y programar su expiración
        addAio(player, expireTime);

        // Configurar colores si está permitido
        if (ConfigData.ALLOW_AIO_NCOLOR) {
            player.getAppearance().setNameColor(ConfigData.AIO_NCOLOR);
        }
        
        if (ConfigData.ALLOW_AIO_TCOLOR) {
            player.getAppearance().setTitleColor(ConfigData.AIO_TCOLOR);
        }

        // Actualizar la información del jugador
        player.broadcastUserInfo();
        
        // Asegúrate de que el tiempo de expiración se esté guardando correctamente
        System.out.println("Expire time set to: " + expireTime); // Depuración

        
        // Enviar mensajes al jugador
        player.sendPacket(new ExShowScreenMessage(
            "You are now AIO until " + PlayerData.get(player).getAioExpireDateFormat(),
            10000,
            SMPOS.TOP_CENTER,
            false
        ));
        informeExpireAio(player, 1);
    }
    
    private static boolean checkTarget(Player ph)
    {
        if (ph.getTarget() == null)
        {
            ph.sendMessage("Este comando necesita un objetivo.");
            return false;
        }
        
        if (!Util.areObjectType(Player.class, ph.getTarget()))
        {
            ph.sendMessage("Este comando necesita un objetivo que sea un jugador.");
            return false;
        }
        
        return true;
    }	
     
}
