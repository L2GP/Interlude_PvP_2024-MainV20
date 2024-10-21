package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.zone.L2SpawnZone;
import net.sf.l2j.gameserver.model.zone.ZoneId;

public class L2TournamentZone extends L2SpawnZone
{
	private boolean _isPeaceZone = true;
	
	public L2TournamentZone(int id)
	{
		super(id);
	}

	@Override
	protected void onEnter(Creature character)
	{
        if (_isPeaceZone && Config.ZONE_TOWN != 2) {
            character.setInsideZone(ZoneId.PEACE, true);
          
        }
        character.setInsideZone(ZoneId.TOWN, true);
        if (character instanceof Player) {
            Player player = (Player)character;
            player.setTournamentTeleport(true);
            if (!player.isArenaProtection()) {
                player.sendMessage("You have entered a Tournament zone.");
                player.getAppearance().setVisible();
                
                player.broadcastUserInfo();
            }
        }
    }

	@Override
	protected void onExit(Creature character)
	{
        if (_isPeaceZone) {
            character.setInsideZone(ZoneId.PEACE, false);
        }
        
        if (character instanceof Player) {
            Player player = (Player)character;
            player.setTournamentTeleport(false);
            if (!player.isArenaProtection() || player.isGM() || player.isInObserverMode()) {
               
            	player.getAppearance().setInvisible();
                player.broadcastUserInfo();
            }
            
            player.sendMessage("You left a Tournament zone.");
            player.broadcastUserInfo();
        }
        
	}

	@Override
	public void onDieInside(Creature character)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReviveInside(Creature character)
	{
		// TODO Auto-generated method stub
		
	}
}
