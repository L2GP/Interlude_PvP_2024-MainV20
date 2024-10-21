/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.handler;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.handler.voicedcommandhandlers.DressMe;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.EventCommands;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedDropList;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.Menu;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedRaidInfo;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedAutoFarm;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedCastles;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedStatus;

public class VoicedCommandHandler
{
	private final Map<Integer, IVoicedCommandHandler> _datatable = new HashMap<>();

	public static VoicedCommandHandler getInstance()
	{
		return SingletonHolder._instance;
	}

	protected VoicedCommandHandler()
	{
		registerHandler(new DressMe());
		registerHandler(new EventCommands());
		registerHandler(new VoicedAutoFarm());
		registerHandler(new Menu());
		registerHandler(new VoicedRaidInfo());
		registerHandler(new VoicedDropList());
		registerHandler(new VoicedStatus());
		registerHandler(new VoicedCastles());
	}
	public void registerHandler(IVoicedCommandHandler handler)
	{
		String[] ids = handler.getVoicedCommandList();

		for (String id : ids)
			_datatable.put(id.hashCode(), handler);
	}

	public IVoicedCommandHandler getHandler(String voicedCommand)
	{
		String command = voicedCommand;

		if (voicedCommand.indexOf(" ") != -1)
			command = voicedCommand.substring(0, voicedCommand.indexOf(" "));

		return _datatable.get(command.hashCode());
	}

	public int size()
	{
		return _datatable.size();
	}

	private static class SingletonHolder
	{
		protected static final VoicedCommandHandler _instance = new VoicedCommandHandler();
	}
}