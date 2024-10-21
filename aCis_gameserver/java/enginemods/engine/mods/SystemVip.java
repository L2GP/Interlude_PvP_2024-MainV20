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
import enginemods.engine.AbstractMods;
import enginemods.enums.ExpSpType;
import enginemods.enums.ItemDropType;
import enginemods.holders.PlayerHolder;
import enginemods.instances.NpcDropsInstance;
import enginemods.instances.NpcExpInstance;
import enginemods.util.Util;
import enginemods.util.builders.html.Html;
import enginemods.util.builders.html.HtmlBuilder;
import enginemods.util.builders.html.HtmlBuilder.HtmlType;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.skills.Stats;

/**
 * @author fissban
 */
public class SystemVip extends AbstractMods {
    // Instancia única de la clase (Singleton)
    private static SystemVip instance;

    // Constructor público (llamado por el motor de mods)
    public SystemVip() {
        registerMod(true); // TODO: Falta configuración
        instance = this; // Asignar la instancia al crear el objeto
    }

    /**
     * Método para obtener la única instancia de SystemVip.
     * @return la instancia única de SystemVip.
     * @throws IllegalStateException si SystemVip no ha sido inicializado.
     */
    public static SystemVip getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SystemVip no ha sido inicializado.");
        }
        return instance;
    }

    @Override
    public void onModState() {
        switch (getState()) {
            case START:
                readAllVips();
                break;
            case END:
                // Implementar lógica al finalizar el mod si es necesario
                break;
        }
    }

    @Override
    public void onNpcExpSp(Player killer, Attackable npc, NpcExpInstance instance) {
        if (!PlayerData.get(killer).isVip()) {
            return;
        }
        
        // Aumentar EXP y SP según las bonificaciones VIP
        instance.increaseRate(ExpSpType.EXP, ConfigData.VIP_BONUS_XP);
        instance.increaseRate(ExpSpType.SP, ConfigData.VIP_BONUS_SP);
    }

    @Override
    public void onNpcDrop(Player killer, Attackable npc, NpcDropsInstance instance) {
        if (!PlayerData.get(killer).isVip()) {
            return;
        }
        // Aumentar cantidad y probabilidad de drops según las bonificaciones VIP
        
        // Aumentar drops normales
        instance.increaseDrop(ItemDropType.NORMAL, ConfigData.VIP_BONUS_DROP_NORMAL_AMOUNT, ConfigData.VIP_BONUS_DROP_NORMAL_CHANCE);
        // Aumentar drops spoil
        instance.increaseDrop(ItemDropType.SPOIL, ConfigData.VIP_BONUS_DROP_SPOIL_AMOUNT, ConfigData.VIP_BONUS_DROP_SPOIL_CHANCE);
        // Aumentar drops herb
        instance.increaseDrop(ItemDropType.HERB, ConfigData.VIP_BONUS_DROP_HERB_AMOUNT, ConfigData.VIP_BONUS_DROP_HERB_CHANCE);
        // Aumentar drops seed
        instance.increaseDrop(ItemDropType.SEED, ConfigData.VIP_BONUS_DROP_SEED_AMOUNT, ConfigData.VIP_BONUS_DROP_SEED_CHANCE);
    }

    @Override
    public void onEvent(Player player, Creature npc, String command) {
        StringTokenizer st = new StringTokenizer(command, " ");
        
        String event = st.nextToken();
        switch (event) {
            case "allVip":
            {
                if (player.getAccessLevel().getLevel() < 1) {
                    break;
                }
                
                if (st.hasMoreTokens()) {
                    int page = Integer.parseInt(st.nextToken());
                    getAllPlayerVips(player, page);
                }
                break;
            }
        }
    }

    @Override
    public boolean onAdminCommand(Player player, String chat) {
        StringTokenizer st = new StringTokenizer(chat, " ");
        
        String command = st.nextToken().toLowerCase();
        switch (command) {
            case "allvip":
            {
                getAllPlayerVips(player, 1);
                return true;
            }
            case "removevip":
            {
                if (!checkTarget(player)) {
                    return true;
                }
                
                Player target = (Player) player.getTarget();
                removeVip(target);
                player.sendPacket(new ExShowScreenMessage("Player: " + target.getName() + " is no longer VIP.", 10000, SMPOS.TOP_CENTER, false));
                target.sendPacket(new ExShowScreenMessage("You are no longer VIP.", 10000, SMPOS.TOP_CENTER, false));
                return true;
            }
            // Solo para admins
            // Formato: setVip days
            case "setvip":
            {
                if (!checkTarget(player)) {
                    return true;
                }
                
                if (!st.hasMoreTokens()) {
                    player.sendMessage("Correct command:");
                    player.sendMessage(".setVip days");
                    return true;
                }
                
                String daysStr = st.nextToken();
                
                if (!Util.isNumber(daysStr)) {
                    player.sendMessage("Correct command:");
                    player.sendMessage(".setVip days");
                    return true;
                }
                
                int days = Integer.parseInt(daysStr);
                Player vipPlayer = (Player) player.getTarget();
                
                // Calcular tiempo de expiración
                Calendar time = new GregorianCalendar();
                time.add(Calendar.DAY_OF_YEAR, days);
                long expireTime = time.getTimeInMillis();
                
                // Establecer VIP usando el método setVip
                setVip(vipPlayer, expireTime);
                
                // Informar al admin y al jugador
                player.sendPacket(new ExShowScreenMessage("Player: " + vipPlayer.getName() + " is now VIP for " + days + " days.", 10000, SMPOS.TOP_CENTER, false));
                vipPlayer.sendPacket(new ExShowScreenMessage("You are now VIP for " + days + " days.", 10000, SMPOS.TOP_CENTER, false));
                
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void onEnterWorld(Player player) {
        if (PlayerData.get(player).isVip()) {
            if (PlayerData.get(player).getVipExpireDate() < System.currentTimeMillis()) {
                removeVip(player);
                return;
            }
            
            addVip(player, PlayerData.get(player).getVipExpireDate());
            informeExpireVip(player);
        }
    }

    @Override
    public double onStats(Stats stat, Creature character, double value) {
        if (!Util.areObjectType(Player.class, character)) {
            return value;
        }
        
        Player player = (Player) character;
        
        if (!PlayerData.get(player.getObjectId()).isVip()) {
            return value;
        }
        
        if (ConfigData.VIP_STATS.containsKey(stat)) {
            return value * ConfigData.VIP_STATS.get(stat);
        }
        
        return value;
    }

    /**
     * Envía un HTML al jugador informando la fecha de expiración del VIP. (formato: dd-MMM-yyyy)
     * @param player el jugador al que se le envía el HTML
     */
    private static void informeExpireVip(Player player) {
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
        hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ((ConfigData.VIP_BONUS_DROP_SPOIL_AMOUNT - 1) * 100), "%</font></td>");
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
        sendHtml(player, null, hb);
    }
    
    /**
     * Añade el estado VIP al jugador y programa su expiración.
     * @param player el jugador al que se le añade VIP.
     * @param expireTime tiempo en milisegundos cuando expirará el VIP.
     */
    public void addVip(Player player, long expireTime) {
        // Programar la expiración del VIP
        ThreadPool.schedule(() -> {
            if (player == null) {
                return;
            }
            
            informeExpireVip(player);
            removeVip(player);
            
        }, expireTime - System.currentTimeMillis());
        
        // Configurar colores si está permitido
        if (ConfigData.ALLOW_VIP_NCOLOR) {
            player.getAppearance().setNameColor(ConfigData.VIP_NCOLOR);
        }
        
        if (ConfigData.ALLOW_VIP_TCOLOR) {
            player.getAppearance().setTitleColor(ConfigData.VIP_TCOLOR);
        }
        
        // Actualizar la información del jugador
        player.broadcastUserInfo();
    }
    
    /**
     * Remueve el estado VIP del jugador.
     * @param player el jugador al que se le remueve VIP.
     */
    public static void removeVip(Player player) {
        // Remover estado VIP en memoria
        PlayerData.get(player).setVip(false);
        player.broadcastUserInfo();
    }
    
    /**
     * Obtiene y muestra todos los jugadores VIP paginados.
     * @param player el jugador que ejecuta el comando.
     * @param page la página actual.
     */
    public void getAllPlayerVips(Player player, int page) {
        HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
        
        hb.append("<html><body>");
        hb.append("<br>");
        hb.append(Html.headHtml("All VIP Players"));
        hb.append("<br>");
        
        hb.append("<table>");
        hb.append("<tr>");
        hb.append("<td width=64><font color=LEVEL>Player:</font></td><td width=200><font color=LEVEL>ExpireDate:</font></td>");
        hb.append("</tr>");
        hb.append("</table>");
        
        int MAX_PER_PAGE = 12;
        int searchStart = MAX_PER_PAGE * (page - 1);
        int count = 0;
        int countVip = 0;
        
        for (PlayerHolder ph : PlayerData.getAllPlayers()) {
            if (ph.isVip()) {
                countVip++;
                // Ignorar hasta llegar a la página actual
                if (count < searchStart) {
                    count++;
                    continue;
                }
                // Detenerse después de alcanzar el máximo por página
                if (count >= searchStart + MAX_PER_PAGE) {
                    break;
                }
                
                hb.append("<table", count % 2 == 0 ? " bgcolor=000000>" : ">");
                hb.append("<tr>");
                hb.append("<td width=64>" + ph.getName() + "</td><td width=200>" + ph.getVipExpireDateFormat() + "</td>");
                hb.append("</tr>");
                hb.append("</table>");
                count++;
            }
        }
        
        // Navegación entre páginas
        hb.append("<center>");
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        hb.append("<table bgcolor=CC99FF>");
        hb.append("<tr>");
        
        int totalPages = (int) Math.ceil((double) countVip / MAX_PER_PAGE);
        
        for (int i = 1; i <= totalPages; i++) {
            hb.append("<td width=18><center><a action=\"bypass -h Engine SystemVip allVip " + i + "\">" + i + "</a></center></td>");
        }
        
        hb.append("</tr>");
        hb.append("</table>");
        hb.append("<img src=L2UI.SquareGray width=264 height=1>");
        hb.append("</center>");
        
        hb.append("</body></html>");
        sendHtml(player, null, hb);
    }
    
    /**
     * Lee todos los jugadores VIP desde la base de datos al iniciar el mod.
     */
    private void readAllVips() {
        for (PlayerHolder ph : PlayerData.getAllPlayers()) {
            String timeInMillis = getValueDB(ph.getObjectId(), "vip");
            // Si no tiene valor en la base de datos, continuar
            if (timeInMillis == null) {
                continue;
            }
            
            long expireTime = Long.parseLong(timeInMillis);
            
            // Si ya expiró, continuar
            if (expireTime < System.currentTimeMillis()) {
                continue;
            }
            
            // Establecer estado VIP en memoria
            PlayerData.get(ph.getObjectId()).setVip(true);
            PlayerData.get(ph.getObjectId()).setVipExpireDate(expireTime);
            
            // Obtener la instancia de Player utilizando World
            Player player = World.getInstance().getPlayer(ph.getObjectId());
            if (player != null) {
                // Añadir VIP y programar su expiración
                addVip(player, expireTime);
            }
        }
    }
    
    /**
     * Verifica si el jugador tiene un objetivo válido para los comandos.
     * @param ph el jugador que ejecuta el comando.
     * @return true si el objetivo es válido, false de lo contrario.
     */
    private static boolean checkTarget(Player ph) {
        if (ph.getTarget() == null) {
            ph.sendMessage("This command requires a target.");
            return false;
        }
        
        if (!Util.areObjectType(Player.class, ph.getTarget())) {
            ph.sendMessage("This command requires a player target.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Establece el estado VIP para un jugador con una fecha de expiración específica.
     * @param player el jugador al que se le establece VIP.
     * @param expireTime tiempo en milisegundos cuando expirará el VIP.
     */
    public void setVip(Player player, long expireTime) {
        // Guardar el estado VIP en la base de datos
        setValueDB(player, "vip", String.valueOf(expireTime));

        // Actualizar en memoria
        PlayerData.get(player).setVip(true);
        PlayerData.get(player).setVipExpireDate(expireTime);

        // Añadir VIP y programar su expiración
        addVip(player, expireTime);

        // Configurar colores si está permitido
        if (ConfigData.ALLOW_VIP_NCOLOR) {
            player.getAppearance().setNameColor(ConfigData.VIP_NCOLOR);
        }
        
        if (ConfigData.ALLOW_VIP_TCOLOR) {
            player.getAppearance().setTitleColor(ConfigData.VIP_TCOLOR);
        }

        // Actualizar la información del jugador
        player.broadcastUserInfo();

        // Enviar mensajes al jugador
        player.sendPacket(new ExShowScreenMessage(
            "You are now VIP until " + PlayerData.get(player).getVipExpireDateFormat(),
            10000,
            SMPOS.TOP_CENTER,
            false
        ));
        informeExpireVip(player);
    }
}


