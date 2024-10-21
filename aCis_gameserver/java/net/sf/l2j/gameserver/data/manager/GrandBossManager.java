/*package net.sf.l2j.gameserver.data.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.templates.StatsSet;

import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.L2DatabaseFactory;

public class GrandBossManager
{
	protected static final Logger LOGGER = Logger.getLogger(GrandBossManager.class.getName());
	
	
	private static final String SELECT_GRAND_BOSS_DATA = "SELECT * from grandboss_data ORDER BY boss_id";
	private static final String UPDATE_GRAND_BOSS_DATA = "UPDATE grandboss_data set loc_x = ?, loc_y = ?, loc_z = ?, heading = ?, respawn_time = ?, currentHP = ?, currentMP = ?, status = ? where boss_id = ?";
	private static final String UPDATE_GRAND_BOSS_DATA2 = "UPDATE grandboss_data set status = ? where boss_id = ?";
	
	private final Map<Integer, GrandBoss> _bosses = new HashMap<>();
	private final Map<Integer, StatsSet> _sets = new HashMap<>();
	private final Map<Integer, Integer> _bossStatus = new HashMap<>();
	protected final Map<Integer, Long> _bossesTime = new HashMap<>();
	
	protected GrandBossManager()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement ps = con.prepareStatement(SELECT_GRAND_BOSS_DATA);
			ResultSet rset = ps.executeQuery();
			
		
			while (rset.next())
			{
				final int bossId = rset.getInt("boss_id");
				
				final StatsSet set = new StatsSet();
				set.set("loc_x", rset.getInt("loc_x"));
				set.set("loc_y", rset.getInt("loc_y"));
				set.set("loc_z", rset.getInt("loc_z"));
				set.set("heading", rset.getInt("heading"));
				set.set("respawn_time", rset.getLong("respawn_time"));
				set.set("currentHP", rset.getDouble("currentHP"));
				set.set("currentMP", rset.getDouble("currentMP"));
				_bossesTime.put(rset.getInt("boss_id"), rset.getLong("respawn_time"));
				_bossStatus.put(bossId, rset.getInt("status"));
				_sets.put(bossId, set);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING,"Couldn't load grandboss.", e);
		}
		LOGGER.info("Loaded {} GrandBosses instances.");
	}
	
	public int getBossStatus(int bossId)
	{
		return _bossStatus.get(bossId);
	}
	
	public void setBossStatus(int bossId, int status)
	{
		_bossStatus.put(bossId, status);
		_bossesTime.put(bossId, 0L);
		LOGGER.log(Level.WARNING,"Updated {} (id: {}) status to {}.", Npc.getInstance().getTemplate(bossId).getName(), bossId, status);
		
		updateDb(bossId, true);
	}
	
	
	public void addBoss(GrandBoss boss)
	{
		if (boss != null)
			_bosses.put(boss.getNpcId(), boss);
		
	}
	
	
	public void addBoss(int npcId, GrandBoss boss)
	{
		if (boss != null)
			_bosses.put(npcId, boss);
	}
	
	public GrandBoss getBoss(int bossId)
	{
		return _bosses.get(bossId);
	}
	
	public StatsSet getStatSet(int bossId)
	{
		return _sets.get(bossId);
	}
	
	public void setStatSet(int bossId, StatsSet info)
	{
		_sets.put(bossId, info);
		
		updateDb(bossId, false);
	}
	
	public long getRespawnTime(int id)
	{
		return _bossStatus.containsKey(id) ? _bossStatus.get(id) : -1;
	}
	
	
	private void updateDb(int bossId, boolean statusOnly)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			final StatsSet set = _sets.get(bossId);
			final GrandBoss boss = _bosses.get(bossId);
			
			if (statusOnly || boss == null || set == null)
			{
				try (PreparedStatement ps = con.prepareStatement(UPDATE_GRAND_BOSS_DATA2))
				{
					ps.setInt(1, _bossStatus.get(bossId));
					ps.setInt(2, bossId);
					ps.executeUpdate();
				}
			}
			else
			{
				try (PreparedStatement ps = con.prepareStatement(UPDATE_GRAND_BOSS_DATA))
				{
					ps.setInt(1, boss.getX());
					ps.setInt(2, boss.getY());
					ps.setInt(3, boss.getZ());
					ps.setInt(4, boss.getHeading());
					ps.setLong(5, set.getLong("respawn_time"));
					ps.setDouble(6, (boss.isDead()) ? boss.getStatus().getMaxHp() : boss.getStatus().getHp());
					ps.setDouble(7, (boss.isDead()) ? boss.getStatus().getMaxMp() : boss.getStatus().getMp());
					ps.setInt(8, _bossStatus.get(bossId));
					ps.setInt(9, bossId);
					ps.executeUpdate();
					
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING,"Couldn't update grandbosses.", e);
		}
	}
	
	
	public void cleanUp()
	{
		// Store to database.
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement ps1 = con.prepareStatement(UPDATE_GRAND_BOSS_DATA2);
			PreparedStatement ps2 = con.prepareStatement(UPDATE_GRAND_BOSS_DATA);
		{
			for (Map.Entry<Integer, StatsSet> entry : _sets.entrySet())
			{
				final int bossId = entry.getKey();
				final StatsSet set = entry.getValue();
				final GrandBoss boss = _bosses.get(bossId);
				
				if (boss == null || set == null)
				{
					ps1.setInt(1, _bossStatus.get(bossId));
					ps1.setInt(2, bossId);
					ps1.addBatch();
					
				}
				else
				{
					ps2.setInt(1, boss.getX());
					ps2.setInt(2, boss.getY());
					ps2.setInt(3, boss.getZ());
					ps2.setInt(4, boss.getHeading());
					ps2.setLong(5, set.getLong("respawn_time"));
					ps2.setDouble(6, (boss.isDead()) ? boss.getStatus().getMaxHp() : boss.getStatus().getHp());
					ps2.setDouble(7, (boss.isDead()) ? boss.getStatus().getMaxMp() : boss.getStatus().getMp());
					ps2.setInt(8, _bossStatus.get(bossId));
					ps2.setInt(9, bossId);
					ps2.addBatch();
					
					_bossesTime.remove(bossId);
				}
			}
			ps1.executeBatch();
			ps2.executeBatch();
		}
	}	
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING ,"Couldn't store grandbosses.", e);
		}
		
		_bosses.clear();
		_sets.clear();
		_bossStatus.clear();
	}
	
	public void putRespawnTimer(int bossId, long time)
	{
		_bossesTime.put(bossId, time);
	}
	
	public long getRespawnTimeinfo(int id)
	{
		return _bossesTime.containsKey(id) ? _bossesTime.get(id) : -1;
	}
	
	public List<Integer> getTimerList()
	{
		List<Integer> list = new ArrayList<>();
		for (Entry<Integer, Long> entry : _bossesTime.entrySet())
			list.add(entry.getKey());
		
		return list;
	}
	
	public static GrandBossManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GrandBossManager INSTANCE = new GrandBossManager();
	}

}*/
