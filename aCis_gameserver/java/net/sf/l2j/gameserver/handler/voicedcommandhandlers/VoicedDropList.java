package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.text.DecimalFormat;
import java.util.StringTokenizer;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.Config;

import enginemods.data.IconData;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.NpcTable;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author BAN L2JDEV
 */
public class VoicedDropList implements IVoicedCommandHandler
{
	
	private static final String[] _voicedCommands =
	{
		"shifffmodddrop"
	
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		
		
		if (command.startsWith("shifffmodddrop"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			int npcId = Integer.parseInt(st.nextToken());
			int page = (st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : 1;
			
			ShiffNpcDropList(activeChar, npcId, page);
		}
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
	
	final static int ITEMS_PER_LIST = 7;
	
	public static void ShiffNpcDropList(Player activeChar, int npcId, int page)
	{
		final NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		if (npcData == null)
		{
			activeChar.sendMessage("Npc template is unknown for id: " + npcId + ".");
			return;
		}
		
		final StringBuilder sb = new StringBuilder(2000);
		
		if (!npcData.getDropData().isEmpty())
		{
			StringUtil.append(sb, "<html><title> ", npcData.getName(), " Drop List  ", page, "</title><body>");
			StringUtil.append(sb, "<center><img src=\"l2ui_ch3.herotower_deco\" width=256 height=32><br>");
			StringUtil.append(sb, "<img src=\"L2UI.SquareGray\" width=280 height=1>");
			StringUtil.append(sb, "<table border=0 bgcolor=000000 width=\"290\"><tr>");
			StringUtil.append(sb, "<td align=center><font color=\"LEVEL\">Name Item</font></td>");
			StringUtil.append(sb, "<td align=center><font color=\"FF6600\">Quantity Drop</font></td>");
			StringUtil.append(sb, "<td align=center>Chance Drop</td>");
			StringUtil.append(sb, "</tr></table>");
			StringUtil.append(sb, "<img src=\"L2UI.SquareGray\" width=280 height=1>");
			StringUtil.append(sb, "<br><img src=\"L2UI.SquareGray\" width=280 height=1>");
			
			int myPage = 1;
			int i = 0;
			int shown = 0;
			boolean hasMore = false;
			
			//SeedState _seedState = new SeedState(null);
			
			for (DropCategory cat : npcData.getDropData())
			{
				if (shown == ITEMS_PER_LIST)
				{
					hasMore = true;
					break;
				}
				
				for (DropData drop : cat.getAllDrops())
				{
					if (myPage != page)
					{
						i++;
						if (i == ITEMS_PER_LIST)
						{
							myPage++;
							i = 0;
						}
						continue;
					}
					
					if (shown == ITEMS_PER_LIST)
					{
						hasMore = true;
						break;
					}
					
					
				
				//double chance = (npcData.isType("RaidBoss") || npcData.isType("GrandBoss") ? drop.getChance() * 1000. : (drop.getItemId() == 57 ? drop.getChance() * Config.RATE_DROP_ADENA : drop.getChance() * Config.RATE_DROP_ITEMS)) / 10000;
				double chance = (drop.getItemId() == 57 ? drop.getChance() * Config.RATE_DROP_ADENA : drop.getChance() * Config.RATE_DROP_ITEMS) / 10000;
				chance = chance > 100 ? 100 : chance;
				
				String percent = null;
				if (chance <= 0.001)
				{
					DecimalFormat df = new DecimalFormat("#.####");
					percent = df.format(chance);
				}
				else if (chance <= 0.01)
				{
					DecimalFormat df = new DecimalFormat("#.###");
					percent = df.format(chance);
				}
				else
				{
					DecimalFormat df = new DecimalFormat("##.##");
				percent = df.format(chance);
				}
					StringUtil.append(sb, "<table bgcolor=000000><tr>");
					StringUtil.append(sb, "<td><img src=\"" + IconData.getIconByItemId(drop.getItemId()) + "\" width=32 height=32></td><td>");
					StringUtil.append(sb, "<table><tr><td><font color=\"LEVEL\">", ItemTable.getInstance().getTemplate(drop.getItemId()).getName(), "</font>");
					
					//calculando porcentagem Adenas Drop
					if(drop.getItemId() == 57) {
						StringUtil.append(sb, "<font color=\"FF6600\"> (", drop.getMinDrop() * chance, "/", drop.getMaxDrop() * chance , ")</font>");
					}
					else
					{
						//calculando porcentagem Items Drop
						StringUtil.append(sb, "<font color=\"FF6600\"> (", drop.getMinDrop() , "/", drop.getMaxDrop() , ")</font>");
					}
					StringUtil.append(sb, "</td></tr>"); 
					//StringUtil.append(sb,"<td width=240>" + (cat.isSweep() ? "<font color=ff00ff>" + name + "</font>" : name) + "<br1><font color=B09878>" + (cat.isSweep() ? "Spoil" : "Drop") + " Chance : " + percent + "%</font></td>");
					StringUtil.append(sb, "<tr><td>Rate: " + percent + "%" + (ItemTable.getInstance().getTemplate(drop.getItemId()).isQuestItem() ? "Quest" : (cat.isSweep() ? "<font color=\"00FF00\"> Sweep</font><img src=\"L2UI.SquareGray\" width=233 height=1>" : "<font color=\"3BB9FF\"> Drop</font><img src=\"L2UI.SquareGray\" width=233 height=1>")) + "</td></tr></table></td></tr>");
					shown++;
				}
			}
			
			
			StringUtil.append(sb, "</table>");
			StringUtil.append(sb, "<img src=\"L2UI.SquareGray\" width=280 height=1><br>");
			StringUtil.append(sb, "<img src=\"L2UI.SquareGray\" width=280 height=1>");
			StringUtil.append(sb, "<table width=\"100%\" bgcolor=000000><tr>");
			
			if (page > 1)
			{
				StringUtil.append(sb, "<td width=120><a action=\"bypass -h voiced_shifffmodddrop ", npcId, " ", page - 1, "\">Prev Page</a></td>");
				if (!hasMore)
					StringUtil.append(sb, "<td width=100>Page ", page, "</td><td width=70></td></tr>");
			}
			
			if (hasMore)
			{
				if (page <= 1)
					StringUtil.append(sb, "<td width=120></td>");
				
				StringUtil.append(sb, "<td width=100>Page ", page, "</td><td width=70><a action=\"bypass -h voiced_shifffmodddrop ", npcId, " ", page + 1, "\">Next Page</a></td></tr>");
			}
			StringUtil.append(sb, "</table>");
		}
		else
			StringUtil.append(sb, "This NPC has no drops.");
		
		StringUtil.append(sb, "</body></html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(sb.toString());
		activeChar.sendPacket(html);
	}
	
	
	public void addRadar(Player activeChar, int x, int y, int z)
	{
		activeChar.getRadar().addMarker(x, y, z);
	}
	
}