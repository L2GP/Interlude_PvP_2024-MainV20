package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**

*

* @author Bluur

* @version 1.0

*/



public class Menu implements IVoicedCommandHandler

{

   private static final String[] _voicedCommands =

   {

       "menu",

       "setPartyRefuse",

       "setTradeRefuse",    

       "setbuffsRefuse",

       "setMessageRefuse",            
       
       "setXpGain", // Nueva opción para XP

   };

   

   private static final String ACTIVED = "<font color=00FF00>ON</font>";

   private static final String DESATIVED = "<font color=FF0000>OFF</font>";

   

   @Override

   public boolean useVoicedCommand(String command, Player activeChar, String target)

   {

       if (command.equals("menu"))

           showHtml(activeChar);                
     
       
       else if (command.equals("setPartyRefuse"))

       {

           if (activeChar.isPartyInRefuse())

               activeChar.setIsPartyInRefuse(false);

           else

               activeChar.setIsPartyInRefuse(true);            

           showHtml(activeChar);

       }    

       else if (command.equals("setTradeRefuse"))

       {

           if (activeChar.getTradeRefusal())

               activeChar.setTradeRefusal(false);

           else

               activeChar.setTradeRefusal(true);

           showHtml(activeChar);

       }        

       else if (command.equals("setMessageRefuse"))

       {        

           if (activeChar.isInRefusalMode())

               activeChar.setInRefusalMode(false);

           else

               activeChar.setInRefusalMode(true);

           showHtml(activeChar);

       }

       else if (command.equals("setbuffsRefuse"))

       {        

           if (activeChar.isBuffProtected())

               activeChar.setIsBuffProtected(false);

           else

               activeChar.setIsBuffProtected(true);

           showHtml(activeChar);

       }  
       
    // Nueva opción para habilitar o deshabilitar ganancia de XP/SP
       else if (command.equals("setXpGain"))
       {
           activeChar.setGainXpSpEnable(!activeChar.getGainXpSpEnable());
           showHtml(activeChar);          
           
       }

       return true;

   }

   

   private static void showHtml(Player activeChar)

   {

       NpcHtmlMessage html = new NpcHtmlMessage(0);

       html.setFile("data/html/mods/menu/Menu.htm");
   
       html.replace("%partyRefusal%", activeChar.isPartyInRefuse() ? ACTIVED : DESATIVED);

       html.replace("%tradeRefusal%", activeChar.getTradeRefusal() ? ACTIVED : DESATIVED);

       html.replace("%buffsRefusal%", activeChar.isBuffProtected() ? ACTIVED : DESATIVED);

       html.replace("%messageRefusal%", activeChar.isInRefusalMode() ? ACTIVED : DESATIVED);        
       
       html.replace("%xpGain%", activeChar.getGainXpSpEnable() ? ACTIVED : DESATIVED);// Mostrar estado de ganancia de XP/SP
    
       activeChar.sendPacket(html);

   }

  

   @Override

   public String[] getVoicedCommandList()

   {

       return _voicedCommands;

   }

}
