package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.events.eventengine.EventManager;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Anarchy
 *
 */
public class EventManagerNpc extends Folk
{
	public EventManagerNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.equals("register"))
			EventManager.getInstance().registerPlayer(player);
		else if (command.equals("remove"))
			EventManager.getInstance().removePlayer(player);
		else
			super.onBypassFeedback(player, command);
	}
	
	private static String getActiveEvent()
	{
		String s = "---";
		
		if (EventManager.getInstance().getActiveEvent() != null)
			s = EventManager.getInstance().getActiveEvent().getName();
		
		return s;
	}
	
	private static int getCurrentReg()
	{
		int i = 0;
		
		if (EventManager.getInstance().getActiveEvent() != null)
			i = EventManager.getInstance().getTotalParticipants();
		
		return i;
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(getObjectId());
		htm.setFile("data/html/mods/eventmanager/"+getNpcId()+(val == 0 ? "" : "-"+val)+".htm");
		
		htm.replace("%activeevent%", getActiveEvent());
		htm.replace("%currentreg%", getCurrentReg());
		
		htm.replace("%objectId%", getObjectId()+"");
		
		player.sendPacket(htm);
	}
	
    @Override
	public String getHtmlPath(int npcId, int val)
    {
        String pom = "";
        if (val == 0)
            pom = "" + npcId;
        else
            pom = npcId + "-" + val;
                
        return "data/html/mods/eventmanager/" + pom + ".htm";
    }
}
