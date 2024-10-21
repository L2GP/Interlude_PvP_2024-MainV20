package net.sf.l2j.gameserver.events;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;

public class NextBossEvent
{
	   private static NextBossEvent _instance = null;
	   protected static final Logger _log = Logger.getLogger(NextBossEvent.class.getName());
	   private Calendar nextEvent;
	   private final SimpleDateFormat format = new SimpleDateFormat("HH:mm");
	   public ScheduledFuture<?> task = null;
	   
	   public static NextBossEvent getInstance()
	   {
	       if (_instance == null)
	       {
	           _instance = new NextBossEvent();
	       }
	       return _instance;
	   }
	   
	   public String getNextTime()
	   {
	       if (nextEvent.getTime() != null)
	       {
	           return format.format(nextEvent.getTime());
	       }
	       return "Erro";
	   }
	   
	   public void startCalculationOfNextEventTime()
	   {
	       try
	       {
	           Calendar currentTime = Calendar.getInstance();
	           Calendar testStartTime = null;
	           long flush2 = 0L;
	           long timeL = 0L;
	           int count = 0;
	           for (String timeOfDay : Config.BOSS_EVENT_BY_TIME_OF_DAY)
	           {
	               testStartTime = Calendar.getInstance();
	               testStartTime.setLenient(true);
	               String[] splitTimeOfDay = timeOfDay.split(":");
	               testStartTime.set(11, Integer.parseInt(splitTimeOfDay[0]));
	               testStartTime.set(12, Integer.parseInt(splitTimeOfDay[1]));
	               testStartTime.set(13, 0);
	               if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
	               {
	                   testStartTime.add(5, 1);
	               }
	               timeL = testStartTime.getTimeInMillis() - currentTime.getTimeInMillis();
	               if (count == 0)
	               {
	                   flush2 = timeL;
	                   nextEvent = testStartTime;
	               }
	               if (timeL < flush2)
	               {
	                   flush2 = timeL;
	                   nextEvent = testStartTime;
	               }
	               count++;
	           }
	           _log.info("[Boss Event]: Next Event Time -> " + nextEvent.getTime().toString());
	           ThreadPool.schedule(new StartEventTask(), flush2);
	       }
	       catch (Exception e)
	       {
	           System.out.println("[Boss Event]: " + e);
	       }
	   }
	   
	   class StartEventTask implements Runnable
	   {
	       StartEventTask()
	       {
	       }
	       
	       @Override
	       public void run()
	       {
	           NextBossEvent._log.info("----------------------------------------------------------------------------");
	           NextBossEvent._log.info("[Boss Event]: Event Started.");
	           NextBossEvent._log.info("----------------------------------------------------------------------------");
	           BossEvent.getInstance().startRegistration();
	       }
	   }	

}
