package net.sf.l2j.gameserver.handler.voicedcommandhandlers;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.templates.StatsSet;


import net.sf.l2j.commons.math.MathUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.data.NpcTable ;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;


public class VoicedRaidInfo implements IVoicedCommandHandler
{
	
	String hora = "MMM dd, HH:mm";
	private final static int PAGE_LIMIT = 14;
	private static final String[] VOICED_COMMANDS =
	{
		"raid",
		"raidinfo",
		"epic",
		"chat"
	
	};
	
	public void showChatWindow(Player player, int val)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/mods/boss/index.htm");
		html.replace("%list%", getList(player, val));
		player.sendPacket(html);
	}
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String targe)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		String currentCommand = st.nextToken();
		
		if (currentCommand.startsWith("chat"))
		{
			int val = Integer.parseInt(st.nextToken());
			showChatWindow(activeChar, val);
			activeChar.getRadar().removeAllMarkers();
			
		}
		
		if (command.equals("raidinfo") || command.equals("raid")|| command.equals("epic"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/mods/boss/index.htm");
			html.replace("%list%", getList(activeChar, 0));
			activeChar.sendPacket(html);
		}
		return true; 
		
	}
	private String getList(Player player, int page)
	{
		// Retrieve the entire types list based on group type.
		List<Integer> list = Config.LIST_RAID_BOSS_IDS;
		
		// Calculate page number.
		final int max = MathUtil.countPagesNumber(list.size(), PAGE_LIMIT);
		page = page > max ? max : page < 1 ? 1 : page;
		
		// Cut skills list up to page number.
		list = list.subList((page - 1) * PAGE_LIMIT, Math.min(page * PAGE_LIMIT, list.size()));
		
		final StringBuilder sb = new StringBuilder();
	sb.append("<center><img src=\"l2ui_ch3.herotower_deco\" width=256 height=32><br>");
		//sb.append("<img src=L2UI.SquareGray width=296 height=1>");
		
		int row = 0;
		
		for (int bossId : list)
		{
			String name = "";
			NpcTemplate template = null;
			if ((template = NpcTable.getInstance().getTemplate(bossId)) != null)
			{
				name = template.getName();
			}
			else
			{
				
				continue;
			}
			StatsSet boss_stat = null;
			L2Spawn actual_boss_stat = null;

			long delay = 0;
			if (NpcTable.getInstance().getTemplate(bossId).isType("RaidBoss"))
			{	
				actual_boss_stat = RaidBossSpawnManager.getInstance().getSpawns().get(bossId);
				if (actual_boss_stat != null) {
					//reload in databse
					RaidBossSpawnManager.getInstance().reloadBossFromDB(bossId);
					delay = actual_boss_stat.getRespawnTimeSaved();
				}
					
			}
			else if (NpcTable.getInstance().getTemplate(bossId).isType("GrandBoss"))
			{
				boss_stat = GrandBossManager.getInstance().getStatsSet(bossId);
				
				if (boss_stat != null)
					delay = boss_stat.getLong("respawn_time");
			}
			else
				continue;
			
			
			byte level = template.getLevel();
			
			if ( delay <= System.currentTimeMillis())
				sb.append("<table width=296 bgcolor=000000><tr><td width=226 align=center><a action=\"bypass voiced_shifffmodddrop " + bossId + "\">Lv " + level + " " + name + "</a></td></tr><tr><td width=70 align=center><font color=9CC300>Alive</font></td></tr></table><img src=L2UI.SquareGray width=296 height=1>");
			else {
				
				sb.append("<table width=296 bgcolor=000000><tr><td width=226 align=center>Lv " + level + " " + name + "</td></tr><tr><td width=70 align=center>Respawn in: <font color=FF0000>" + new SimpleDateFormat("d/M/y HH:mm:ss").format(new Date(delay)) + "</font></td></tr></table><img src=L2UI.SquareGray width=296 height=1>");
			}
			row++;
		}
		
		for (int i = PAGE_LIMIT; i > row; i--)
			sb.append("<img height=20>");
		
		// Build page footer.
		sb.append("<img height=4><img src=L2UI.SquareGray width=296 height=1><table width=296 bgcolor=000000><tr>");
		sb.append("<td align=left width=75><button value=\"Refresh\" action=\"bypass voiced" + "_chat " + page + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2></td>");
		sb.append("<td align=center width=75>" + (page > 1 ? "<button value=\"< PREV\" action=\"bypass voiced" + "_chat " + (page - 1) + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("<td align=center width=71>Page " + page + "</td>");
		sb.append("<td align=right width=75>" + (page < max ? "<button value=\"NEXT >\" action=\"bypass voiced" + "_chat " + (page + 1) + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("</tr></table>");
		
		return sb.toString();
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
	

	
}
