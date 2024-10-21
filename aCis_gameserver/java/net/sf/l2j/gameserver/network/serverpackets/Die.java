package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.events.eventengine.EventListener;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.Siege.SiegeSide;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.zone.ZoneId;

import mods.autofarm.AutofarmPlayerRoutine;

public class Die extends L2GameServerPacket
{
	private final Creature _activeChar;
	private final int _charObjId;
	private final boolean _fake;
	
	private boolean _sweepable;
	private boolean _allowFixedRes;
	private Clan _clan;
	private boolean _canTeleport;
	
	public Die(Creature cha)
	{
		_activeChar = cha;
		_charObjId = cha.getObjectId();
		_fake = !cha.isDead();
		
		if (cha instanceof Player)
		{
			Player player = (Player) cha;
			final AutofarmPlayerRoutine bot = player.getBot();
			_allowFixedRes = player.getAccessLevel().allowFixedRes();
			_clan = player.getClan();
			_canTeleport = !(player.isInsideZone(ZoneId.MULTI_FUNCTION) && Config.REVIVE);
			
			if (player.isAutoFarm())
	    	{
	    		bot.stop();
	    		player.setAutoFarm(false);
	    	}
		}
		else if (cha instanceof Attackable)
			_sweepable = ((Attackable) cha).isSpoiled();
	}
	
	@Override
	protected final void writeImpl()
	{
		if (_fake)
			return;
		
		writeC(0x06);
		writeD(_charObjId);		
		
		if (_activeChar instanceof Player && (!EventListener.allowDiePacket((Player)_activeChar)))
		{
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			return;
		}
		
		//writeD(0x01); // to nearest village
		writeD(_canTeleport ? 0x01 : 0); // to nearest village
		
		if (_canTeleport && _clan != null)
		{
			SiegeSide side = null;
			
			final Siege siege = CastleManager.getInstance().getActiveSiege(_activeChar);
			if (siege != null)
				side = siege.getSide(_clan);
			
			writeD((_clan.hasHideout()) ? 0x01 : 0x00); // to clanhall
			writeD((_clan.hasCastle() || side == SiegeSide.OWNER || side == SiegeSide.DEFENDER) ? 0x01 : 0x00); // to castle
			writeD((side == SiegeSide.ATTACKER && _clan.getFlag() != null) ? 0x01 : 0x00); // to siege HQ
		}
		else
		{
			writeD(0x00); // to clanhall
			writeD(0x00); // to castle
			writeD(0x00); // to siege HQ
		}
		
		writeD((_sweepable) ? 0x01 : 0x00); // sweepable (blue glow)
		writeD((_allowFixedRes) ? 0x01 : 0x00); // FIXED
	}
}