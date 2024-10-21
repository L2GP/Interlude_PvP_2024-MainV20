package net.sf.l2j.gameserver.scripting.quests;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q509_TheClansPrestige extends Quest
{
	private static final String qn = "Q509_TheClansPrestige";
	
	// NPCs
	private static final int VALDIS = 31331;
	
	// Items
	private static final int DAIMON_EYES = 8489;
	private static final int HESTIA_FAIRY_STONE = 8490;
	private static final int NUCLEUS_OF_LESSER_GOLEM = 8491;
	private static final int FALSTON_FANG = 8492;
	private static final int SHAID_TALON = 8493;
	private static final int BLOOD_OF_QUEEN_ANT = 9914;
	private static final int CORE_PEARL = 9915;
	private static final int ORFEN_BRAIN = 9916;
	private static final int TREASURE_OF_ZAKEN = 9917;
	private static final int ESSENCE_OF_BAIUM = 9918;
	private static final int HEART_OF_ANTHARAS = 9919;
	private static final int ASHES_OF_VALAKAS = 9920;
	private static final int FRINTEZZA_SOUL_CRYSTAL = 9921;
	
	// Raid Bosses
	private static final int DAIMON_THE_WHITE_EYED = 25290;
	private static final int HESTIA_GUARDIAN_DEITY = 25293;
	private static final int PLAGUE_GOLEM = 25523;
	private static final int DEMON_AGENT_FALSTON = 25322;
	private static final int QUEEN_SHYEED = 25514;
	private static final int QUEEN_ANT = 29001;
	private static final int CORE = 29006;
	private static final int ORFEN = 29014;
	private static final int ZAKEN = 29022;
	private static final int BAIUM = 29020;
	private static final int ANTHARAS = 29019;
	private static final int VALAKAS = 29028;
	private static final int FRINTEZZA = 29045;
	
	// Reward list (itemId, minClanPoints, maxClanPoints)
	private static final int reward_list[][] =
	{
		{
			DAIMON_THE_WHITE_EYED,
			DAIMON_EYES,
			6000,
			12000
		},
		{
			HESTIA_GUARDIAN_DEITY,
			HESTIA_FAIRY_STONE,
			6000,
			12000
		},
		{
			PLAGUE_GOLEM,
			NUCLEUS_OF_LESSER_GOLEM,
			6000,
			12000
		},
		{
			DEMON_AGENT_FALSTON,
			FALSTON_FANG,
			6000,
			12000
		},
		{
			QUEEN_SHYEED,
			SHAID_TALON,
			6000,
			12000
		},
		{
			QUEEN_ANT,
			BLOOD_OF_QUEEN_ANT,
			20000,
			40000
		},
		{
			CORE,
			CORE_PEARL,
			20000,
			40000
		},
		{
			ORFEN,
			ORFEN_BRAIN,
			20000,
			40000
		},
		{
			ZAKEN,
			TREASURE_OF_ZAKEN,
			20000,
			40000
		},
		{
			BAIUM,
			ESSENCE_OF_BAIUM,
			20000,
			40000
		},
		{
			ANTHARAS,
			HEART_OF_ANTHARAS,
			20000,
			40000
		},
		{
			VALAKAS,
			ASHES_OF_VALAKAS,
			20000,
			40000
		},
		{
			FRINTEZZA,
			FRINTEZZA_SOUL_CRYSTAL,
			20000,
			40000
		}
		
	};
	
	// Radar
	private static final int radar[][] =
	{
		{
			186320,
			-43904,
			-3175
		},
		{
			134672,
			-115600,
			-1216
		},
		{
			170000,
			-59900,
			-3848
		},
		{
			93296,
			-75104,
			-1824
		},
		{
			79635,
			-55612,
			-5980
		},
		{
			-21518,
			181641,
			-5748
		},
		{
			17622,
			109560,
			-6514
		},
		{
			55357,
			17648,
			-5512
		},
		{
			56189,
			218104,
			-3251
		},
		{
			113154,
			14680,
			10051
		},
		{
			177932,
			116035,
			-7734
		},
		{
			214167,
			-115735,
			-1662
		},
		{
			174260,
			-88056,
			-5112
		}
	};
	
	public Q509_TheClansPrestige()
	{
		super(509, "The Clan's Prestige");
		
		setItemsIds(DAIMON_EYES, HESTIA_FAIRY_STONE, NUCLEUS_OF_LESSER_GOLEM, FALSTON_FANG, SHAID_TALON, BLOOD_OF_QUEEN_ANT, CORE_PEARL, ORFEN_BRAIN, TREASURE_OF_ZAKEN, ESSENCE_OF_BAIUM, HEART_OF_ANTHARAS, ASHES_OF_VALAKAS, FRINTEZZA_SOUL_CRYSTAL);
		
		addStartNpc(VALDIS);
		addTalkId(VALDIS);
		
		addKillId(DAIMON_THE_WHITE_EYED, HESTIA_GUARDIAN_DEITY, PLAGUE_GOLEM, DEMON_AGENT_FALSTON, QUEEN_SHYEED, QUEEN_ANT, CORE, ORFEN, ZAKEN, BAIUM, ANTHARAS, VALAKAS, FRINTEZZA);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (StringUtil.isDigit(event))
		{
			htmltext = "31331-" + event + ".htm";
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.set("raid", event);
			st.playSound(QuestState.SOUND_ACCEPT);
			
			int evt = Integer.parseInt(event);
			
			int x = radar[evt - 1][0];
			int y = radar[evt - 1][1];
			int z = radar[evt - 1][2];
			
			if (x + y + z > 0)
				st.addRadar(x, y, z);
		}
		else if (event.equalsIgnoreCase("31331-30.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;
		
		Clan clan = player.getClan();
		
		switch (st.getState())
		{
			case STATE_CREATED:
				if (!player.isClanLeader())
					htmltext = "31331-0a.htm";
				else if (clan.getLevel() < 6)
					htmltext = "31331-0b.htm";
				else
					htmltext = "31331-0c.htm";
				break;
			
			case STATE_STARTED:
				final int raid = st.getInt("raid");
				final int item = reward_list[raid - 1][1];
				
				if (!st.hasQuestItems(item))
					htmltext = "31331-" + raid + "a.htm";
				else
				{
					final int reward = Rnd.get(reward_list[raid - 1][2], reward_list[raid - 1][3]);
					
					htmltext = "31331-" + raid + "b.htm";
					st.takeItems(item, 1);
					clan.addReputationScore(reward);
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CLAN_QUEST_COMPLETED_AND_S1_POINTS_GAINED).addNumber(reward));
					clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				}
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isPet)
	{
		// Retrieve the qs of the clan leader.
		QuestState st = getClanLeaderQuestState(player, npc);
		if (st == null || !st.isStarted())
			return null;
		
		// Reward only if quest is setup on good index.
		int raid = st.getInt("raid");
		if (reward_list[raid - 1][0] == npc.getNpcId())
			st.dropItemsAlways(reward_list[raid - 1][1], 1, 1);
		
		return null;
	}
}