package net.sf.l2j.gameserver.model.pledge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.math.MathUtil;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.CrestCache;
import net.sf.l2j.gameserver.cache.CrestCache.CrestType;
import net.sf.l2j.gameserver.communitybbs.BB.Forum;
import net.sf.l2j.gameserver.communitybbs.Manager.ForumsBBSManager;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Player.TimeStamp;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.Siege.SiegeSide;
import net.sf.l2j.gameserver.model.itemcontainer.ClanWarehouse;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.PledgeReceiveSubPledgeCreated;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListDeleteAll;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PledgeSkillList;
import net.sf.l2j.gameserver.network.serverpackets.PledgeSkillListAdd;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;

public class Clan
{
	private static final Logger _log = Logger.getLogger(Clan.class.getName());
	
	private String _name;
	private int _clanId;
	private ClanMember _leader;
	private final Map<Integer, ClanMember> _members = new HashMap<>();
	
	private String _allyName;
	private int _allyId;
	private int _level;
	private int _castleId;
	private int _hideoutId;
	private int _hiredGuards;
	private int _crestId;
	private int _crestLargeId;
	private int _allyCrestId;
	private int _auctionBiddedAt;
	private long _allyPenaltyExpiryTime;
	private int _allyPenaltyType;
	private long _charPenaltyExpiryTime;
	private long _dissolvingExpiryTime;
	
	// Ally Penalty Types
	public static final int PENALTY_TYPE_CLAN_LEAVED = 1;
	public static final int PENALTY_TYPE_CLAN_DISMISSED = 2;
	public static final int PENALTY_TYPE_DISMISS_CLAN = 3;
	public static final int PENALTY_TYPE_DISSOLVE_ALLY = 4;
	
	private final ItemContainer _warehouse = new ClanWarehouse(this);
	private final List<Integer> _atWarWith = new ArrayList<>();
	private final List<Integer> _atWarAttackers = new ArrayList<>();
	private final Map<Integer, Long> _warPenaltyExpiryTime = new HashMap<>();
	
	private Forum _forum;
	
	// Clan Privileges
	public static final int CP_NOTHING = 0;
	public static final int CP_CL_JOIN_CLAN = 2;
	public static final int CP_CL_GIVE_TITLE = 4;
	public static final int CP_CL_VIEW_WAREHOUSE = 8;
	public static final int CP_CL_MANAGE_RANKS = 16;
	public static final int CP_CL_PLEDGE_WAR = 32;
	public static final int CP_CL_DISMISS = 64;
	public static final int CP_CL_REGISTER_CREST = 128;
	public static final int CP_CL_MASTER_RIGHTS = 256;
	public static final int CP_CL_MANAGE_LEVELS = 512;
	public static final int CP_CH_OPEN_DOOR = 1024;
	public static final int CP_CH_OTHER_RIGHTS = 2048;
	public static final int CP_CH_AUCTION = 4096;
	public static final int CP_CH_DISMISS = 8192;
	public static final int CP_CH_SET_FUNCTIONS = 16384;
	public static final int CP_CS_OPEN_DOOR = 32768;
	public static final int CP_CS_MANOR_ADMIN = 65536;
	public static final int CP_CS_MANAGE_SIEGE = 131072;
	public static final int CP_CS_USE_FUNCTIONS = 262144;
	public static final int CP_CS_DISMISS = 524288;
	public static final int CP_CS_TAXES = 1048576;
	public static final int CP_CS_MERCENARIES = 2097152;
	public static final int CP_CS_SET_FUNCTIONS = 4194304;
	public static final int CP_ALL = 8388606;
	
	// Sub-unit types
	public static final int SUBUNIT_ACADEMY = -1;
	public static final int SUBUNIT_ROYAL1 = 100;
	public static final int SUBUNIT_ROYAL2 = 200;
	public static final int SUBUNIT_KNIGHT1 = 1001;
	public static final int SUBUNIT_KNIGHT2 = 1002;
	public static final int SUBUNIT_KNIGHT3 = 2001;
	public static final int SUBUNIT_KNIGHT4 = 2002;
	
	protected final Map<Integer, L2Skill> _skills = new HashMap<>();
	protected final Map<Integer, RankPrivs> _privs = new HashMap<>();
	protected final Map<Integer, SubPledge> _subPledges = new HashMap<>();
	
	private int _reputationScore;
	private int _rank;
	
	private String _notice;
	private boolean _noticeEnabled;
	private static final int MAX_NOTICE_LENGTH = 8192;
	
	private String _introduction;
	private static final int MAX_INTRODUCTION_LENGTH = 300;
	
	private int _siegeKills;
	private int _siegeDeaths;
	
	private Npc _flag;
	
	/**
	 * Called if a clan is referenced only by id. In this case all other data needs to be fetched from db
	 * @param clanId A valid clan Id to create and restore
	 * @param leaderId clan leader Id
	 */
	public Clan(int clanId, int leaderId)
	{
		_clanId = clanId;
		initializePrivs();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT char_name,level,classid,obj_Id,title,power_grade,subpledge,apprentice,sponsor,sex,race FROM characters WHERE clanid=?");
			statement.setInt(1, _clanId);
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				ClanMember member = new ClanMember(this, rset);
				if (member.getObjectId() == leaderId)
					setLeader(member);
				else
					_members.put(member.getObjectId(), member);
				
				member.setApprenticeAndSponsor(rset.getInt("apprentice"), rset.getInt("sponsor"));
			}
			
			rset.close();
			statement.close();
			
			restoreSubPledges();
			restoreRankPrivs();
			restoreSkills();
			checkCrests();
		}
		catch (Exception e)
		{
			_log.warning("Error while restoring clan " + e.getMessage());
		}
		
		_warehouse.restore();
	}
	
	/**
	 * Called only if a new clan is created
	 * @param clanId A valid clan Id to create
	 * @param clanName A valid clan name
	 */
	public Clan(int clanId, String clanName)
	{
		_clanId = clanId;
		_name = clanName;
		initializePrivs();
	}
	
	/**
	 * @return the clanId.
	 */
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * @param clanId The clanId to set.
	 */
	public void setClanId(int clanId)
	{
		_clanId = clanId;
	}
	
	/**
	 * @return the leaderId.
	 */
	public int getLeaderId()
	{
		return _leader.getObjectId();
	}
	
	/**
	 * @return L2ClanMember of clan leader.
	 */
	public ClanMember getLeader()
	{
		return _leader;
	}
	
	/**
	 * @param leader The leader to set.
	 */
	public void setLeader(ClanMember leader)
	{
		_leader = leader;
		_members.put(leader.getObjectId(), leader);
	}
	
	public void setNewLeader(ClanMember member)
	{
		if (!_leader.isOnline())
			return;
		
		if (member == null || !member.isOnline())
			return;
		
		Player exLeader = _leader.getPlayerInstance();
		exLeader.removeSiegeSkills();
		exLeader.setClan(this);
		exLeader.setClanPrivileges(Clan.CP_NOTHING);
		exLeader.broadcastUserInfo();
		
		setLeader(member);
		updateClanInDB();
		
		exLeader.setPledgeClass(ClanMember.calculatePledgeClass(exLeader));
		exLeader.broadcastUserInfo();
		
		Player newLeader = member.getPlayerInstance();
		newLeader.setClan(this);
		newLeader.setPledgeClass(ClanMember.calculatePledgeClass(newLeader));
		newLeader.setClanPrivileges(Clan.CP_ALL);
		if (_level >= Config.MINIMUM_CLAN_LEVEL)
		{
			newLeader.addSiegeSkills();
			
			// Transfering siege skills TimeStamps from old leader to new leader to prevent unlimited headquarters
			if (!exLeader.getReuseTimeStamp().isEmpty())
			{
				for (L2Skill sk : SkillTable.getInstance().getSiegeSkills(newLeader.isNoble()))
				{
					if (exLeader.getReuseTimeStamp().containsKey(sk.getReuseHashCode()))
					{
						TimeStamp t = exLeader.getReuseTimeStamp().get(sk.getReuseHashCode());
						newLeader.addTimeStamp(sk, t.getReuse(), t.getStamp());
					}
				}
				newLeader.sendPacket(new SkillCoolTime(newLeader));
			}
		}
		newLeader.broadcastUserInfo();
		
		broadcastClanStatus();
		broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.CLAN_LEADER_PRIVILEGES_HAVE_BEEN_TRANSFERRED_TO_S1).addCharName(newLeader));
	}
	
	/**
	 * @return the leaderName.
	 */
	public String getLeaderName()
	{
		return (_leader == null) ? "" : _leader.getName();
	}
	
	/**
	 * @return the name.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * @param name : The name to set.
	 */
	public void setName(String name)
	{
		_name = name;
	}
	
	public void addClanMember(Player player)
	{
		final ClanMember member = new ClanMember(this, player);
		_members.put(member.getObjectId(), member);
		member.setPlayerInstance(player);
		player.setClan(this);
		player.setPledgeClass(ClanMember.calculatePledgeClass(player));
		
		// Update siege flag, if needed.
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			final Siege siege = castle.getSiege();
			if (!siege.isInProgress())
				continue;
			
			if (siege.checkSide(this, SiegeSide.ATTACKER))
				player.setSiegeState((byte) 1);
			else if (siege.checkSides(this, SiegeSide.DEFENDER, SiegeSide.OWNER))
				player.setSiegeState((byte) 2);
		}
		
		player.sendPacket(new PledgeShowMemberListUpdate(player));
		player.sendPacket(new UserInfo(player));
	}
	
	public void updateClanMember(Player player)
	{
		final ClanMember member = new ClanMember(player.getClan(), player);
		if (player.isClanLeader())
			setLeader(member);
		
		_members.put(member.getObjectId(), member);
	}
	
	public ClanMember getClanMember(String name)
	{
		for (ClanMember temp : _members.values())
		{
			if (temp.getName().equals(name))
				return temp;
		}
		return null;
	}
	
	/**
	 * @param objectID : the required clan member object Id.
	 * @return the clan member for a given {@code objectID}.
	 */
	public ClanMember getClanMember(int objectID)
	{
		return _members.get(objectID);
	}
	
	/**
	 * @param objectId : the object Id of the member that will be removed.
	 * @param clanJoinExpiryTime : time penalty to join a clan.
	 */
	public void removeClanMember(int objectId, long clanJoinExpiryTime)
	{
		final ClanMember exMember = _members.remove(objectId);
		if (exMember == null)
		{
			_log.warning("Member Object ID: " + objectId + " not found in clan while trying to remove");
			return;
		}
		
		final int leadssubpledge = getLeaderSubPledge(objectId);
		if (leadssubpledge != 0)
		{
			// Sub-unit leader withdraws, position becomes vacant and leader should appoint new via NPC
			getSubPledge(leadssubpledge).setLeaderId(0);
			updateSubPledgeInDB(leadssubpledge);
		}
		
		if (exMember.getApprentice() != 0)
		{
			final ClanMember apprentice = getClanMember(exMember.getApprentice());
			if (apprentice != null)
			{
				if (apprentice.getPlayerInstance() != null)
					apprentice.getPlayerInstance().setSponsor(0);
				else
					apprentice.setApprenticeAndSponsor(0, 0);
				
				apprentice.saveApprenticeAndSponsor(0, 0);
			}
		}
		
		if (exMember.getSponsor() != 0)
		{
			final ClanMember sponsor = getClanMember(exMember.getSponsor());
			if (sponsor != null)
			{
				if (sponsor.getPlayerInstance() != null)
					sponsor.getPlayerInstance().setApprentice(0);
				else
					sponsor.setApprenticeAndSponsor(0, 0);
				
				sponsor.saveApprenticeAndSponsor(0, 0);
			}
		}
		exMember.saveApprenticeAndSponsor(0, 0);
		
		if (Config.REMOVE_CASTLE_CIRCLETS && hasCastle())
			CastleManager.getInstance().getCastleById(_castleId).removeCircletsAndCrown(exMember);
		
		if (exMember.isOnline())
		{
			Player player = exMember.getPlayerInstance();
			
			// Clean title only for non nobles.
			if (!player.isNoble())
				player.setTitle("");
			
			// Setup active warehouse to null.
			if (player.getActiveWarehouse() != null)
				player.setActiveWarehouse(null);
			
			player.setApprentice(0);
			player.setSponsor(0);
			player.setSiegeState((byte) 0);
			
			if (player.isClanLeader())
			{
				player.removeSiegeSkills();
				player.setClanCreateExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_CREATE_DAYS * 86400000L);
			}
			
			for (L2Skill skill : player.getClan().getClanSkills().values())
				player.removeSkill(skill.getId(), false);
			
			player.sendSkillList();
			player.setClan(null);
			
			// players leaving from clan academy have no penalty
			if (exMember.getPledgeType() != -1)
				player.setClanJoinExpiryTime(clanJoinExpiryTime);
			
			player.setPledgeClass(ClanMember.calculatePledgeClass(player));
			player.broadcastUserInfo();
			
			// disable clan tab
			player.sendPacket(PledgeShowMemberListDeleteAll.STATIC_PACKET);
		}
		else
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				PreparedStatement statement = con.prepareStatement("UPDATE characters SET clanid=0, title=?, clan_join_expiry_time=?, clan_create_expiry_time=?, clan_privs=0, wantspeace=0, subpledge=0, lvl_joined_academy=0, apprentice=0, sponsor=0 WHERE obj_Id=?");
				statement.setString(1, "");
				statement.setLong(2, clanJoinExpiryTime);
				statement.setLong(3, _leader.getObjectId() == objectId ? System.currentTimeMillis() + Config.ALT_CLAN_CREATE_DAYS * 86400000L : 0);
				statement.setInt(4, exMember.getObjectId());
				statement.execute();
				statement.close();
				
				statement = con.prepareStatement("UPDATE characters SET apprentice=0 WHERE apprentice=?");
				statement.setInt(1, exMember.getObjectId());
				statement.execute();
				statement.close();
				
				statement = con.prepareStatement("UPDATE characters SET sponsor=0 WHERE sponsor=?");
				statement.setInt(1, exMember.getObjectId());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("error while removing clan member in db " + e);
			}
		}
	}
	
	public ClanMember[] getMembers()
	{
		return _members.values().toArray(new ClanMember[_members.size()]);
	}
	
	public int getMembersCount()
	{
		return _members.size();
	}
	
	public int getSubPledgeMembersCount(int subpl)
	{
		int result = 0;
		for (ClanMember temp : _members.values())
		{
			if (temp.getPledgeType() == subpl)
				result++;
		}
		return result;
	}
	
	public String getSubPledgeLeaderName(int pledgeType)
	{
		if (pledgeType == 0)
			return _leader.getName();
		
		SubPledge subPledge = _subPledges.get(pledgeType);
		int leaderId = subPledge.getLeaderId();
		
		if (subPledge.getId() == Clan.SUBUNIT_ACADEMY || leaderId == 0)
			return "";
		
		if (!_members.containsKey(leaderId))
		{
			_log.warning("SubPledgeLeader: " + leaderId + " is missing from clan: " + _name + "[" + _clanId + "]");
			return "";
		}
		
		return _members.get(leaderId).getName();
	}
	
	/**
	 * @param pledgeType the Id of the pledge type.
	 * @return the maximum number of members allowed for a given {@code pledgeType}.
	 */
	public int getMaxNrOfMembers(int pledgeType)
	{
		switch (pledgeType)
		{
			case 0:
				switch (_level)
				{
					case 0:
						return 10;
					
					case 1:
						return 15;
					
					case 2:
						return 20;
					
					case 3:
						return 30;
					
					default:
						return 40;
				}
				
			case -1:
			case 100:
			case 200:
				return 20;
			
			case 1001:
			case 1002:
			case 2001:
			case 2002:
				return 10;
		}
		return 0;
	}
	
	public Player[] getOnlineMembers()
	{
		List<Player> list = new ArrayList<>();
		for (ClanMember temp : _members.values())
		{
			if (temp != null && temp.isOnline())
				list.add(temp.getPlayerInstance());
		}
		return list.toArray(new Player[list.size()]);
	}
	
	/**
	 * @return the online clan member count.
	 */
	public int getOnlineMembersCount()
	{
		int count = 0;
		for (ClanMember temp : _members.values())
		{
			if (temp == null || !temp.isOnline())
				continue;
			
			count++;
		}
		return count;
	}
	
	/**
	 * @return the alliance Id.
	 */
	public int getAllyId()
	{
		return _allyId;
	}
	
	/**
	 * @return the alliance name.
	 */
	public String getAllyName()
	{
		return _allyName;
	}
	
	/**
	 * @param allyCrestId the alliance crest Id to be set.
	 */
	public void setAllyCrestId(int allyCrestId)
	{
		_allyCrestId = allyCrestId;
	}
	
	/**
	 * @return the alliance crest Id.
	 */
	public int getAllyCrestId()
	{
		return _allyCrestId;
	}
	
	/**
	 * @return the clan level.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Sets the clan level and updates the clan forum if it's needed.
	 * @param level the clan level to be set.
	 */
	public void setLevel(int level)
	{
		_level = level;
		
		if (Config.ENABLE_COMMUNITY_BOARD && _level >= 2 && _forum == null)
		{
			final Forum forum = ForumsBBSManager.getInstance().getForumByName("ClanRoot");
			if (forum != null)
			{
				_forum = forum.getChildByName(_name);
				if (_forum == null)
					_forum = ForumsBBSManager.getInstance().createNewForum(_name, ForumsBBSManager.getInstance().getForumByName("ClanRoot"), Forum.CLAN, Forum.CLANMEMBERONLY, _clanId);
			}
		}
	}
	
	/**
	 * @return clan castle id.
	 */
	public int getCastleId()
	{
		return _castleId;
	}
	
	/**
	 * @return clan hideout id.
	 */
	public int getHideoutId()
	{
		return _hideoutId;
	}
	
	/**
	 * @return {code true} if the clan has a castle.
	 */
	public boolean hasCastle()
	{
		return _castleId > 0;
	}
	
	/**
	 * @return {code true} if the clan has a hideout.
	 */
	public boolean hasHideout()
	{
		return _hideoutId > 0;
	}
	
	/**
	 * @param crestId : The id of pledge crest.
	 */
	public void setCrestId(int crestId)
	{
		_crestId = crestId;
	}
	
	/**
	 * @return the clanCrestId.
	 */
	public int getCrestId()
	{
		return _crestId;
	}
	
	/**
	 * @param crestLargeId : The id of pledge LargeCrest.
	 */
	public void setCrestLargeId(int crestLargeId)
	{
		_crestLargeId = crestLargeId;
	}
	
	/**
	 * @return the clan CrestLargeId
	 */
	public int getCrestLargeId()
	{
		return _crestLargeId;
	}
	
	/**
	 * @param allyId : The allyId to set.
	 */
	public void setAllyId(int allyId)
	{
		_allyId = allyId;
	}
	
	/**
	 * @param allyName : The allyName to set.
	 */
	public void setAllyName(String allyName)
	{
		_allyName = allyName;
	}
	
	/**
	 * @param castle : The castle id to set.
	 */
	public void setCastle(int castle)
	{
		_castleId = castle;
	}
	
	/**
	 * @param hideout : The hideout id to set.
	 */
	public void setHideout(int hideout)
	{
		_hideoutId = hideout;
	}
	
	/**
	 * @param id the member id.
	 * @return true if the member id given as parameter is in the _members list.
	 */
	public boolean isMember(int id)
	{
		return _members.containsKey(id);
	}
	
	public void updateClanInDB()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET leader_id=?,ally_id=?,ally_name=?,reputation_score=?,ally_penalty_expiry_time=?,ally_penalty_type=?,char_penalty_expiry_time=?,dissolving_expiry_time=? WHERE clan_id=?");
			statement.setInt(1, _leader.getObjectId());
			statement.setInt(2, _allyId);
			statement.setString(3, _allyName);
			statement.setInt(4, _reputationScore);
			statement.setLong(5, _allyPenaltyExpiryTime);
			statement.setInt(6, _allyPenaltyType);
			statement.setLong(7, _charPenaltyExpiryTime);
			statement.setLong(8, _dissolvingExpiryTime);
			statement.setInt(9, _clanId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("error while saving new clan leader to db " + e);
		}
	}
	
	public void store()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("INSERT INTO clan_data (clan_id,clan_name,clan_level,hasCastle,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id) values (?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, _clanId);
			statement.setString(2, _name);
			statement.setInt(3, _level);
			statement.setInt(4, _castleId);
			statement.setInt(5, _allyId);
			statement.setString(6, _allyName);
			statement.setInt(7, _leader.getObjectId());
			statement.setInt(8, _crestId);
			statement.setInt(9, _crestLargeId);
			statement.setInt(10, _allyCrestId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("error while saving new clan to db " + e);
		}
	}
	
	private void storeNotice(String notice, boolean enabled)
	{
		if (notice == null)
			notice = "";
		
		if (notice.length() > MAX_NOTICE_LENGTH)
			notice = notice.substring(0, MAX_NOTICE_LENGTH - 1);
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET enabled=?,notice=? WHERE clan_id=?");
			statement.setString(1, (enabled) ? "true" : "false");
			statement.setString(2, notice);
			statement.setInt(3, _clanId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "L2Clan : could not store clan notice: " + e.getMessage(), e);
		}
		
		_notice = notice;
		_noticeEnabled = enabled;
	}
	
	public void setNoticeEnabledAndStore(boolean enabled)
	{
		storeNotice(_notice, enabled);
	}
	
	public void setNoticeAndStore(String notice)
	{
		storeNotice(notice, _noticeEnabled);
	}
	
	public boolean isNoticeEnabled()
	{
		return _noticeEnabled;
	}
	
	public void setNoticeEnabled(boolean enabled)
	{
		_noticeEnabled = enabled;
	}
	
	public String getNotice()
	{
		return (_notice == null) ? "" : _notice;
	}
	
	public void setNotice(String notice)
	{
		_notice = notice;
	}
	
	public String getIntroduction()
	{
		return (_introduction == null) ? "" : _introduction;
	}
	
	public void setIntroduction(String intro, boolean saveOnDb)
	{
		if (saveOnDb)
		{
			if (intro == null)
				intro = "";
			
			if (intro.length() > MAX_INTRODUCTION_LENGTH)
				intro = intro.substring(0, MAX_INTRODUCTION_LENGTH - 1);
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET introduction=? WHERE clan_id=?");
				statement.setString(1, intro);
				statement.setInt(2, _clanId);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "L2Clan : could not store clan introduction: " + e.getMessage(), e);
			}
		}
		
		_introduction = intro;
	}
	
	public int getSiegeKills()
	{
		return _siegeKills;
	}
	
	public int getSiegeDeaths()
	{
		return _siegeDeaths;
	}
	
	public void setSiegeKills(int value)
	{
		_siegeKills = value;
	}
	
	public void setSiegeDeaths(int value)
	{
		_siegeDeaths = value;
	}
	
	public Npc getFlag()
	{
		return _flag;
	}
	
	public void setFlag(Npc flag)
	{
		// If we set flag as null and clan flag is currently existing, delete it.
		if (flag == null && _flag != null)
			_flag.deleteMe();
		
		_flag = flag;
	}
	
	/**
	 * Restore skills of that clan, and feed _skills Map.
	 */
	private void restoreSkills()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT skill_id,skill_level FROM clan_skills WHERE clan_id=?");
			statement.setInt(1, _clanId);
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				int id = rset.getInt("skill_id");
				int level = rset.getInt("skill_level");
				
				L2Skill skill = SkillTable.getInstance().getInfo(id, level);
				if (skill == null)
					continue;
				
				_skills.put(id, skill);
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore clan skills: " + e);
		}
	}
	
	/**
	 * @return an array with all clan skills that clan knows.
	 */
	public final Map<Integer, L2Skill> getClanSkills()
	{
		return _skills;
	}
	
	/**
	 * Add a new skill to the list, send a packet to all online clan members, update their stats and store it in db
	 * @param newSkill The skill to add
	 */
	public void addNewSkill(L2Skill newSkill) {
		if (newSkill == null)
			return;

		try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
			// Replace oldSkill by newSkill or add the newSkill
			final L2Skill oldSkill = _skills.put(newSkill.getId(), newSkill);

			String query;
			if (oldSkill != null) {
				query = "UPDATE clan_skills SET skill_level=? WHERE skill_id=? AND clan_id=?";
			} else {
				query = "INSERT INTO clan_skills (clan_id, skill_id, skill_level, skill_name) VALUES (?,?,?,?)";
			}

			try (PreparedStatement statement = con.prepareStatement(query)) {
				statement.setInt(1, newSkill.getLevel());
				statement.setInt(2, newSkill.getId());
				statement.setInt(3, _clanId);

				// Add the name for new skills only
				if (oldSkill == null) {
					statement.setString(4, newSkill.getName());
				}
				
				statement.executeUpdate();
			}

		} catch (Exception e) {
			_log.warning("Error could not store clan skills: " + e);
			return;
		}

		// Notify members
		final PledgeSkillListAdd pledgeListAdd = new PledgeSkillListAdd(newSkill.getId(), newSkill.getLevel());
		final PledgeSkillList pledgeList = new PledgeSkillList(this);
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED).addSkillName(newSkill.getId());

		for (Player temp : getOnlineMembers()) {
			if (newSkill.getMinPledgeClass() <= temp.getPledgeClass()) {
				temp.addSkill(newSkill, false);
				temp.sendPacket(pledgeListAdd);
				temp.sendSkillList();
			}
			temp.sendPacket(pledgeList);
			temp.sendPacket(sm);
		}
	}

	
	public void addSkillEffects(Player member)
	{
		if (member == null || _reputationScore <= 0)
			return;
		
		for (L2Skill skill : _skills.values())
		{
			if (skill.getMinPledgeClass() <= member.getPledgeClass())
				member.addSkill(skill, false);
		}
	}
	
	public void broadcastToOnlineAllyMembers(L2GameServerPacket packet)
	{
		for (Clan clan : ClanTable.getInstance().getClanAllies(_allyId))
			clan.broadcastToOnlineMembers(packet);
	}
	
	public void broadcastToOnlineMembers(L2GameServerPacket... packets)
	{
		for (ClanMember member : _members.values())
		{
			if (member != null && member.isOnline())
			{
				for (L2GameServerPacket packet : packets)
					member.getPlayerInstance().sendPacket(packet);
			}
		}
	}
	
	public void broadcastToOtherOnlineMembers(L2GameServerPacket packet, Player player)
	{
		for (ClanMember member : _members.values())
		{
			if (member != null && member.isOnline() && member.getPlayerInstance() != player)
				member.getPlayerInstance().sendPacket(packet);
		}
	}
	
	@Override
	public String toString()
	{
		return _name + "[" + _clanId + "]";
	}
	
	public ItemContainer getWarehouse()
	{
		return _warehouse;
	}
	
	public boolean isAtWarWith(int id)
	{
		return _atWarWith.contains(id);
	}
	
	public boolean isAtWarAttacker(int id)
	{
		return _atWarAttackers.contains(id);
	}
	
	public void setEnemyClan(int clanId)
	{
		_atWarWith.add(clanId);
	}
	
	public void setAttackerClan(int clanId)
	{
		_atWarAttackers.add(clanId);
	}
	
	public void deleteEnemyClan(int clanId)
	{
		_atWarWith.remove(Integer.valueOf(clanId));
	}
	
	public void deleteAttackerClan(int clanId)
	{
		_atWarAttackers.remove(Integer.valueOf(clanId));
	}
	
	public void addWarPenaltyTime(int clanId, long expiryTime)
	{
		_warPenaltyExpiryTime.put(clanId, expiryTime);
	}
	
	public boolean hasWarPenaltyWith(int clanId)
	{
		if (!_warPenaltyExpiryTime.containsKey(clanId))
			return false;
		
		return _warPenaltyExpiryTime.get(clanId) > System.currentTimeMillis();
	}
	
	public Map<Integer, Long> getWarPenalty()
	{
		return _warPenaltyExpiryTime;
	}
	
	public int getHiredGuards()
	{
		return _hiredGuards;
	}
	
	public void incrementHiredGuards()
	{
		_hiredGuards++;
	}
	
	public boolean isAtWar()
	{
		return !_atWarWith.isEmpty();
	}
	
	public List<Integer> getWarList()
	{
		return _atWarWith;
	}
	
	public List<Integer> getAttackerList()
	{
		return _atWarAttackers;
	}
	
	public void broadcastClanStatus()
	{
		for (Player member : getOnlineMembers())
		{
			member.sendPacket(PledgeShowMemberListDeleteAll.STATIC_PACKET);
			member.sendPacket(new PledgeShowMemberListAll(this, 0));
			
			for (SubPledge sp : getAllSubPledges())
				member.sendPacket(new PledgeShowMemberListAll(this, sp.getId()));
			
			member.sendPacket(new UserInfo(member));
		}
	}
	
	public static class SubPledge
	{
		private final int _id;
		private String _subPledgeName;
		private int _leaderId;
		
		public SubPledge(int id, String name, int leaderId)
		{
			_id = id;
			_subPledgeName = name;
			_leaderId = leaderId;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public String getName()
		{
			return _subPledgeName;
		}
		
		public void setName(String name)
		{
			_subPledgeName = name;
		}
		
		public int getLeaderId()
		{
			return _leaderId;
		}
		
		public void setLeaderId(int leaderId)
		{
			_leaderId = leaderId;
		}
	}
	
	public class RankPrivs
	{
		private final int _rankId;
		private int _rankPrivs;
		
		public RankPrivs(int rank, int privs)
		{
			_rankId = rank;
			_rankPrivs = privs;
		}
		
		public int getRank()
		{
			return _rankId;
		}
		
		public int getPrivs()
		{
			return _rankPrivs;
		}
		
		public void setPrivs(int privs)
		{
			_rankPrivs = privs;
		}
	}
	
	public boolean isSubPledgeLeader(int objectId)
	{
		for (SubPledge sp : getAllSubPledges())
		{
			if (sp.getLeaderId() == objectId)
				return true;
		}
		
		return false;
	}
	
	private void restoreSubPledges()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT sub_pledge_id,name,leader_id FROM clan_subpledges WHERE clan_id=?");
			statement.setInt(1, _clanId);
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				int id = rset.getInt("sub_pledge_id");
				_subPledges.put(id, new SubPledge(id, rset.getString("name"), rset.getInt("leader_id")));
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not restore clan sub-units: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieve subPledge by type
	 * @param pledgeType
	 * @return the subpledge object.
	 */
	public final SubPledge getSubPledge(int pledgeType)
	{
		return _subPledges.get(pledgeType);
	}
	
	/**
	 * Retrieve subPledge by name
	 * @param pledgeName
	 * @return the subpledge object.
	 */
	public final SubPledge getSubPledge(String pledgeName)
	{
		for (SubPledge sp : _subPledges.values())
		{
			if (sp.getName().equalsIgnoreCase(pledgeName))
				return sp;
		}
		return null;
	}
	
	/**
	 * Retrieve all subPledges.
	 * @return an array containing all subpledge objects.
	 */
	public final SubPledge[] getAllSubPledges()
	{
		return _subPledges.values().toArray(new SubPledge[_subPledges.values().size()]);
	}
	
	public SubPledge createSubPledge(Player player, int pledgeType, int leaderId, String subPledgeName)
	{
		pledgeType = getAvailablePledgeTypes(pledgeType);
		if (pledgeType == 0)
		{
			if (pledgeType == Clan.SUBUNIT_ACADEMY)
				player.sendPacket(SystemMessageId.CLAN_HAS_ALREADY_ESTABLISHED_A_CLAN_ACADEMY);
			else
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_MILITARY_UNIT);
			
			return null;
		}
		
		if (_leader.getObjectId() == leaderId)
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_MILITARY_UNIT);
			return null;
		}
		
		// Check regarding clan reputation points need : Royal Guard 5000 points, Order of Knights 10000 points.
		if (pledgeType != -1 && ((_reputationScore < 5000 && pledgeType < Clan.SUBUNIT_KNIGHT1) || (_reputationScore < 10000 && pledgeType > Clan.SUBUNIT_ROYAL2)))
		{
			player.sendPacket(SystemMessageId.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
			return null;
		}
		
		SubPledge subPledge = null;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("INSERT INTO clan_subpledges (clan_id,sub_pledge_id,name,leader_id) values (?,?,?,?)");
			statement.setInt(1, _clanId);
			statement.setInt(2, pledgeType);
			statement.setString(3, subPledgeName);
			statement.setInt(4, (pledgeType != -1) ? leaderId : 0);
			statement.execute();
			statement.close();
			
			subPledge = new SubPledge(pledgeType, subPledgeName, leaderId);
			_subPledges.put(pledgeType, subPledge);
			
			if (pledgeType != -1)
			{
				if (pledgeType < Clan.SUBUNIT_KNIGHT1) // royal
					takeReputationScore(5000);
				else if (pledgeType > Clan.SUBUNIT_ROYAL2) // knight
					takeReputationScore(10000);
			}
		}
		catch (Exception e)
		{
			_log.warning("error while saving new sub_clan to db " + e);
		}
		
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(_leader.getClan()), new PledgeReceiveSubPledgeCreated(subPledge, _leader.getClan()));
		
		return subPledge;
	}
	
	public int getAvailablePledgeTypes(int pledgeType)
	{
		if (_subPledges.get(pledgeType) != null)
		{
			switch (pledgeType)
			{
				case SUBUNIT_ACADEMY:
					return 0;
				
				case SUBUNIT_ROYAL1:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_ROYAL2);
					break;
				
				case SUBUNIT_ROYAL2:
					return 0;
				
				case SUBUNIT_KNIGHT1:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT2);
					break;
				
				case SUBUNIT_KNIGHT2:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT3);
					break;
				
				case SUBUNIT_KNIGHT3:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT4);
					break;
				
				case SUBUNIT_KNIGHT4:
					return 0;
			}
		}
		return pledgeType;
	}
	
	public void updateSubPledgeInDB(int pledgeType)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE clan_subpledges SET leader_id=?, name=? WHERE clan_id=? AND sub_pledge_id=?");
			statement.setInt(1, getSubPledge(pledgeType).getLeaderId());
			statement.setString(2, getSubPledge(pledgeType).getName());
			statement.setInt(3, _clanId);
			statement.setInt(4, pledgeType);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error updating subpledge: " + e.getMessage(), e);
		}
	}
	
	private void restoreRankPrivs()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT privs,rank FROM clan_privs WHERE clan_id=?");
			statement.setInt(1, _clanId);
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
				_privs.get(rset.getInt("rank")).setPrivs(rset.getInt("privs"));
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore clan privs by rank: " + e);
		}
	}
	
	public void initializePrivs()
	{
		RankPrivs privs;
		for (int i = 1; i < 10; i++)
		{
			privs = new RankPrivs(i, CP_NOTHING);
			_privs.put(i, privs);
		}
	}
	
	public int getRankPrivs(int rank)
	{
		return (_privs.get(rank) != null) ? _privs.get(rank).getPrivs() : CP_NOTHING;
	}
	
	/**
	 * Update ranks priviledges.
	 * @param rank : The rank to edit.
	 * @param privs : The priviledges to be set.
	 */
	public void setRankPrivs(int rank, int privs)
	{
		if (_privs.get(rank) != null)
		{
			_privs.get(rank).setPrivs(privs);
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				PreparedStatement statement = con.prepareStatement("INSERT INTO clan_privs (clan_id,rank,privs) VALUES (?,?,?) ON DUPLICATE KEY UPDATE privs = ?");
				statement.setInt(1, _clanId);
				statement.setInt(2, rank);
				statement.setInt(3, privs);
				statement.setInt(4, privs);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("Could not store clan privs for rank: " + e);
			}
			
			for (Player member : getOnlineMembers())
			{
				if (member.getPowerGrade() == rank)
					member.setClanPrivileges(privs);
			}
			broadcastClanStatus();
		}
		else
		{
			_privs.put(rank, new RankPrivs(rank, privs));
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				PreparedStatement statement = con.prepareStatement("INSERT INTO clan_privs (clan_id,rank,privs) VALUES (?,?,?)");
				statement.setInt(1, _clanId);
				statement.setInt(2, rank);
				statement.setInt(3, privs);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("Could not create new rank and store clan privs for rank: " + e);
			}
		}
	}
	
	/**
	 * Retrieve all RankPrivs
	 * @return an array containing all RankPrivs objects.
	 */
	public final RankPrivs[] getAllRankPrivs()
	{
		return _privs.values().toArray(new RankPrivs[_privs.values().size()]);
	}
	
	public int getLeaderSubPledge(int leaderId)
	{
		int id = 0;
		for (SubPledge sp : _subPledges.values())
		{
			if (sp.getLeaderId() == 0)
				continue;
			
			if (sp.getLeaderId() == leaderId)
				id = sp.getId();
		}
		return id;
	}
	
	/**
	 * Add the value to the total amount of the clan's reputation score.<br>
	 * <b>This method updates the database.</b>
	 * @param value : The value to add to current amount.
	 */
	public synchronized void addReputationScore(int value)
	{
		setReputationScore(_reputationScore + value);
	}
	
	/**
	 * Removes the value to the total amount of the clan's reputation score.<br>
	 * <b>This method updates the database.</b>
	 * @param value : The value to remove to current amount.
	 */
	public synchronized void takeReputationScore(int value)
	{
		setReputationScore(_reputationScore - value);
	}
	
	/**
	 * Launch behaviors following how big or low is the actual reputation.<br>
	 * <b>This method DOESN'T update the database.</b>
	 * @param value : The total amount to set to _reputationScore.
	 */
	private void setReputationScore(int value)
	{
		// That check is used to see if it needs a refresh.
		final boolean needRefresh = (_reputationScore > 0 && value <= 0) || (value > 0 && _reputationScore <= 0);
		
		// Store the online members (used in 2 positions, can't merge)
		final Player[] members = getOnlineMembers();
		
		_reputationScore = MathUtil.limit(value, -100000000, 100000000);
		
		// Refresh clan windows of all clan members, and reward/remove skills.
		if (needRefresh)
		{
			final Collection<L2Skill> skills = getClanSkills().values();
			
			if (_reputationScore <= 0)
			{
				for (Player member : members)
				{
					member.sendPacket(SystemMessageId.REPUTATION_POINTS_0_OR_LOWER_CLAN_SKILLS_DEACTIVATED);
					
					for (L2Skill sk : skills)
						member.removeSkill(sk.getId(), false);
					
					member.sendSkillList();
				}
			}
			else
			{
				for (Player member : members)
				{
					member.sendPacket(SystemMessageId.CLAN_SKILLS_WILL_BE_ACTIVATED_SINCE_REPUTATION_IS_0_OR_HIGHER);
					
					for (L2Skill sk : skills)
					{
						if (sk.getMinPledgeClass() <= member.getPledgeClass())
							member.addSkill(sk, false);
					}
					
					member.sendSkillList();
				}
			}
		}
		
		// Points reputation update for all.
		final PledgeShowInfoUpdate infoRefresh = new PledgeShowInfoUpdate(this);
		for (Player member : members)
			member.sendPacket(infoRefresh);
		
		// Save the amount on the database.
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET reputation_score=? WHERE clan_id=?");
			statement.setInt(1, _reputationScore);
			statement.setInt(2, _clanId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on updateClanScoreInDb(): " + e.getMessage(), e);
		}
	}
	
	public int getReputationScore()
	{
		return _reputationScore;
	}
	
	public void setRank(int rank)
	{
		_rank = rank;
	}
	
	public int getRank()
	{
		return _rank;
	}
	
	public int getAuctionBiddedAt()
	{
		return _auctionBiddedAt;
	}
	
	public void setAuctionBiddedAt(int id)
	{
		_auctionBiddedAt = id;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET auction_bid_at=? WHERE clan_id=?");
			statement.setInt(1, id);
			statement.setInt(2, _clanId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not store auction for clan: " + e);
		}
	}
	
	/**
	 * Checks if activeChar and target meet various conditions to join a clan
	 * @param activeChar
	 * @param target
	 * @param pledgeType
	 * @return
	 */
	public boolean checkClanJoinCondition(Player activeChar, Player target, int pledgeType)
	{
		if (activeChar == null)
			return false;
		
		if ((activeChar.getClanPrivileges() & Clan.CP_CL_JOIN_CLAN) != Clan.CP_CL_JOIN_CLAN)
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return false;
		}
		
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return false;
		}
		
		if (activeChar.getObjectId() == target.getObjectId())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_INVITE_YOURSELF);
			return false;
		}
		
		if (_charPenaltyExpiryTime > System.currentTimeMillis())
		{
			activeChar.sendPacket(SystemMessageId.YOU_MUST_WAIT_BEFORE_ACCEPTING_A_NEW_MEMBER);
			return false;
		}
		
		if (target.getClanId() != 0)
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_WORKING_WITH_ANOTHER_CLAN).addCharName(target));
			return false;
		}
		
		if (target.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_MUST_WAIT_BEFORE_JOINING_ANOTHER_CLAN).addCharName(target));
			return false;
		}
		
		if ((target.getLevel() > 40 || target.getClassId().level() >= 2) && pledgeType == -1)
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DOESNOT_MEET_REQUIREMENTS_TO_JOIN_ACADEMY).addCharName(target));
			activeChar.sendPacket(SystemMessageId.ACADEMY_REQUIREMENTS);
			return false;
		}
		
		if (getSubPledgeMembersCount(pledgeType) >= getMaxNrOfMembers(pledgeType))
		{
			if (pledgeType == 0)
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CLAN_IS_FULL).addCharName(target));
			else
				activeChar.sendPacket(SystemMessageId.SUBCLAN_IS_FULL);
			return false;
		}
		return true;
	}
	
	/**
	 * @param player : The player alliance launching the invitation.
	 * @param target : The target clan to recruit.
	 * @return true if target's clan meet conditions to join player's alliance.
	 */
	public static boolean checkAllyJoinCondition(Player player, Player target)
	{
		if (player == null)
			return false;
		
		if (player.getAllyId() == 0 || !player.isClanLeader() || player.getClanId() != player.getAllyId())
		{
			player.sendPacket(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER);
			return false;
		}
		
		final Clan leaderClan = player.getClan();
		if (leaderClan.getAllyPenaltyType() == PENALTY_TYPE_DISMISS_CLAN && leaderClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.CANT_INVITE_CLAN_WITHIN_1_DAY);
			return false;
		}
		
		if (target == null)
		{
			player.sendPacket(SystemMessageId.SELECT_USER_TO_INVITE);
			return false;
		}
		
		if (player.getObjectId() == target.getObjectId())
		{
			player.sendPacket(SystemMessageId.CANNOT_INVITE_YOURSELF);
			return false;
		}
		
		if (target.getClan() == null)
		{
			player.sendPacket(SystemMessageId.TARGET_MUST_BE_IN_CLAN);
			return false;
		}
		
		if (!target.isClanLeader())
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER).addCharName(target));
			return false;
		}
		
		final Clan targetClan = target.getClan();
		if (target.getAllyId() != 0)
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CLAN_ALREADY_MEMBER_OF_S2_ALLIANCE).addString(targetClan.getName()).addString(targetClan.getAllyName()));
			return false;
		}
		
		if (targetClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_LEAVED)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANT_ENTER_ALLIANCE_WITHIN_1_DAY).addString(targetClan.getName()));
				return false;
			}
			
			if (targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_DISMISSED)
			{
				player.sendPacket(SystemMessageId.CANT_ENTER_ALLIANCE_WITHIN_1_DAY);
				return false;
			}
		}
		
		// Check if clans are registered as opponents on the same siege.
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (castle.getSiege().isOnOppositeSide(leaderClan, targetClan))
			{
				player.sendPacket(SystemMessageId.OPPOSING_CLAN_IS_PARTICIPATING_IN_SIEGE);
				return false;
			}
		}
		
		if (leaderClan.isAtWarWith(targetClan.getClanId()))
		{
			player.sendPacket(SystemMessageId.MAY_NOT_ALLY_CLAN_BATTLE);
			return false;
		}
		
		if (ClanTable.getInstance().getClanAllies(player.getAllyId()).size() >= Config.ALT_MAX_NUM_OF_CLANS_IN_ALLY)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_LIMIT);
			return false;
		}
		
		return true;
	}
	
	public long getAllyPenaltyExpiryTime()
	{
		return _allyPenaltyExpiryTime;
	}
	
	public int getAllyPenaltyType()
	{
		return _allyPenaltyType;
	}
	
	public void setAllyPenaltyExpiryTime(long expiryTime, int penaltyType)
	{
		_allyPenaltyExpiryTime = expiryTime;
		_allyPenaltyType = penaltyType;
	}
	
	public long getCharPenaltyExpiryTime()
	{
		return _charPenaltyExpiryTime;
	}
	
	public void setCharPenaltyExpiryTime(long time)
	{
		_charPenaltyExpiryTime = time;
	}
	
	public long getDissolvingExpiryTime()
	{
		return _dissolvingExpiryTime;
	}
	
	public void setDissolvingExpiryTime(long time)
	{
		_dissolvingExpiryTime = time;
	}
	
	public void createAlly(Player player, String allyName)
	{
		if (player == null)
			return;
		
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE);
			return;
		}
		
		if (_allyId != 0)
		{
			player.sendPacket(SystemMessageId.ALREADY_JOINED_ALLIANCE);
			return;
		}
		
		if (_level < 5)
		{
			player.sendPacket(SystemMessageId.TO_CREATE_AN_ALLY_YOU_CLAN_MUST_BE_LEVEL_5_OR_HIGHER);
			return;
		}
		
		if (_allyPenaltyType == Clan.PENALTY_TYPE_DISSOLVE_ALLY && _allyPenaltyExpiryTime > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.CANT_CREATE_ALLIANCE_10_DAYS_DISOLUTION);
			return;
		}
		
		if (_dissolvingExpiryTime > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_CREATE_ALLY_WHILE_DISSOLVING);
			return;
		}
		
		if (!StringUtil.isAlphaNumeric(allyName))
		{
			player.sendPacket(SystemMessageId.INCORRECT_ALLIANCE_NAME);
			return;
		}
		
		if (allyName.length() > 16 || allyName.length() < 2)
		{
			player.sendPacket(SystemMessageId.INCORRECT_ALLIANCE_NAME_LENGTH);
			return;
		}
		
		if (ClanTable.getInstance().isAllyExists(allyName))
		{
			player.sendPacket(SystemMessageId.ALLIANCE_ALREADY_EXISTS);
			return;
		}
		
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (castle.getSiege().isInProgress() && castle.getSiege().checkSides(this))
			{
				player.sendPacket(SystemMessageId.NO_ALLY_CREATION_WHILE_SIEGE);
				return;
			}
		}
		
		_allyId = _clanId;
		_allyName = allyName;
		setAllyPenaltyExpiryTime(0, 0);
		updateClanInDB();
		
		player.sendPacket(new UserInfo(player));
	}
	
	public void dissolveAlly(Player player)
	{
		if (_allyId == 0)
		{
			player.sendPacket(SystemMessageId.NO_CURRENT_ALLIANCES);
			return;
		}
		
		if (!player.isClanLeader() || _clanId != _allyId)
		{
			player.sendPacket(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER);
			return;
		}
		
		// For every clan in alliance, check if it is currently registered on siege.
		for (Clan clan : ClanTable.getInstance().getClanAllies(_allyId))
		{
			for (Castle castle : CastleManager.getInstance().getCastles())
			{
				if (castle.getSiege().isInProgress() && castle.getSiege().checkSides(clan))
				{
					player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_ALLY_WHILE_IN_SIEGE);
					return;
				}
			}
		}
		
		broadcastToOnlineAllyMembers(SystemMessage.getSystemMessage(SystemMessageId.ALLIANCE_DISOLVED));
		
		long currentTime = System.currentTimeMillis();
		for (Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() == _allyId && clan.getClanId() != _clanId)
			{
				clan.setAllyId(0);
				clan.setAllyName(null);
				clan.setAllyPenaltyExpiryTime(0, 0);
				clan.updateClanInDB();
			}
		}
		
		_allyId = 0;
		_allyName = null;
		changeAllyCrest(0, false);
		setAllyPenaltyExpiryTime(currentTime + Config.ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED * 86400000L, Clan.PENALTY_TYPE_DISSOLVE_ALLY);
		updateClanInDB();
		
		// The clan leader should take the XP penalty of a full death.
		player.deathPenalty(false, false, false);
	}
	
	public boolean levelUpClan(Player player)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return false;
		}
		
		if (System.currentTimeMillis() < _dissolvingExpiryTime)
		{
			player.sendPacket(SystemMessageId.CANNOT_RISE_LEVEL_WHILE_DISSOLUTION_IN_PROGRESS);
			return false;
		}
		
		boolean increaseClanLevel = false;
		
		switch (_level)
		{
			case 0: // upgrade to 1
				if (player.getSp() >= 500000 && player.reduceAdena("ClanLvl", 650000, player.getTarget(), true))
				{
					player.removeExpAndSp(0, 500000);
					increaseClanLevel = true;
				}
				break;
			
			case 1: // upgrade to 2
				if (player.getSp() >= 1000000 && player.reduceAdena("ClanLvl", 2500000, player.getTarget(), true))
				{
					player.removeExpAndSp(0, 1000000);
					increaseClanLevel = true;
				}
				break;
			
			case 2:// upgrade to 3
				if (player.getSp() >= 3000000 && player.destroyItemByItemId("ClanLvl", 1419, 1, player.getTarget(), true))
				{
					player.removeExpAndSp(0, 3000000);
					increaseClanLevel = true;
				}
				break;
			
			case 3: // upgrade to 4
				if (player.getSp() >= 5000000 && player.destroyItemByItemId("ClanLvl", 3874, 1, player.getTarget(), true))
				{
					player.removeExpAndSp(0, 5000000);
					increaseClanLevel = true;
				}
				break;
			
			case 4: // upgrade to 5
				if (player.getSp() >= 7000000 && player.destroyItemByItemId("ClanLvl", 3870, 1, player.getTarget(), true))
				{
					player.removeExpAndSp(0, 7000000);
					increaseClanLevel = true;
				}
				break;
			
			case 5:
				if (_reputationScore >= 10000 && getMembersCount() >= 5)
				{
					takeReputationScore(10000);
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(10000));
					increaseClanLevel = true;
				}
				break;
			
			case 6:
				if (_reputationScore >= 20000 && getMembersCount() >= 10)
				{
					takeReputationScore(20000);
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(20000));
					increaseClanLevel = true;
				}
				break;
			
			case 7:
				if (_reputationScore >= 40000 && getMembersCount() >= 15)
				{
					takeReputationScore(40000);
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(40000));
					increaseClanLevel = true;
				}
				break;
		}
		
		if (!increaseClanLevel)
		{
			player.sendPacket(SystemMessageId.FAILED_TO_INCREASE_CLAN_LEVEL);
			return false;
		}
		
		player.sendPacket(new ItemList(player, false));
		
		changeLevel(_level + 1);
		return true;
	}
	
	public void changeLevel(int level)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET clan_level = ? WHERE clan_id = ?");
			statement.setInt(1, level);
			statement.setInt(2, _clanId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not increase clan level:" + e.getMessage(), e);
		}
		
		setLevel(level);
		
		if (_leader.isOnline())
		{
			Player leader = _leader.getPlayerInstance();
			if (3 < level)
				leader.addSiegeSkills();
			else if (4 > level)
				leader.removeSiegeSkills();
			
			if (4 < level)
				leader.sendPacket(SystemMessageId.CLAN_CAN_ACCUMULATE_CLAN_REPUTATION_POINTS);
		}
		
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(this), SystemMessage.getSystemMessage(SystemMessageId.CLAN_LEVEL_INCREASED));
	}
	
	/**
	 * Change the clan crest. If crest id is 0, crest is removed. New crest id is saved to database.
	 * @param crestId if 0, crest is removed, else new crest id is set and saved to database
	 */
	public void changeClanCrest(int crestId)
	{
		if (crestId == 0)
			CrestCache.getInstance().removeCrest(CrestType.PLEDGE, _crestId);
		
		_crestId = crestId;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET crest_id = ? WHERE clan_id = ?");
			statement.setInt(1, crestId);
			statement.setInt(2, _clanId);
			statement.executeUpdate();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "Could not update crest for clan " + _name + " [" + _clanId + "] : " + e.getMessage(), e);
		}
		
		for (Player member : getOnlineMembers())
			member.broadcastUserInfo();
	}
	
	/**
	 * Change the ally crest. If crest id is 0, crest is removed. New crest id is saved to database.
	 * @param crestId if 0, crest is removed, else new crest id is set and saved to database
	 * @param onlyThisClan Do it for the ally aswell.
	 */
	public void changeAllyCrest(int crestId, boolean onlyThisClan)
	{
		String sqlStatement = "UPDATE clan_data SET ally_crest_id = ? WHERE clan_id = ?";
		int allyId = _clanId;
		if (!onlyThisClan)
		{
			if (crestId == 0)
				CrestCache.getInstance().removeCrest(CrestType.ALLY, _allyCrestId);
			
			sqlStatement = "UPDATE clan_data SET ally_crest_id = ? WHERE ally_id = ?";
			allyId = _allyId;
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(sqlStatement);
			statement.setInt(1, crestId);
			statement.setInt(2, allyId);
			statement.executeUpdate();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "Could not update ally crest for ally/clan id " + allyId + " : " + e.getMessage(), e);
		}
		
		if (onlyThisClan)
		{
			_allyCrestId = crestId;
			for (Player member : getOnlineMembers())
				member.broadcastUserInfo();
		}
		else
		{
			for (Clan clan : ClanTable.getInstance().getClans())
			{
				if (clan.getAllyId() == _allyId)
				{
					clan.setAllyCrestId(crestId);
					for (Player member : clan.getOnlineMembers())
						member.broadcastUserInfo();
				}
			}
		}
	}
	
	/**
	 * Change the large crest. If crest id is 0, crest is removed. New crest id is saved to database.
	 * @param crestId if 0, crest is removed, else new crest id is set and saved to database
	 */
	public void changeLargeCrest(int crestId)
	{
		if (crestId == 0)
			CrestCache.getInstance().removeCrest(CrestType.PLEDGE_LARGE, _crestLargeId);
		
		_crestLargeId = crestId;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET crest_large_id = ? WHERE clan_id = ?");
			statement.setInt(1, crestId);
			statement.setInt(2, _clanId);
			statement.executeUpdate();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "Could not update large crest for clan " + _name + " [" + _clanId + "] : " + e.getMessage(), e);
		}
		
		for (Player member : getOnlineMembers())
			member.broadcastUserInfo();
	}
	
	private void checkCrests()
	{
		if (_crestId != 0)
		{
			if (CrestCache.getInstance().getCrest(CrestType.PLEDGE, _crestId) == null)
			{
				_log.log(Level.INFO, "Removing non-existent crest for clan " + _name + " [" + _clanId + "], crestId:" + _crestId);
				changeClanCrest(0);
			}
		}
		
		if (_crestLargeId != 0)
		{
			if (CrestCache.getInstance().getCrest(CrestType.PLEDGE_LARGE, _crestLargeId) == null)
			{
				_log.log(Level.INFO, "Removing non-existent large crest for clan " + _name + " [" + _clanId + "], crestLargeId:" + _crestLargeId);
				changeLargeCrest(0);
			}
		}
		
		if (_allyCrestId != 0)
		{
			if (CrestCache.getInstance().getCrest(CrestType.ALLY, _allyCrestId) == null)
			{
				_log.log(Level.INFO, "Removing non-existent ally crest for clan " + _name + " [" + _clanId + "], allyCrestId:" + _allyCrestId);
				changeAllyCrest(0, true);
			}
		}
	}
	
	/**
	 * Verify if the clan is registered to any siege.
	 * @return true if the clan is registered or owner of a castle
	 */
	public boolean isRegisteredOnSiege()
	{
		for (Castle castle : CastleManager.getInstance().getCastles())
			if (castle.getSiege().checkSides(this))
				return true;
			
		return false;
	}
}