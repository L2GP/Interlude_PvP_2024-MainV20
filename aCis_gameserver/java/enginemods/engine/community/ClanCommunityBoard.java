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
package enginemods.engine.community;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import enginemods.data.ConfigData;
import enginemods.data.PlayerData;
import enginemods.engine.AbstractMods;
import enginemods.util.builders.html.Html;
import enginemods.util.builders.html.HtmlBuilder;
import enginemods.util.builders.html.HtmlBuilder.HtmlType;
import enginemods.util.builders.html.L2UI;
import enginemods.util.builders.html.L2UI_CH3;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.VillageMaster;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.pledge.ClanMember;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;

/**
 * @author fissban
 */
public class ClanCommunityBoard extends AbstractMods
{
	public ClanCommunityBoard()
	{
		registerMod(ConfigData.ENABLE_BBS_CLAN);
	}
	
	@Override
	public void onModState()
	{
		//
	}
	
	@Override
	public boolean onCommunityBoard(Player player, String command)
	{
		if (command.startsWith("_bbsclan"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			// _bbsclan
			st.nextToken();
			// bypass
			String bypass = st.hasMoreTokens() ? st.nextToken() : "listClans";
			
			HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
			hb.append(Html.HTML_START);
			hb.append("<br>");
			hb.append("<center>");
			hb.append(bbsHead(bypass));
			
			switch (bypass)
			{
				case "info":
				{
					// TODO: Sin hacer
					// informacion sobre un clan especifico
					// int clanId = Integer.parseInt(st.nextToken());
					
					// Clan clan = ClanTable.getInstance().getClan(clanId);
					
					break;
				}
				case "listClans":
				{
					// listado de todos los clanes
					// page
					int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
					
					hb.append(bbsListClan(page));
					break;
				}
				case "myClan":
				{
					boolean hasClan = false;
					if (player.getClanId() > 0)
					{
						hasClan = true;
					}
					
					hb.append(Html.htmlHeadCommunity(hasClan ? player.getClan().getName() : "No Name"));
					hb.append("<table border=0 cellspacing=0 cellpadding=0>");
					hb.append("<tr>");
					// menu lateral start -----------------------------------------
					hb.append("<td align=center fixwidth=100>");
					hb.append(bbsMyClanLeft());
					hb.append("</td>");
					// menu lateral end -------------------------------------------
					
					String center = st.hasMoreTokens() ? st.nextToken() : "membersPage";
					int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
					
					hb.append("<td align=center fixwidth=460>");
					hb.append(topInfoClan());
					
					if (!hasClan)
					{
						center = "noHasClan";
						hb.append(bodyInfoNoClan(0));
					}
					else
					{
						// mostramos info del clan
						hb.append(bodyInfoClan(player.getClan(), 0));
					}
					
					switch (center)
					{
						case "noHasClan":
						{
							// TODO Podriamos meterlo en un metodo para ocupar menos espacio no?
							
							hb.append("<br>");
							hb.append("<table border=0 cellspacing=0 cellpadding=0 width=460");
							hb.append("<tr><td align=center>");
							hb.append("Currently you do not have a clan.<br>");
							hb.append(Html.newFontColor("LEVEL", "Would you like to create one ?"), "<br>");
							hb.append("Enter clan name.<br>");
							hb.append("<edit var=\"name\" width=120><br>");
							hb.append("<button value=\"Next\" action=\"bypass _bbsclan createClan $name\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
							hb.append("</td></tr>");
							hb.append("</table>");
							break;
						}
						case "membersPage":
						{
							hb.append(bbsInfoMembers(player, page));
							break;
						}
						case "createAllyPage":
						{
							hb.append("<br>");
							hb.append("<table border=0 cellspacing=0 cellpadding=0 width=460");
							hb.append("<tr><td align=center>");
							hb.append(Html.newFontColor("LEVEL", "Do you want to have an alliance ?"), "<br>");
							hb.append("Enter the alliance name<br>");
							hb.append("<edit var=\"name\" width=120><br>");
							hb.append("<button value=\"Next\" action=\"bypass _bbsclan createAlly $name\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
							hb.append("</td></tr>");
							hb.append("</table>");
							break;
						}
						case "disolveClanPage":
						{
							hb.append("<br>");
							hb.append("<table border=0 cellspacing=0 cellpadding=0 width=460");
							hb.append("<tr><td align=center>");
							hb.append(Html.newFontColor("LEVEL", "Do you want to dissolve your clan?"), "<br>");
							hb.append("<button value=\"Yes\" action=\"bypass _bbsclan disolveClan\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
							hb.append("</td></tr>");
							hb.append("</table>");
							break;
						}
						case "disolveAllyPage":
						case "createAcademyPage":
						case "createRoyalPage":
						case "createKnightPage":
						{
							hb.append("<br>");
							hb.append("<table border=0 cellspacing=0 cellpadding=0 width=460");
							hb.append("<tr><td align=center>");
							hb.append(Html.newFontColor("LEVEL", "Not working yet"), "<br>");
							hb.append("</td></tr>");
							hb.append("</table>");
							break;
						}
						case "changeClanLeaderPage":
						{
							hb.append("<br>");
							hb.append("<table border=0 cellspacing=0 cellpadding=0 width=460");
							hb.append("<tr><td align=center>");
							hb.append(Html.newFontColor("LEVEL", "Do you want to make another player the clan leader?"), "<br>");
							hb.append("Ingresa el nombre del jugador<br>");
							hb.append("<edit var=\"name\" width=120><br>");
							hb.append("<button value=\"Next\" action=\"bypass _bbsclan changeClanLeader $name\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
							hb.append("</td></tr>");
							hb.append("</table>");
							break;
						}
						case "increaseClanLvlPage":
						{
							hb.append(increaseClanLvlPage(player));
							break;
						}
						case "changeClanLeader":
						{
							String name = st.nextToken();
							if (!st.hasMoreTokens())
							{
								hb.append("You have not entered the new leader's name!");
							}
							else if (!player.isClanLeader())
							{
								hb.append("Only the clan leader can execute this action");
							}
							else if (player.getName().equalsIgnoreCase(name))
							{
								hb.append("Are you assigning yourself the new leader?");
							}
							else
							{
								final Clan clan = player.getClan();
								final ClanMember member = clan.getClanMember(name);
								
								if (member == null)
								{
									hb.append("This user does not exist in your clan");
								}
								else if (!member.isOnline())
								{
									hb.append("User is not online");
								}
								
								clan.setNewLeader(member);
								hb.append("Congratulations, the new leader is " + name);
							}
							
							break;
						}
						case "increaseClanLvl":
						{
							if (player.getClan().levelUpClan(player))
							{
								player.broadcastPacket(new MagicSkillUse(player, player, 5103, 1, 0, 0));
								hb.append("Congratulations, you have successfully uploaded your clan.");
								hb.append("<button value=Back action=\"bypass _bbsclan myClan\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td>");
							}
							else
							{
								hb.append("Sorry, you don't meet the requirements!");
								hb.append("<button value=Back action=\"bypass _bbsclan myClan\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td>");
							}
						}
						case "learnSkill":
						{
							if (!player.isClanLeader())
							{
								hb.append("Only the clan leader can learn the skills.");
							}
							else
							{
								/*
								 * AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.SkillType.CLAN); boolean empty = true; for (L2PledgeSkillLearn psl : SkillTreeData.getInstance().getAvailablePledgeSkills(player)) { L2Skill sk = SkillTable.getInstance().getInfo(psl.getId(),
								 * psl.getLevel()); if (sk == null) { continue; } asl.addSkill(psl.getId(), psl.getLevel(), psl.getLevel(), psl.getRepCost(), 0); empty = false; } if (empty) { hb.append("No tienes ningun skill por aprender"); } else { player.sendPacket(asl); }
								 */
								VillageMaster.showPledgeSkillList(player);
							}
							break;
						}
					}
					
					hb.append("</td>");
					// index end --------------------------------------------------------
					hb.append("</tr>");
					hb.append("</table>");
					break;
				}
				case "disolveClan":
				{
					hb.append(dissolveClan(player));
					break;
				}
				case "createClan":
				{
					if (!st.hasMoreTokens())
					{
						hb.append("Enter a name please!");
					}
					else
					{
						String clanName = st.nextToken();
						if (ClanTable.getInstance().createClan(player, clanName) != null)
						{
							hb.append("Your clan has been successfully created");
						}
						else
						{
							hb.append("The clan could not be created");
						}
						
						hb.append("<button value=Back action=\"bypass _bbsclan myClan\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td>");
					}
					break;
				}
				case "createAlly":
				{
					if (!st.hasMoreTokens())
					{
						hb.append("Enter a name please!");
					}
					else
					{
						String allyName = st.nextToken();
						hb.append(createAlly(player, allyName));
					}
					break;
				}
				case "eject":
				{
					if (!st.hasMoreTokens())
					{
						// posible bypass
						break;
					}
					
					ClanMember member = player.getClan().getClanMember(st.nextToken());
					
					hb.append(ejectMember(player, member));
					
					break;
				}
			}
			
			hb.append("</center>");
			hb.append(Html.HTML_END);
			
			sendCommunity(player, hb.toString());
			return true;
		}
		return false;
	}
	
	// XXX HTML LIST CLAN -----------------------------------------------------------------------------
	
	private static String bbsMyClanLeft()
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		
		hb.append("<table width=100 border=0 cellspacing=0 cellpadding=0>");// marco top -> start
		hb.append("<tr>");
		hb.append("<td width=16 valign=top align=center height=22>", Html.newImage(L2UI_CH3.FrameBackLeft, 16, 22), "</td>");
		hb.append("<td width=68 valign=top align=center height=22>", Html.newImage(L2UI_CH3.FrameBackMid, 68, 22), "</td>");
		hb.append("<td width=16 valign=top align=center height=22>", Html.newImage(L2UI_CH3.FrameBackRight, 16, 22), "</td>");
		hb.append("</tr>");
		hb.append("</table>");
		hb.append("<br>");
		hb.append("<table border=0 cellspacing=0 cellpadding=2 width=100 height=22>");
		hb.append("<tr><td width=93 height=22 align=center><button value=\"Members\" action=\"bypass _bbsclan myClan membersPage\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td></tr>");
		hb.append("<tr><td width=93 align=center><button value=\"Create Ally\" action=\"bypass _bbsclan myClan createAllyPage\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td></tr>");
		hb.append("<tr><td width=93 align=center><button value=\"Disolve Ally\" action=\"bypass _bbsclan myClan disolveClanPage\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td></tr>");
		hb.append("<tr><td width=93 align=center><button value=\"Disolve Clan\" action=\"bypass _bbsclan myClan disolveAllyPage\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td></tr>");
		hb.append("<tr><td width=93 align=center><button value=\"Create Academy\" action=\"bypass _bbsclan myClan createAcademyPage\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td></tr>");
		hb.append("<tr><td width=93 align=center><button value=\"Create Royal\" action=\"bypass _bbsclan myClan createRoyalPage\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td></tr>");
		hb.append("<tr><td width=93 align=center><button value=\"Create Knight\" action=\"bypass _bbsclan myClan createKnightPage\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td></tr>");
		hb.append("<tr><td width=93 align=center><button value=\"Change Leader\" action=\"bypass _bbsclan myClan changeClanLeaderPage\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td></tr>");
		hb.append("<tr><td width=93 align=center><button value=\"Increase Lvl\" action=\"bypass _bbsclan myClan increaseClanLvlPage\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td></tr>");
		hb.append("<tr><td width=93 align=center><button value=\"Learn Skills\" action=\"bypass _bbsclan myClan learnSkill\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td></tr>");
		hb.append("</table>");
		return hb.toString();
	}
	
	private static String bbsInfoMembers(Player player, int page)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		hb.append("<br>");
		hb.append("<table><tr><td>", Html.newFontColor("FF8000", "MEMBERS"), "</td></tr></table>");
		
		hb.append("<table border=0 cellspacing=0 cellpadding=0 width=460 height=22");
		hb.append("<tr>");
		hb.append("<td width=16 valign=top align=center height=22>", Html.newImage(L2UI_CH3.FrameBackLeft, 16, 22), "</td>");
		hb.append("<td fixwidth=98 align=center><button value=Name width=98 height=22 back=", L2UI_CH3.FrameBackMid, " fore=", L2UI_CH3.FrameBackMid, "></td>");
		hb.append("<td fixwidth=30 align=center><button value=Lvl width=30 height=22 back=", L2UI_CH3.FrameBackMid, " fore=", L2UI_CH3.FrameBackMid, "></td>");
		hb.append("<td fixwidth=50 align=center><button value=Online width=50 height=22 back=", L2UI_CH3.FrameBackMid, " fore=", L2UI_CH3.FrameBackMid, "></td>");
		hb.append("<td fixwidth=50 align=center><button value=Rebirth width=50 height=22 back=", L2UI_CH3.FrameBackMid, " fore=", L2UI_CH3.FrameBackMid, "></td>");
		hb.append("<td fixwidth=25 align=center><button value=aio width=25 height=22 back=", L2UI_CH3.FrameBackMid, " fore=", L2UI_CH3.FrameBackMid, "></td>");
		hb.append("<td fixwidth=25 align=center><button value=vip width=25 height=22 back=", L2UI_CH3.FrameBackMid, " fore=", L2UI_CH3.FrameBackMid, "></td>");
		hb.append("<td fixwidth=100 align=center><button value=Class width=100 height=22 back=", L2UI_CH3.FrameBackMid, " fore=", L2UI_CH3.FrameBackMid, "></td>");
		hb.append("<td fixwidth=50 align=center><button value=Eject width=50 height=22 back=", L2UI_CH3.FrameBackMid, " fore=", L2UI_CH3.FrameBackMid, "></td>");
		hb.append("<td width=16 valign=top align=center>", Html.newImage(L2UI_CH3.FrameBackRight, 16, 22), "</td>");
		hb.append("</tr>");
		hb.append("</table>");
		
		int MAX_PER_PAGE = 10;
		int searchPage = MAX_PER_PAGE * (page - 1);
		int count = 0;
		int color = 0;
		
		for (ClanMember member : player.getClan().getMembers())
		{
			if (member == null)
			{
				continue;
			}
			// min
			if (count < searchPage)
			{
				count++;
				continue;
			}
			// max
			if (count >= searchPage + MAX_PER_PAGE)
			{
				continue;
			}
			
			// 50
			hb.append("<table width=460 ", color % 2 == 0 ? "bgcolor=000000 " : "", "border=0 cellspacing=0 cellpadding=0>");
			hb.append("<tr>");
			hb.append("<td fixwidth=114 align=center>", member.getName(), "</td>");
			hb.append("<td fixwidth=30 align=center>", getColorLevel(member.getLevel()), "</td>");
			hb.append("<td fixwidth=50 align=center>", member.isOnline() ? Html.newFontColor("3CFF00", "online") : Html.newFontColor("FF0000", "offline"), "</td>");
			hb.append("<td fixwidth=50 align=center>", PlayerData.get(member.getObjectId()).getRebirth(), "</td>");
			hb.append("<td fixwidth=25 align=center>", getIconStatus(PlayerData.get(member.getObjectId()).isAio()), "</td>");
			hb.append("<td fixwidth=25 align=center>", getIconStatus(PlayerData.get(member.getObjectId()).isVip()), "</td>");
			hb.append("<td fixwidth=100 align=center>", ClassId.values()[member.getClassId()], "</td>");
			hb.append("<td fixwidth=66 align=center><button action=\"bypass _bbsclan eject ", member.getName(), "\" width=16 height=16 back=", L2UI.bbs_delete_down, " fore=", L2UI.bbs_delete, "></td>");
			hb.append("</tr>");
			hb.append("</table>");
			count++;
			color++;
		}
		
		hb.append("<br>");
		hb.append("<table>");
		hb.append("<tr>");
		int currentPage = 1;
		int size = player.getClan().getMembersCount();
		for (int i = 0; i < size; i++)
		{
			if (i % MAX_PER_PAGE == 0)
			{
				if (currentPage == page)
				{
					hb.append("<td width=20>", Html.newFontColor("LEVEL", currentPage), "</td>");
				}
				else
				{
					hb.append("<td width=20><a action=\"bypass _bbsclan membersPage ", currentPage, "\">", currentPage, "</a></td>");
				}
				currentPage++;
			}
		}
		hb.append("</tr>");
		hb.append("</table>");
		
		return hb.toString();
	}
	
	private static String topInfoClan()
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		hb.append("<table width=460 height=22 border=0 cellspacing=0 cellpadding=0>");
		hb.append("<tr>");
		hb.append("<td width=16 valign=top align=center>", Html.newImage(L2UI_CH3.FrameBackLeft, 16, 22), "</td>");
		hb.append("<td fixwidth=84 align=center><button value=Name width=84 height=22 back=", L2UI_CH3.FrameBackMid, " fore=", L2UI_CH3.FrameBackMid, "></td>");
		hb.append("<td fixwidth=30 align=center><button value=Lvl width=30 height=22 back=", L2UI_CH3.FrameBackMid, " fore=", L2UI_CH3.FrameBackMid, "></td>");
		hb.append("<td fixwidth=90 align=center><button value=Leader width=90 height=22 back=", L2UI_CH3.FrameBackMid, " fore=", L2UI_CH3.FrameBackMid, "></td>");
		hb.append("<td fixwidth=40 align=center><button value=Members width=40 height=22 back=", L2UI_CH3.FrameBackMid, " fore=", L2UI_CH3.FrameBackMid, "></td>");
		hb.append("<td fixwidth=100 align=center><button value=Ally width=100 height=22 back=", L2UI_CH3.FrameBackMid, " fore=", L2UI_CH3.FrameBackMid, "></td>");
		hb.append("<td fixwidth=84 align=center><button value=Castle width=84 height=22 back=", L2UI_CH3.FrameBackMid, " fore=", L2UI_CH3.FrameBackMid, "></td>");
		hb.append("<td width=16 valign=top align=center>", Html.newImage(L2UI_CH3.FrameBackRight, 16, 22), "</td>");
		hb.append("</tr>");
		hb.append("</table>");
		return hb.toString();
	}
	
	private static String bodyInfoClan(Clan clan, int color)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		hb.append("<table width=460 height=22 ", color % 2 == 0 ? "bgcolor=000000 " : "", "border=0 cellspacing=0 cellpadding=0>");
		hb.append("<tr>");
		hb.append("<td fixwidth=16 height=22 align=center>", Html.newImage(L2UI_CH3.ps_sizecontrol2_over, 16, 16), "</td>");
		hb.append("<td fixwidth=84 height=22 align=center>", Html.newFontColor("FF8000", clan.getName()), "</td>");
		hb.append("<td fixwidth=30 align=center>", getClanColorLevel(clan.getLevel()), "</td>");
		hb.append("<td fixwidth=90 align=center>", clan.getLeader().getName(), "</td>");
		hb.append("<td fixwidth=40 align=center>", clan.getMembersCount(), "</td>");
		hb.append("<td fixwidth=100 align=center>", clan.getAllyId() > 0 ? clan.getAllyName() : "No Ally", "</td>");
		hb.append("<td fixwidth=100 align=center>", clan.hasCastle() ? CastleManager.getInstance().getCastleById(clan.getClanId()).getName() : "No Castle", "</td>");
		// hb.append("<td fixwidth=75 align=center><button value=Info action=\"bypass _bbsclan infor " + clan.getClanId() + "\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
		hb.append("</tr>");
		hb.append("</table>");
		return hb.toString();
	}
	
	private static String bodyInfoNoClan(int color)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		hb.append("<table width=460 ", color % 2 == 0 ? "bgcolor=000000 " : "", "border=0 cellspacing=0 cellpadding=0>");
		hb.append("<tr>");
		hb.append("<td fixwidth=100 align=center>-</td>");
		hb.append("<td fixwidth=30 align=center>-</td>");
		hb.append("<td fixwidth=90 align=center>-</td>");
		hb.append("<td fixwidth=40 align=center>-</td>");
		hb.append("<td fixwidth=100 align=center>-</td>");
		hb.append("<td fixwidth=100 align=center>-</td>");
		// hb.append("<td fixwidth=75 align=center><button value=Info action=\"bypass _bbsclan infor " + clan.getClanId() + "\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
		hb.append("</tr>");
		hb.append("</table>");
		return hb.toString();
	}
	
	private static String bbsListClan(int page)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		
		hb.append(topInfoClan());
		
		int MAX_PER_PAGE = 15;
		int searchPage = MAX_PER_PAGE * (page - 1);
		int count = 0;
		int color = 0;
		
		List<Clan> clansList = new ArrayList<>();
		
		for (Clan clan : ClanTable.getInstance().getClans())
		{
			clansList.add(clan);
		}
		
		// ordenamos el listado segun sus scores
		Collections.sort(clansList, (p1, p2) -> Integer.valueOf(p1.getReputationScore()).compareTo(Integer.valueOf(p2.getReputationScore())));
		
		for (Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan == null)
			{
				continue;
			}
			// min
			if (count < searchPage)
			{
				count++;
				continue;
			}
			// max
			if (count >= searchPage + MAX_PER_PAGE)
			{
				continue;
			}
			
			hb.append(bodyInfoClan(clan, color));
			hb.append(Html.newImage(L2UI.SquareGray, 460, 1));
			color++;
			count++;
		}
		
		int currentPage = 1;
		int size = ClanTable.getInstance().getClans().size();
		
		hb.append("<br>");
		hb.append("<table>");
		hb.append("<tr>");
		for (int i = 0; i < size; i++)
		{
			if (i % MAX_PER_PAGE == 0)
			{
				if (currentPage == page)
				{
					hb.append("<td width=20>", Html.newFontColor("LEVEL", currentPage), "</td>");
				}
				else
				{
					hb.append("<td width=20><a action=\"bypass _bbsclan listClans ", currentPage, "\">", currentPage, "</a></td>");
				}
				
				currentPage++;
			}
		}
		hb.append("</tr>");
		hb.append("</table>");
		
		return hb.toString();
	}
	
	private static String increaseClanLvlPage(Player player)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		hb.append("Requiere:");
		
		switch (player.getClan().getLevel())
		{
			case 0:
				hb.append(Html.newFontColor("LEVEL", "SP: "), "30.000");
				hb.append(Html.newFontColor("LEVEL", "Adena: "), "650.000");
				break;
			case 1:
				hb.append(Html.newFontColor("LEVEL", "SP: "), "150.000");
				hb.append(Html.newFontColor("LEVEL", "Adena: "), "2.500.000");
				break;
			case 2:
				hb.append(Html.newFontColor("LEVEL", "SP: "), "500.000");
				hb.append(Html.newFontColor("LEVEL", "Blood Mark: "), "1");
				break;
			case 3:
				hb.append(Html.newFontColor("LEVEL", "SP: "), "1.400.000");
				hb.append(Html.newFontColor("LEVEL", "Alliance Manifesto: "), "1");
				break;
			case 4:
				hb.append(Html.newFontColor("LEVEL", "SP: 3.500.000"), "");
				hb.append(Html.newFontColor("LEVEL", "Seal of Aspiration: 1"), "");
				break;
			case 5:
				hb.append(Html.newFontColor("LEVEL", "Reputation Score: "), "10.000");
				hb.append(Html.newFontColor("LEVEL", "Member Count: "), "30");
				break;
			case 6:
				hb.append(Html.newFontColor("LEVEL", "Reputation Score: "), "20.000");
				hb.append(Html.newFontColor("LEVEL", "Member Count: "), "80");
				break;
			case 7:
				hb.append(Html.newFontColor("LEVEL", "Reputation Score: "), "40.000");
				hb.append(Html.newFontColor("LEVEL", "Member Count: "), "120");
				break;
			default:
				hb.append(Html.newFontColor("LEVEL", "Maximo Nivel"));
		}
		
		hb.append("<button value=\"Increase Level\" action=\"bypass _bbsclan increaseClanLvl\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		return hb.toString();
	}
	
	private static String dissolveClan(Player player)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		
		if (!player.isClanLeader())
		{
			hb.append("Only the leader can dissolve the clan!");
		}
		else if (player.getClan().getAllyId() != 0)
		{
			hb.append("You cannot dissolve a clan while in an alliance!");
		}
		else if (player.getClan().isAtWar())
		{
			hb.append("You cannot dissolve the clan while in a war!");
		}
		
		else if (player.getClan().hasCastle() || player.getClan().hasHideout())
		{
			hb.append("You cannot dissolve the clan while in a war!");
		}
		else if (player.getClan().getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			hb.append("Your clan is already in dissolution process!");
		}
		else
		{
			for (Castle castle : CastleManager.getInstance().getCastles())
			{
				final Siege siege = castle.getSiege();
				
				if (siege.getAttackerClans().contains(player.getClan()) || siege.getDefenderClans().contains(player.getClan()) || siege.getPendingClans().contains(player.getClan()))
				{
					hb.append("You cannot disband your clan if you are participating in a siege!");
					return hb.toString();
				}
			}
			
			if (Config.ALT_CLAN_DISSOLVE_DAYS > 0)
			{
				player.getClan().setDissolvingExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_DISSOLVE_DAYS * 86400000L);
				player.getClan().updateClanInDB();
				
				ClanTable.getInstance().scheduleRemoveClan(player.getClan());
				hb.append("Your clan started its destruction process!");
			}
			else
			{
				ClanTable.getInstance().destroyClan(player.getClan());
				hb.append("Your clan has been successfully destroyed!");
			}
			
			// The clan leader should take the XP penalty of a full death.
			player.deathPenalty(false, false, false);
		}
		
		return hb.toString();
	}
	
	private static String createAlly(Player player, String allyName)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		
		if (!player.isClanLeader())
		{
			hb.append("Only the leader can create an alliance.");
		}
		else if (player.getClan().getAllyId() != 0)
		{
			hb.append("You already have an alliance");
		}
		else if (player.getClan().getLevel() < 5)
		{
			hb.append("Your clan must be at least level 5 to create an alliance.");
		}
		else if (player.getClan().getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			hb.append("You are penalized for alliance creation.");
		}
		else if (player.getClan().getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			hb.append("You cannot create alliances if your clan is dissolving.");
		}
		else if (ClanTable.getInstance().isAllyExists(allyName))
		{
			hb.append("The alliance name already exists.");
		}
		else
		{
			for (Castle castle : CastleManager.getInstance().getCastles())
			{
				final Siege siege = castle.getSiege();
				
				if (siege.getAttackerClans().contains(player.getClan()) || siege.getDefenderClans().contains(player.getClan()) || siege.getPendingClans().contains(player.getClan()))
				{
					hb.append("You cannot create an alliance during a siege.");
					return hb.toString();
				}
			}
			
			player.getClan().setAllyId(player.getClan().getClanId());
			player.getClan().setAllyName(allyName);
			player.getClan().setAllyPenaltyExpiryTime(0, 0);
			player.getClan().updateClanInDB();
			player.sendPacket(new UserInfo(player));
			
			hb.append("The alliance ", allyName, " was successfully created!");
		}
		
		return hb.toString();
	}
	
	private static String ejectMember(Player player, ClanMember member)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		
		if (member == null)
		{
			// posible bypass
			return hb.toString();
		}
		
		// no te puedes echar a ti mismo
		if (member.getName().equals(player.getName()))
		{
			hb.append(Html.newFontColor("LEVEL", "You can't kick yourself out of the clan!"));
			return hb.toString();
		}
		
		// no se puede echar al duenio del clan
		if (member.getClan().getLeaderName().equals(member.getName()))
		{
			hb.append(Html.newFontColor("LEVEL", "You can't kick out the clan owner!"));
			return hb.toString();
		}
		
		// chequeamos los privilegios del player
		if ((player.getClanPrivileges() & Clan.CP_CL_DISMISS) != Clan.CP_CL_DISMISS)
		{
			hb.append(Html.newFontColor("LEVEL", "You don't have sufficient privileges!"));
			return hb.toString();
		}
		
		Clan clan = player.getClan();
		
		// proceso de actualizacion para el clan --------------------------
		// this also updates the database
		clan.removeClanMember(member.getObjectId(), System.currentTimeMillis() + Config.ALT_CLAN_JOIN_DAYS * 86400000L);
		clan.setCharPenaltyExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_JOIN_DAYS * 86400000L);
		clan.updateClanInDB();
		
		// Remove the player from the members list.
		if (clan.isSubPledgeLeader(member.getObjectId()))
		{
			clan.broadcastClanStatus(); // refresh clan tab
		}
		else
		{
			clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(member.getName()));
		}
		
		clan.broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_EXPELLED).addString(member.getName()));
		player.sendPacket(SystemMessageId.YOU_HAVE_SUCCEEDED_IN_EXPELLING_CLAN_MEMBER);
		player.sendPacket(SystemMessageId.YOU_MUST_WAIT_BEFORE_ACCEPTING_A_NEW_MEMBER);
		
		if (member.isOnline())
		{
			member.getPlayerInstance().sendPacket(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED);
		}
		
		hb.append("The player ", Html.newFontColor("LEVEL", member.getName()), " was successfully kicked out!<br>");
		hb.append("<button value=Back action=\"bypass _bbsclan myClan\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td>");
		
		return hb.toString();
	}
	
	// XXX MISC ---------------------------------------------------------------------------------------
	private static String bbsHead(String bypass)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		hb.append(marcButton(bypass));
		hb.append("<table border=0 cellspacing=0 cellpadding=0>");
		hb.append("<tr>");
		hb.append(newMenu("LIST OF CLANS", "listClans"));
		hb.append(newMenu("MY CLAN", "myClan"));
		hb.append("</tr>");
		hb.append("</table>");
		hb.append(marcButton(bypass));
		
		hb.append("<br>");
		return hb.toString();
	}
	
	private static String newMenu(String butonName, String bypass)
	{
		HtmlBuilder hb = new HtmlBuilder();
		hb.append("<td><button value=\"", butonName, "\" action=\"bypass _bbsclan ", bypass, "\" width=100 height=32 back=", L2UI_CH3.refinegrade3_21, " fore=", L2UI_CH3.refinegrade3_22, "></td>");
		return hb.toString();
	}
	
	private static String marcButton(String bypass)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		if (bypass != null)
		{
			hb.append("<table border=0 cellspacing=0 cellpadding=0>");
			hb.append("<tr>");
			hb.append("<td>", Html.newImage(bypass.equals("listClans") ? L2UI_CH3.fishing_bar1 : L2UI_CH3.ssq_cell1, 100, 1), "</td>");
			hb.append("<td>", Html.newImage(bypass.equals("myClan") ? L2UI_CH3.fishing_bar1 : L2UI_CH3.ssq_cell1, 100, 1), "</td>");
			hb.append("</tr>");
			hb.append("</table>");
		}
		else
		{
			hb.append(Html.newImage(L2UI.SquareGray, 506, 1));
		}
		return hb.toString();
	}
	
	private static String getIconStatus(boolean status)
	{
		if (status)
		{
			return Html.newImage(L2UI_CH3.QuestWndInfoIcon_5, 16, 16);
		}
		return "X";
	}
	
	/**
	 * Asignamos un color segun el lvl del personaje
	 * @param lvl
	 * @return
	 */
	private static String getColorLevel(int lvl)
	{
		HtmlBuilder hb = new HtmlBuilder();
		
		if (lvl >= 20 && lvl < 40)
		{
			hb.append(Html.newFontColor("LEVEL", lvl)); // amarillo
		}
		else if (lvl >= 40 && lvl < 76)
		{
			hb.append(Html.newFontColor("9A5C00", lvl)); // naranja oscuro
		}
		else if (lvl >= 76)
		{
			hb.append(Html.newFontColor("FF0000", lvl));// rojo
		}
		else
		{
			hb.append(lvl);
		}
		
		return hb.toString();
	}
	
	/**
	 * Asignamos un color segun el lvl del personaje
	 * @param lvl
	 * @return
	 */
	private static String getClanColorLevel(int lvl)
	{
		HtmlBuilder hb = new HtmlBuilder();
		
		if (lvl >= 2 && lvl < 4)
		{
			hb.append(Html.newFontColor("LEVEL", lvl)); // amarillo
		}
		else if (lvl >= 5 && lvl < 7)
		{
			hb.append(Html.newFontColor("9A5C00", lvl)); // naranja oscuro
		}
		else if (lvl >= 7)
		{
			hb.append(Html.newFontColor("FF0000", lvl));// rojo
		}
		else
		{
			hb.append(lvl);
		}
		
		return hb.toString();
	}
}
