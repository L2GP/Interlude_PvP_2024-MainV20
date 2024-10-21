package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.gameserver.events.eventengine.EventManager;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;


/**
 * @author Anarchy
 *
 */
public class EventCommands implements IVoicedCommandHandler
{
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (command.equals("register"))
			EventManager.getInstance().registerPlayer(activeChar);
		else if (command.equals("leave"))
			EventManager.getInstance().removePlayer(activeChar);
		
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return new String[] { "register", "leave" };
	}	
}
