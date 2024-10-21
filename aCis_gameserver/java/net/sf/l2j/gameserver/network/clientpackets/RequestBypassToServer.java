package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.CustomBypassHandler;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.OlympiadManagerNpc;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.type.WeaponType;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

import enginemods.EngineModsManager;
import mods.autofarm.AutofarmPlayerRoutine;
import mods.dressme.DressMeData;
import mods.dressme.SkinPackage;

public final class RequestBypassToServer extends L2GameClientPacket
{
	private static final Logger GMAUDIT_LOG = Logger.getLogger("gmaudit");
	
	private String _command;
	
	@Override
	protected void readImpl()
	{
		_command = readS();
	}
	
	@Override
	protected void runImpl()
	{
		if (!FloodProtectors.performAction(getClient(), Action.SERVER_BYPASS))
			return;
		
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		final AutofarmPlayerRoutine bot = activeChar.getBot();
		
		if (_command.isEmpty())
		{
			_log.info(activeChar.getName() + " sent an empty requestBypass packet.");
			activeChar.logout();
			return;
		}
		
		try
		{
			if (_command.startsWith("admin_"))
			{
				if (EngineModsManager.onVoiced(activeChar, _command))
					return;
				
				String command = _command.split(" ")[0];
				
				IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler(command);
				if (ach == null)
				{
					if (activeChar.isGM())
						activeChar.sendMessage("The command " + command.substring(6) + " doesn't exist.");
					
					_log.warning("No handler registered for admin command '" + command + "'");
					return;
				}
				
				if (!AdminData.getInstance().hasAccess(command, activeChar.getAccessLevel()))
				{
					activeChar.sendMessage("You don't have the access rights to use this command.");
					_log.warning(activeChar.getName() + " tried to use admin command " + command + " without proper Access Level.");
					return;
				}
				
				if (Config.GMAUDIT)
					GMAUDIT_LOG.info(activeChar.getName() + " [" + activeChar.getObjectId() + "] used '" + _command + "' command on: " + ((activeChar.getTarget() != null) ? activeChar.getTarget().getName() : "none"));
				
				ach.useAdminCommand(_command, activeChar);
			}
			
			// Autofarm
			else if (_command.startsWith("_infosettings"))
			{
				showAutoFarm(activeChar);
			}
			
			else if (_command.startsWith("_autofarm"))
			{
				if (activeChar.isAutoFarm())
				{
					bot.stop();
					activeChar.setAutoFarm(false);
				}
				else
				{
					bot.start();
					activeChar.setAutoFarm(true);
				}
				
			}
			
			if (_command.startsWith("_pageAutoFarm"))
			{
				StringTokenizer st = new StringTokenizer(_command, " ");
				st.nextToken();
				try
				{
					String param = st.nextToken();
					
					if (param.startsWith("inc_page") || param.startsWith("dec_page"))
					{
						int newPage;
						
						if (param.startsWith("inc_page"))
						{
							newPage = activeChar.getPage() + 1;
						}
						else
						{
							newPage = activeChar.getPage() - 1;
						}
						
						if (newPage >= 0 && newPage <= 9)
						{
							String[] pageStrings =
							{
								"F1",
								"F2",
								"F3",
								"F4",
								"F5",
								"F6",
								"F7",
								"F8",
								"F9",
								"F10"
							};
							
							activeChar.setPage(newPage);
							activeChar.sendPacket(new ExShowScreenMessage("Auto Farm Skill Bar " + pageStrings[newPage], 3 * 1000, SMPOS.TOP_CENTER, false));
							activeChar.saveAutoFarmSettings();
							
						}
						
					}
					
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			
			if (_command.startsWith("_enableBuffProtect"))
			{
				activeChar.setNoBuffProtection(!activeChar.isNoBuffProtected());
				if (activeChar.isNoBuffProtected())
				{
					activeChar.sendPacket(new ExShowScreenMessage("Auto Farm Buff Protect On", 3 * 1000, SMPOS.TOP_CENTER, false));
				}
				else
				{
					activeChar.sendPacket(new ExShowScreenMessage("Auto Farm Buff Protect Off", 3 * 1000, SMPOS.TOP_CENTER, false));
				}
				activeChar.saveAutoFarmSettings();
			}
			if (_command.startsWith("_enableSummonAttack"))
			{
				activeChar.setSummonAttack(!activeChar.isSummonAttack());
				if (activeChar.isSummonAttack())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.ACTIVATE_SUMMON_ACTACK));
					activeChar.sendPacket(new ExShowScreenMessage("Auto Farm Summon Attack On", 3 * 1000, SMPOS.TOP_CENTER, false));
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.DESACTIVATE_SUMMON_ACTACK));
					activeChar.sendPacket(new ExShowScreenMessage("Auto Farm Summon Attack Off", 3 * 1000, SMPOS.TOP_CENTER, false));
				}
				activeChar.saveAutoFarmSettings();
			}
			// Autofarm enf
			
			else if (_command.startsWith("player_help "))
			{
				playerHelp(activeChar, _command.substring(12));
			}
			if (_command.startsWith("voiced_"))
			{
				String command = _command.split(" ")[0];
				
				IVoicedCommandHandler ach = VoicedCommandHandler.getInstance().getHandler(_command.substring(7));
				if (ach == null)
				{
					activeChar.sendMessage("The command " + command.substring(7) + " does not exist!");
					_log.warning("No handler registered for command '" + _command + "'");
					return;
				}
				
				ach.useVoicedCommand(_command.substring(7), activeChar, null);
			}  
			else if (_command.startsWith("npc_"))
			{
				if (!activeChar.validateBypass(_command))
					return;
				
				int endOfId = _command.indexOf('_', 5);
				String id;
				if (endOfId > 0)
					id = _command.substring(4, endOfId);
				else
					id = _command.substring(4);
				
				try
				{
					final WorldObject object = World.getInstance().getObject(Integer.parseInt(id));
					
					if (object != null && object instanceof Npc && endOfId > 0 && ((Npc) object).canInteract(activeChar))
						((Npc) object).onBypassFeedback(activeChar, _command.substring(endOfId + 1));
					
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{
				}
			}
			// Navigate throught Manor windows
			else if (_command.startsWith("manor_menu_select?"))
			{
				WorldObject object = activeChar.getTarget();
				if (object instanceof Npc)
					((Npc) object).onBypassFeedback(activeChar, _command);
			}
			else if (_command.startsWith("bbs_") || _command.startsWith("_bbs") || _command.startsWith("_friend") || _command.startsWith("_mail") || _command.startsWith("_block"))
			{
				CommunityBoard.getInstance().handleCommands(getClient(), _command);
			}
			else if (_command.startsWith("Quest "))
			{
				if (!activeChar.validateBypass(_command))
					return;
				
				String[] str = _command.substring(6).trim().split(" ", 2);
				if (str.length == 1)
					activeChar.processQuestEvent(str[0], "");
				else
					activeChar.processQuestEvent(str[0], str[1]);
			}
			else if (_command.startsWith("_match"))
			{
				String params = _command.substring(_command.indexOf("?") + 1);
				StringTokenizer st = new StringTokenizer(params, "&");
				int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
				int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
				int heroid = Hero.getInstance().getHeroByClass(heroclass);
				if (heroid > 0)
					Hero.getInstance().showHeroFights(activeChar, heroclass, heroid, heropage);
			}
			else if (_command.startsWith("_diary"))
			{
				String params = _command.substring(_command.indexOf("?") + 1);
				StringTokenizer st = new StringTokenizer(params, "&");
				int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
				int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
				int heroid = Hero.getInstance().getHeroByClass(heroclass);
				if (heroid > 0)
					Hero.getInstance().showHeroDiary(activeChar, heroclass, heroid, heropage);
			}
			else if (_command.startsWith("arenachange")) // change
			{
				final boolean isManager = activeChar.getCurrentFolk() instanceof OlympiadManagerNpc;
				if (!isManager)
				{
					// Without npc, command can be used only in observer mode on arena
					if (!activeChar.isInObserverMode() || activeChar.isInOlympiadMode() || activeChar.getOlympiadGameId() < 0)
						return;
				}
				
				if (OlympiadManager.getInstance().isRegisteredInComp(activeChar))
				{
					activeChar.sendPacket(SystemMessageId.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME);
					return;
				}
				
				final int arenaId = Integer.parseInt(_command.substring(12).trim());
				activeChar.enterOlympiadObserverMode(arenaId);
			}
			else if (_command.startsWith("droplist"))
			{
				StringTokenizer st = new StringTokenizer(_command, " ");
				st.nextToken();
				
				int npcId = Integer.parseInt(st.nextToken());
				int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
				
				if (st.hasMoreTokens())
					activeChar.ignored(Integer.parseInt(st.nextToken()));
				
				Npc.sendNpcDrop(activeChar, npcId, page);
			}
			else if (_command.startsWith("voiced_"))
			{
				String command = _command.split(" ")[0];

				IVoicedCommandHandler ach = VoicedCommandHandler.getInstance().getHandler(_command.substring(7));

				if (ach == null)
				{
					activeChar.sendMessage("The command " + command.substring(7) + " does not exist!");
					_log.warning("No handler registered for command '" + _command + "'");
					return;
				}

				ach.useVoicedCommand(_command.substring(7), activeChar, null);
			}
			else if (_command.startsWith("Engine"))
			{	
				EngineModsManager.onEvent(activeChar,_command.replace("Engine ", ""));
			}
			// Dressme ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
			else if (_command.startsWith("custom_"))
			{
				Player player = getClient().getActiveChar();
				CustomBypassHandler.getInstance().handleBypass(player, _command);
			}
			
			
			else if (_command.startsWith("dressme"))
			{
				if (!Config.ALLOW_DRESS_ME_IN_OLY && activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("DressMe can't be used on The Olympiad game.");
					return;
				}
				
				StringTokenizer st = new StringTokenizer(_command, " ");
				st.nextToken();
				if (!st.hasMoreTokens())
				{
					showDressMeMainPage(activeChar);
					return;
				}
				int page = Integer.parseInt(st.nextToken());
				
				if (!st.hasMoreTokens())
				{
					showDressMeMainPage(activeChar);
					return;
				}
				String next = st.nextToken();
				if (next.startsWith("skinlist"))
				{
					String type = st.nextToken();
					showSkinList(activeChar, type, page);
				}
				else if (next.startsWith("myskinlist"))
				{
					
					showMySkinList(activeChar, page);
				}
				if (next.equals("clean"))
				{
					String type = st.nextToken();
					
					if (activeChar.isTryingSkin())
					{
						activeChar.sendMessage("You can't do this while trying a skin.");
						activeChar.sendPacket(new ExShowScreenMessage("You can't do this while trying a skin.", 2000, 2, false));
						activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						showDressMeMainPage(activeChar);
						return;
					}
					
					switch (type.toLowerCase())
					{
						case "armor":
							activeChar.setArmorSkinOption(0);
							break;
						case "weapon":
							activeChar.setWeaponSkinOption(0);
							break;
						case "hair":
							activeChar.setHairSkinOption(0);
							break;
						case "face":
							activeChar.setFaceSkinOption(0);
							break;
						case "shield":
							activeChar.setShieldSkinOption(0);
							break;
					}
					
					activeChar.broadcastUserInfo();
					showMySkinList(activeChar, page);
				}
				else if (next.startsWith("buyskin"))
				{
					if (!st.hasMoreTokens())
					{
						showDressMeMainPage(activeChar);
						return;
					}
					
					int skinId = Integer.parseInt(st.nextToken());
					String type = st.nextToken();
					int itemId = Integer.parseInt(st.nextToken());
					
					SkinPackage sp = null;
					
					switch (type.toLowerCase())
					{
						case "armor":
							sp = DressMeData.getInstance().getArmorSkinsPackage(skinId);
							break;
						case "weapon":
							sp = DressMeData.getInstance().getWeaponSkinsPackage(skinId);
							
							if (activeChar.getActiveWeaponItem() == null)
							{
								activeChar.sendMessage("You can't buy this skin without a weapon.");
								activeChar.sendPacket(new ExShowScreenMessage("You can't buy this skin without a weapon.", 2000, 2, false));
								activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
								showSkinList(activeChar, type, page);
								return;
							}
							
							ItemInstance skinWeapon = null;
							if (ItemTable.getInstance().getTemplate(itemId) != null)
							{
								skinWeapon = ItemTable.getInstance().createDummyItem(itemId);
								
								if (!checkWeapons(activeChar, skinWeapon, WeaponType.BOW, WeaponType.BOW) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.SWORD, WeaponType.SWORD) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.BLUNT, WeaponType.BLUNT) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.DAGGER, WeaponType.DAGGER) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.POLE, WeaponType.POLE) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.DUAL, WeaponType.DUAL) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.DUALFIST, WeaponType.DUALFIST) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.BIGSWORD, WeaponType.BIGSWORD) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.FIST, WeaponType.FIST) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.BIGBLUNT, WeaponType.BIGBLUNT))
								{
									activeChar.sendMessage("This skin is not suitable for your weapon type.");
									activeChar.sendPacket(new ExShowScreenMessage("This skin is not suitable for your weapon type.", 2000, 2, false));
									activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
									showSkinList(activeChar, type, page);
									return;
								}
							}
							break;
						case "hair":
							sp = DressMeData.getInstance().getHairSkinsPackage(skinId);
							break;
						case "face":
							sp = DressMeData.getInstance().getFaceSkinsPackage(skinId);
							break;
						case "shield":
							sp = DressMeData.getInstance().getShieldSkinsPackage(skinId);
							if (activeChar.getActiveWeaponItem() == null)
							{
								activeChar.sendMessage("You can't buy this skin without a weapon.");
								activeChar.sendPacket(new ExShowScreenMessage("You can't buy this skin without a weapon.", 2000, 2, false));
								activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
								showSkinList(activeChar, type, page);
								return;
							}
							
							ItemInstance skinShield = null;
							if (ItemTable.getInstance().getTemplate(itemId) != null)
							{
								skinShield = ItemTable.getInstance().createDummyItem(itemId);
								
								if (!checkWeapons(activeChar, skinShield, WeaponType.BOW, WeaponType.BOW) //
									|| !checkWeapons(activeChar, skinShield, WeaponType.POLE, WeaponType.POLE) //
									|| !checkWeapons(activeChar, skinShield, WeaponType.DUAL, WeaponType.DUAL) //
									|| !checkWeapons(activeChar, skinShield, WeaponType.DUALFIST, WeaponType.DUALFIST) //
									|| !checkWeapons(activeChar, skinShield, WeaponType.BIGSWORD, WeaponType.BIGSWORD) //
									|| !checkWeapons(activeChar, skinShield, WeaponType.FIST, WeaponType.FIST) //
									|| !checkWeapons(activeChar, skinShield, WeaponType.BIGBLUNT, WeaponType.BIGBLUNT))
								{
									activeChar.sendMessage("This skin is not suitable for your weapon type.");
									activeChar.sendPacket(new ExShowScreenMessage("This skin is not suitable for your weapon type.", 2000, 2, false));
									activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
									showSkinList(activeChar, type, page);
									return;
								}
							}
							break;
					}
					
					if (sp == null)
					{
						activeChar.sendMessage("There is no such skin.");
						activeChar.sendPacket(new ExShowScreenMessage("There is no such skin.", 2000, 2, false));
						activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						showSkinList(activeChar, type, page);
						return;
					}
					
					
					if (activeChar.destroyItemByItemId("dressme", sp.getPriceId(), sp.getPriceCount(), activeChar, true))
					{
						activeChar.sendMessage("You have successfully purchased " + sp.getName() + " skin.");
						activeChar.sendPacket(new ExShowScreenMessage("You have successfully purchased " + sp.getName() + " skin.", 2000, 2, false));
						
						switch (type.toLowerCase())
						{
							case "armor":
								activeChar.buyArmorSkin(skinId);
								activeChar.setArmorSkinOption(skinId);
								break;
							case "weapon":
								activeChar.buyWeaponSkin(skinId);
								activeChar.setWeaponSkinOption(skinId);
								break;
							case "hair":
								activeChar.buyHairSkin(skinId);
								activeChar.setHairSkinOption(skinId);
								break;
							case "face":
								activeChar.buyFaceSkin(skinId);
								activeChar.setFaceSkinOption(skinId);
								break;
							case "shield":
								activeChar.buyShieldSkin(skinId);
								activeChar.setShieldSkinOption(skinId);
								break;
						}
						
						activeChar.broadcastUserInfo();
					}
					showSkinList(activeChar, type, page);
				}
				else if (next.startsWith("tryskin"))
				{
					
					int skinId = Integer.parseInt(st.nextToken());
					
					String type = st.nextToken();
					
					if (activeChar.isTryingSkin())
					{
						activeChar.sendMessage("You are already trying a skin.");
						activeChar.sendPacket(new ExShowScreenMessage("You are already trying a skin.", 2000, 2, false));
						activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						showSkinList(activeChar, type, page);
						return;
					}
					
					activeChar.setIsTryingSkin(true);
					
					int oldArmorSkinId = activeChar.getArmorSkinOption();
					int oldWeaponSkinId = activeChar.getWeaponSkinOption();
					int oldHairSkinId = activeChar.getHairSkinOption();
					int oldFaceSkinId = activeChar.getFaceSkinOption();
					int oldShieldSkinId = activeChar.getShieldSkinOption();
					
					switch (type.toLowerCase())
					{
						case "armor":
							activeChar.setArmorSkinOption(skinId);
							break;
						case "weapon":
							activeChar.setWeaponSkinOption(skinId);
							break;
						case "hair":
							activeChar.setHairSkinOption(skinId);
							break;
						case "face":
							activeChar.setFaceSkinOption(skinId);
							break;
						case "shield":
							
							activeChar.setShieldSkinOption(skinId);
							
							break;
					}
					
					activeChar.broadcastUserInfo();
					showSkinList(activeChar, type, page);
					
					ThreadPool.schedule(() ->
					{
						switch (type.toLowerCase())
						{
							case "armor":
								activeChar.setArmorSkinOption(oldArmorSkinId);
								break;
							case "weapon":
								activeChar.setWeaponSkinOption(oldWeaponSkinId);
								break;
							case "hair":
								activeChar.setHairSkinOption(oldHairSkinId);
								break;
							case "face":
								activeChar.setFaceSkinOption(oldFaceSkinId);
								break;
							case "shield":
								activeChar.setShieldSkinOption(oldShieldSkinId);
								break;
						}
						
						activeChar.broadcastUserInfo();
						activeChar.setIsTryingSkin(false);
					}, 5000);
				}
				else if (next.startsWith("setskin"))
				{
					int id = Integer.parseInt(st.nextToken());
					String type = st.nextToken();
					int itemId = Integer.parseInt(st.nextToken());
					
					if (activeChar.isTryingSkin())
					{
						activeChar.sendMessage("You can't do this while trying skins.");
						activeChar.sendPacket(new ExShowScreenMessage("You can't do this while trying skins.", 2000, 2, false));
						activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						showMySkinList(activeChar, page);
						return;
					}
					
					if (type.toLowerCase().contains("armor") && activeChar.hasEquippedArmorSkin(String.valueOf(id)) || type.toLowerCase().contains("weapon") && activeChar.hasEquippedWeaponSkin(String.valueOf(id))
						|| type.toLowerCase().contains("hair") && activeChar.hasEquippedHairSkin(String.valueOf(id)) || type.toLowerCase().contains("face") && activeChar.hasEquippedFaceSkin(String.valueOf(id)))
					{
						activeChar.sendMessage("You are already equipped this skin.");
						activeChar.sendPacket(new ExShowScreenMessage("You are already equipped this skin.", 2000, 2, false));
						activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						showMySkinList(activeChar, page);
						return;
					}
					
					switch (type.toLowerCase())
					{
						case "armor":
							activeChar.setArmorSkinOption(id);
							break;
						case "weapon":
							if (activeChar.getActiveWeaponItem() == null)
							{
								activeChar.sendMessage("You can't use this skin without a weapon.");
								activeChar.sendPacket(new ExShowScreenMessage("You can't use this skin without a weapon.", 2000, 2, false));
								activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
								showMySkinList(activeChar, page);
								return;
							}
							
							ItemInstance skinWeapon = null;
							if (ItemTable.getInstance().getTemplate(itemId) != null)
							{
								skinWeapon = ItemTable.getInstance().createDummyItem(itemId);
								
								if (!checkWeapons(activeChar, skinWeapon, WeaponType.BOW, WeaponType.BOW) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.SWORD, WeaponType.SWORD) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.BLUNT, WeaponType.BLUNT) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.DAGGER, WeaponType.DAGGER) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.POLE, WeaponType.POLE) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.DUAL, WeaponType.DUAL) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.DUALFIST, WeaponType.DUALFIST) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.BIGSWORD, WeaponType.BIGSWORD) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.FIST, WeaponType.FIST) //
									|| !checkWeapons(activeChar, skinWeapon, WeaponType.BIGBLUNT, WeaponType.BIGBLUNT))
								{
									activeChar.sendMessage("This skin is not suitable for your weapon type.");
									activeChar.sendPacket(new ExShowScreenMessage("This skin is not suitable for your weapon type.", 2000, 2, false));
									activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
									showMySkinList(activeChar, page);
									return;
								}
								
								activeChar.setWeaponSkinOption(id);
							}
							break;
						case "hair":
							activeChar.setHairSkinOption(id);
							break;
						case "face":
							activeChar.setFaceSkinOption(id);
							break;
						case "shield":
							if (activeChar.getActiveWeaponItem() == null)
							{
								activeChar.sendMessage("You can't use this skin without a weapon.");
								activeChar.sendPacket(new ExShowScreenMessage("You can't use this skin without a weapon.", 2000, 2, false));
								activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
								showMySkinList(activeChar, page);
								return;
							}
							
							ItemInstance skinShield = null;
							if (ItemTable.getInstance().getTemplate(itemId) != null)
							{
								skinShield = ItemTable.getInstance().createDummyItem(itemId);
								
								if (!checkWeapons(activeChar, skinShield, WeaponType.BOW, WeaponType.BOW) //
									|| !checkWeapons(activeChar, skinShield, WeaponType.POLE, WeaponType.POLE) //
									|| !checkWeapons(activeChar, skinShield, WeaponType.DUAL, WeaponType.DUAL) //
									|| !checkWeapons(activeChar, skinShield, WeaponType.DUALFIST, WeaponType.DUALFIST) //
									|| !checkWeapons(activeChar, skinShield, WeaponType.BIGSWORD, WeaponType.BIGSWORD) //
									|| !checkWeapons(activeChar, skinShield, WeaponType.FIST, WeaponType.FIST) //
									|| !checkWeapons(activeChar, skinShield, WeaponType.BIGBLUNT, WeaponType.BIGBLUNT))
								{
									activeChar.sendMessage("This skin is not suitable for your weapon type.");
									activeChar.sendPacket(new ExShowScreenMessage("This skin is not suitable for your weapon type.", 2000, 2, false));
									activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
									showMySkinList(activeChar, page);
									return;
								}
								
								activeChar.setShieldSkinOption(id);
							}
							
							break;
					}
					
					activeChar.broadcastUserInfo();
					showMySkinList(activeChar, page);
				}
	
			
			}	
			// Dressme ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
		}
		
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Bad RequestBypassToServer: " + e, e);
		}
	}
	
	// Dressme --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	public static String getItemNameById(int itemId)
	{
		Item item = ItemTable.getInstance().getTemplate(itemId);
		
		String itemName = "NoName";
		
		if (itemId != 0)
		{
			itemName = item.getName();
		}
		
		return itemName;
	}
	
	
	public static void showDressMeMainPage(Player player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(1);
		String text = HtmCache.getInstance().getHtm("data/html/dressme/index.htm");
		
		htm.setHtml(text);
		
		{
			htm.replace("%time%", sdf.format(new Date(System.currentTimeMillis())));
			htm.replace("%dat%", (new SimpleDateFormat("dd/MM/yyyy")).format(new Date(System.currentTimeMillis())));
			
		}
		
		player.sendPacket(htm);
	}
	
	static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
	
	private static void showSkinList(Player player, String type, int page)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		
		html.setFile("data/html/dressme/allskins.htm");
		
		html.replace("%time%", sdf.format(new Date(System.currentTimeMillis())));
		html.replace("%dat%", (new SimpleDateFormat("dd/MM/yyyy")).format(new Date(System.currentTimeMillis())));
		
		final int ITEMS_PER_PAGE = 8;
		
		int myPage = 1;
		int i = 0;
		int shown = 0;
		boolean hasMore = false;
		int itemId = 0;
		
		final StringBuilder sb = new StringBuilder();
		
		List<SkinPackage> tempList = null;
		switch (type.toLowerCase())
		{
			case "armor":
				tempList = DressMeData.getInstance().getArmorSkinOptions().values().stream().filter(s -> !player.hasArmorSkin(s.getId())).collect(Collectors.toList());
				break;
			case "weapon":
				tempList = DressMeData.getInstance().getWeaponSkinOptions().values().stream().filter(s -> !player.hasWeaponSkin(s.getId())).collect(Collectors.toList());
				break;
			case "hair":
				tempList = DressMeData.getInstance().getHairSkinOptions().values().stream().filter(s -> !player.hasHairSkin(s.getId())).collect(Collectors.toList());
				break;
			case "face":
				tempList = DressMeData.getInstance().getFaceSkinOptions().values().stream().filter(s -> !player.hasFaceSkin(s.getId())).collect(Collectors.toList());
				break;
			case "shield":
				tempList = DressMeData.getInstance().getShieldSkinOptions().values().stream().filter(s -> !player.hasShieldSkin(s.getId())).collect(Collectors.toList());
				break;
		}
		
		if (tempList != null && !tempList.isEmpty())
		{
			for (SkinPackage sp : tempList)
			{
				if (sp == null)
				{
					continue;
				}
				
				if (shown == ITEMS_PER_PAGE)
				{
					hasMore = true;
					break;
				}
				
				if (myPage != page)
				{
					i++;
					if (i == ITEMS_PER_PAGE)
					{
						myPage++;
						i = 0;
					}
					continue;
				}
				
				if (shown == ITEMS_PER_PAGE)
				{
					hasMore = true;
					break;
				}
				
				switch (type.toLowerCase())
				{
					case "armor":
						itemId = sp.getChestId();
						break;
					case "weapon":
						itemId = sp.getWeaponId();
						break;
					case "hair":
						itemId = sp.getHairId();
						break;
					case "face":
						itemId = sp.getFaceId();
						break;
					case "shield":
						itemId = sp.getShieldId();
						break;
				}
				
				sb.append("<table border=0 cellspacing=0 cellpadding=2 height=36><tr>");
				sb.append("<td width=32 align=center>" + "<button width=32 height=32 back=" + Item.getItemIcon(itemId) + " fore=" + Item.getItemIcon(itemId) + ">" + "</td>");
				sb.append("<td width=124>" + sp.getName() + "<br1> <font color=999999>Price:</font> <font color=339966>" + Item.getItemNameById(sp.getPriceId()) + "</font> (<font color=LEVEL>" + sp.getPriceCount() + "</font>)</td>");
				sb.append("<td align=center width=65>" + "<button value=\"Buy\" action=\"bypass -h dressme " + page + " buyskin  " + sp.getId() + " " + type + " " + itemId + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" + "</td>");
				sb.append("<td align=center width=65>" + "<button value=\"Try\" action=\"bypass -h dressme " + page + " tryskin  " + sp.getId() + " " + type + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" + "</td>");
				
				sb.append("</tr></table>");
				sb.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
				shown++;
			}
		}
		
		sb.append("<table width=300><tr>");
		sb.append("<td align=center width=70>" + (page > 1 ? "<button value=\"< PREV\" action=\"bypass -h dressme " + (page - 1) + " skinlist " + type + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("<td align=center width=140>Page: " + page + "</td>");
		sb.append("<td align=center width=70>" + (hasMore ? "<button value=\"NEXT >\" action=\"bypass -h dressme " + (page + 1) + " skinlist " + type + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("</tr></table>");
		
		html.replace("%showList%", sb.toString());
		player.sendPacket(html);
	}
	
	private static void showMySkinList(Player player, int page)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile("data/html/dressme/myskins.htm");
		
		html.replace("%time%", sdf.format(new Date(System.currentTimeMillis())));
		html.replace("%dat%", (new SimpleDateFormat("dd/MM/yyyy")).format(new Date(System.currentTimeMillis())));
		
		final int ITEMS_PER_PAGE = 8;
		int itemId = 0;
		
		int myPage = 1;
		int i = 0;
		int shown = 0;
		boolean hasMore = false;
		
		final StringBuilder sb = new StringBuilder();
		
		List<SkinPackage> armors = DressMeData.getInstance().getArmorSkinOptions().values().stream().filter(s -> player.hasArmorSkin(s.getId())).collect(Collectors.toList());
		List<SkinPackage> weapons = DressMeData.getInstance().getWeaponSkinOptions().values().stream().filter(s -> player.hasWeaponSkin(s.getId())).collect(Collectors.toList());
		List<SkinPackage> hairs = DressMeData.getInstance().getHairSkinOptions().values().stream().filter(s -> player.hasHairSkin(s.getId())).collect(Collectors.toList());
		// List<SkinPackage> faces = DressMeData.getInstance().getFaceSkinOptions().values().stream().filter(s -> player.hasFaceSkin(s.getId())).collect(Collectors.toList());
		List<SkinPackage> shield = DressMeData.getInstance().getShieldSkinOptions().values().stream().filter(s -> player.hasShieldSkin(s.getId())).collect(Collectors.toList());
		
		List<SkinPackage> list = Stream.concat(armors.stream(), weapons.stream()).collect(Collectors.toList());
		shield.stream().collect(Collectors.toList());
		List<SkinPackage> list2 = Stream.concat(hairs.stream(), shield.stream()).collect(Collectors.toList());
		
		List<SkinPackage> allLists = Stream.concat(list.stream(), list2.stream()).collect(Collectors.toList());
		
		if (!allLists.isEmpty())
		{
			for (SkinPackage sp : allLists)
			{
				if (sp == null)
				{
					continue;
				}
				
				if (shown == ITEMS_PER_PAGE)
				{
					hasMore = true;
					break;
				}
				
				if (myPage != page)
				{
					i++;
					if (i == ITEMS_PER_PAGE)
					{
						myPage++;
						i = 0;
					}
					continue;
				}
				
				if (shown == ITEMS_PER_PAGE)
				{
					hasMore = true;
					break;
				}
				
				switch (sp.getType().toLowerCase())
				{
					case "armor":
						itemId = sp.getChestId();
						break;
					case "weapon":
						itemId = sp.getWeaponId();
						break;
					case "hair":
						itemId = sp.getHairId();
						break;
					case "face":
						itemId = sp.getFaceId();
						break;
					case "shield":
						itemId = sp.getShieldId();
						break;
				}
				
				sb.append("<table border=0 cellspacing=0 cellpadding=2 height=36><tr>");
				sb.append("<td width=32 align=center>" + "<button width=32 height=32 back=" + Item.getItemIcon(itemId) + " fore=" + Item.getItemIcon(itemId) + ">" + "</td>");
				sb.append("<td width=124>" + sp.getName() + "</td>");
				sb.append("<td align=center width=65>" + "<button value=\"Equip\" action=\"bypass -h dressme " + page + " setskin " + sp.getId() + " " + sp.getType() + " " + itemId + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" + "</td>");
				sb.append("<td align=center width=65>" + "<button value=\"Remove\" action=\"bypass -h dressme " + page + " clean " + sp.getType() + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" + "</td>");
				sb.append("</tr></table>");
				sb.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
				shown++;
			}
		}
		
		sb.append("<table width=300><tr>");
		sb.append("<td align=center width=70>" + (page > 1 ? "<button value=\"< PREV\" action=\"bypass -h dressme " + (page - 1) + " myskinlist\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("<td align=center width=140>Page: " + page + "</td>");
		sb.append("<td align=center width=70>" + (hasMore ? "<button value=\"NEXT >\" action=\"bypass -h dressme " + (page + 1) + " myskinlist\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("</tr></table>");
		
		html.replace("%showList%", sb.toString());
		player.sendPacket(html);
	}
	
	public static boolean checkWeapons(Player player, ItemInstance skin, WeaponType weapon1, WeaponType weapon2)
	{
		if (player.getActiveWeaponItem().getItemType() == weapon1 && skin.getItem().getItemType() != weapon2)
		{
			return false;
		}
		
		return true;
	}			
	// Dressme --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	
	private static void playerHelp(Player activeChar, String path)
	{
		if (path.indexOf("..") != -1)
			return;
		
		final StringTokenizer st = new StringTokenizer(path);
		final String[] cmd = st.nextToken().split("#");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/help/" + cmd[0]);
		if (cmd.length > 1)
			html.setItemId(Integer.parseInt(cmd[1]));
		html.disableValidation();
		activeChar.sendPacket(html);
	}
	
	// Auto Farm
	private static final String ACTIVED = "<font color=00FF00>STARTED</font>";
	private static final String DESATIVED = "<font color=FF0000>STOPPED</font>";
	private static final String STOP = "STOP";
	private static final String START = "START";
	
	public static void showAutoFarm(Player activeChar)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/mods/menu/AutoFarm.htm");
		html.replace("%player%", activeChar.getName());
		html.replace("%page%", StringUtil.formatNumber(activeChar.getPage() + 1));
		html.replace("%heal%", StringUtil.formatNumber(activeChar.getHealPercent()));
		html.replace("%radius%", StringUtil.formatNumber(activeChar.getRadius()));
		html.replace("%summonSkill%", StringUtil.formatNumber(activeChar.getSummonSkillPercent()));
		html.replace("%hpPotion%", StringUtil.formatNumber(activeChar.getHpPotionPercentage()));
		html.replace("%mpPotion%", StringUtil.formatNumber(activeChar.getMpPotionPercentage()));
		html.replace("%noBuff%", activeChar.isNoBuffProtected() ? "back=L2UI.CheckBox_checked fore=L2UI.CheckBox_checked" : "back=L2UI.CheckBox fore=L2UI.CheckBox");
		html.replace("%summonAtk%", activeChar.isSummonAttack() ? "back=L2UI.CheckBox_checked fore=L2UI.CheckBox_checked" : "back=L2UI.CheckBox fore=L2UI.CheckBox");
		html.replace("%antiKs%", activeChar.isAntiKsProtected() ? "back=L2UI.CheckBox_checked fore=L2UI.CheckBox_checked" : "back=L2UI.CheckBox fore=L2UI.CheckBox");
		html.replace("%autofarm%", activeChar.isAutoFarm() ? ACTIVED : DESATIVED);
		html.replace("%button%", activeChar.isAutoFarm() ? STOP : START);
		activeChar.sendPacket(html);
	}
	
	
}