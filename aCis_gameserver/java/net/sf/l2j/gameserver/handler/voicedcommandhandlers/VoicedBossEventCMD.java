package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.gameserver.events.BossEvent;
import net.sf.l2j.gameserver.events.BossEvent.EventState;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;

public class VoicedBossEventCMD implements IVoicedCommandHandler 
{	
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		if (command.startsWith("bossevent"))
		{
			if (BossEvent.getInstance().getState() != EventState.REGISTRATION)
			{
				activeChar.sendMessage("Boss Event is not running!");
				return false;
			}
			if (!BossEvent.getInstance().isRegistered(activeChar))
			{
				if (BossEvent.getInstance().addPlayer(activeChar))
				{
					activeChar.sendMessage("You have been successfully registered in Boss Event!");
				}
				
			}
			else
			{
				if (BossEvent.getInstance().removePlayer(activeChar))
				{
					activeChar.sendMessage("You have been successfully removed of Boss Event!");
				}
			}
		}
		return false;
	}
	
	
	public String[] getVoicedCommandList()
	{
		
		return new String[]
			{
				"bossevent"
			};
	}	

}
