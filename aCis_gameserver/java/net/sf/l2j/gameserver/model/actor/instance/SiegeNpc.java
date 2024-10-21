package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SiegeInfo;

public class SiegeNpc extends Folk
{
	public SiegeNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		int castleId = getCastle().getCastleId();
		
		// Verificamos si el castillo tiene el siege habilitado en la configuración
		if (!isSiegeEnabled(castleId))
		{
			player.sendMessage("The siege for this castle is disabled.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Si el asedio no está en progreso, mostramos la información del castillo
		if (!getCastle().getSiege().isInProgress())
		{
			player.sendPacket(new SiegeInfo(getCastle()));
		}
		else
		{
			// Si el asedio está en progreso, mostramos un HTML indicando que está ocupado
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/siege/" + getNpcId() + "-busy.htm");
			html.replace("%castlename%", getCastle().getName());
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	// Método para verificar si el asedio está habilitado para un castillo en la configuración
	private boolean isSiegeEnabled(int castleId)
	{
		switch (castleId)
		{
			case 1: return Config.SIEGE_GLUDIO;
			case 2: return Config.SIEGE_DION;
			case 3: return Config.SIEGE_GIRAN;
			case 4: return Config.SIEGE_OREN;
			case 5: return Config.SIEGE_ADEN;
			case 6: return Config.SIEGE_INNADRIL;
			case 7: return Config.SIEGE_GODDARD;
			case 8: return Config.SIEGE_RUNE;
			case 9: return Config.SIEGE_SCHUT;
			default: return false;
		}
	}
}