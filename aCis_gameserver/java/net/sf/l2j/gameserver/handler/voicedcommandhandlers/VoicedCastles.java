package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SiegeInfo;

public class VoicedCastles implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"castlemanager",
		"siege_gludio",
		"siege_dion",
		"siege_giran",
		"siege_oren",
		"siege_aden",
		"siege_innadril",
		"siege_goddard",
		"siege_rune",
		"siege_schuttgart"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (command.startsWith("castlemanager"))
			sendHtml(activeChar);
		if (command.startsWith("siege_"))
		{
			if (activeChar.getClan() != null && !activeChar.isClanLeader())
			{
				activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return false;
			}
			
			int castleId = 0;
			if (command.startsWith("siege_gludio") && Config.SIEGE_GLUDIO)
				castleId = 1;
			else if (command.startsWith("siege_dion") && Config.SIEGE_DION)
				castleId = 2;
			else if (command.startsWith("siege_giran") && Config.SIEGE_GIRAN)
				castleId = 3;
			else if (command.startsWith("siege_oren") && Config.SIEGE_OREN)
				castleId = 4;
			else if (command.startsWith("siege_aden") && Config.SIEGE_ADEN)
				castleId = 5;
			else if (command.startsWith("siege_innadril") && Config.SIEGE_INNADRIL)
				castleId = 6;
			else if (command.startsWith("siege_goddard") && Config.SIEGE_GODDARD)
				castleId = 7;
			else if (command.startsWith("siege_rune") && Config.SIEGE_RUNE)
				castleId = 8;
			else if (command.startsWith("siege_schuttgart") && Config.SIEGE_SCHUT)
				castleId = 9;
			else
				activeChar.sendMessage("This Castle has been disabled");
			
			Castle castle = CastleManager.getInstance().getCastleById(castleId);
			if ((castle != null) && (castleId != 0))
				activeChar.sendPacket(new SiegeInfo(castle));
		}
		return true;
	}
	
	private static void sendHtml(Player activeChar)
	{
		String htmFile = "data/html/mods/menu/CastleManager.htm";
		NpcHtmlMessage msg = new NpcHtmlMessage(5);
		msg.setFile(htmFile);
		activeChar.sendPacket(msg);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
