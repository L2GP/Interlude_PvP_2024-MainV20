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

public class Q508_AClansReputation extends Quest
{
	private static final String qn = "Q508_AClansReputation";
	
	// NPC
	private static final int SIR_ERIC_RODEMAI = 30868;
	
	// Items
	private static final int NUCLEUS_OF_FLAMESTONE_GIANT = 8494;
	private static final int THEMIS_SCALE = 8277;
	private static final int NUCLEUS_OF_HEKATON_PRIME = 8279;
	private static final int TIPHON_SHARD = 8280;
	private static final int GLAKI_NUCLEUS = 8281;
	private static final int RAHHA_FANG = 8282;
	private static final int KRAVEN_LETTER = 9900;
	private static final int SHACRAM_MEAT = 9901;
	private static final int EYE_OF_KATU = 9902;
	private static final int HANDMAIDEN_CLAW = 9903;
	private static final int MIRROR_PIECE = 9904;
	private static final int CARNAMAKOS_NUCLEUS = 9905;
	private static final int PEASANT_BONE = 9906;
	private static final int TRUNK_OF_RUELL = 9907;
	private static final int LILY_HEART = 9908;
	private static final int CERBERON_LIVER = 9909;
	private static final int RUDELTO_HORN = 9910;
	private static final int HEAD_OF_ISHKA = 9911;
	private static final int KERNON_NECKLACE = 9912;
	private static final int KORIM_MANE = 9913;
	
	// Raidbosses
	private static final int FLAMESTONE_GIANT = 25524;
	private static final int PALIBATI_QUEEN_THEMIS = 25252;
	private static final int HEKATON_PRIME = 25140;
	private static final int GARGOYLE_LORD_TIPHON = 25255;
	private static final int LAST_LESSER_GIANT_GLAKI = 25245;
	private static final int RAHHA = 25051;
	private static final int DREAD_AVENGER_KRAVEN = 25418;
	private static final int SHACRAM = 25102;
	private static final int KATU_VAN_LEADER_ATUI = 25026;
	private static final int ORFENS_HANDMAIDEN = 25420;
	private static final int MIRROR_OF_OBLIVION = 25456;
	private static final int CARNAMAKOS = 25273;
	private static final int GHOST_OF_PEASANT_LEADER = 25013;
	private static final int ENCHANTED_FOREST_WATCHER_RUELL = 25070;
	private static final int BLACK_LILY = 25176;
	private static final int PAGAN_WATCHER_CERBERON = 25280;
	private static final int BLOODY_PRIEST_RUDELTO = 25073;
	private static final int LORD_ISHKA = 25407;
	private static final int KERNON = 25054;
	private static final int KORIM = 25092;
	
	// Reward list (itemId, minClanPoints, maxClanPoints)
	private static final int reward_list[][] =
	{
		{
			PALIBATI_QUEEN_THEMIS,
			THEMIS_SCALE,
			5000,
			10000
		},
		{
			HEKATON_PRIME,
			NUCLEUS_OF_HEKATON_PRIME,
			4000,
			8000
		},
		{
			GARGOYLE_LORD_TIPHON,
			TIPHON_SHARD,
			4000,
			8000
		},
		{
			LAST_LESSER_GIANT_GLAKI,
			GLAKI_NUCLEUS,
			5000,
			10000
		},
		{
			RAHHA,
			RAHHA_FANG,
			4000,
			8000
		},
		{
			FLAMESTONE_GIANT,
			NUCLEUS_OF_FLAMESTONE_GIANT,
			5000,
			10000
		},
		{
			DREAD_AVENGER_KRAVEN,
			KRAVEN_LETTER,
			2000,
			4000
		},
		{
			SHACRAM,
			SHACRAM_MEAT,
			2000,
			4000
		},
		{
			KATU_VAN_LEADER_ATUI,
			EYE_OF_KATU,
			2000,
			4000
		},
		{
			ORFENS_HANDMAIDEN,
			HANDMAIDEN_CLAW,
			2000,
			4000
		},
		{
			MIRROR_OF_OBLIVION,
			MIRROR_PIECE,
			2000,
			4000
		},
		{
			CARNAMAKOS,
			CARNAMAKOS_NUCLEUS,
			3000,
			6000
		},
		{
			GHOST_OF_PEASANT_LEADER,
			PEASANT_BONE,
			3000,
			6000
		},
		{
			ENCHANTED_FOREST_WATCHER_RUELL,
			TRUNK_OF_RUELL,
			3000,
			6000
		},
		{
			BLACK_LILY,
			LILY_HEART,
			3000,
			6000
		},
		{
			PAGAN_WATCHER_CERBERON,
			CERBERON_LIVER,
			3000,
			6000
		},
		{
			BLOODY_PRIEST_RUDELTO,
			RUDELTO_HORN,
			4000,
			8000
		},
		{
			LORD_ISHKA,
			HEAD_OF_ISHKA,
			4000,
			8000
		},
		{
			KERNON,
			KERNON_NECKLACE,
			5000,
			10000
		},
		{
			KORIM,
			KORIM_MANE,
			5000,
			10000
		}
	};
	
	// Radar
	private static final int radar[][] =
	{
		{
			192346,
			21528,
			-3648
		},
		{
			191979,
			54902,
			-7658
		},
		{
			170038,
			-26236,
			-3824
		},
		{
			171762,
			55028,
			-5992
		},
		{
			117232,
			-9476,
			-3320
		},
		{
			144218,
			-5816,
			-4722
		},
		{
			62696,
			8235,
			-3330
		},
		{
			113846,
			84178,
			-2497
		},
		{
			93005,
			7931,
			-3932
		},
		{
			41864,
			23802,
			-4600
		},
		{
			133810,
			87018,
			-3643
		},
		{
			23704,
			119323,
			-9004
		},
		{
			169667,
			11973,
			-2759
		},
		{
			125403,
			50161,
			-3664
		},
		{
			92597,
			115381,
			-3204
		},
		{
			-12596,
			-240448,
			-8189
		},
		{
			143369,
			110450,
			-3975
		},
		{
			114950,
			112505,
			-2996
		},
		{
			113382,
			16458,
			3967
		},
		{
			116186,
			16261,
			1921
		}
	};
	
	public Q508_AClansReputation()
	{
		super(508, "A Clan's Reputation");
		
		setItemsIds(THEMIS_SCALE, NUCLEUS_OF_HEKATON_PRIME, TIPHON_SHARD, GLAKI_NUCLEUS, RAHHA_FANG, NUCLEUS_OF_FLAMESTONE_GIANT, KRAVEN_LETTER, SHACRAM_MEAT, EYE_OF_KATU, HANDMAIDEN_CLAW, MIRROR_PIECE, CARNAMAKOS_NUCLEUS, PEASANT_BONE, TRUNK_OF_RUELL, LILY_HEART, CERBERON_LIVER, RUDELTO_HORN, HEAD_OF_ISHKA, KERNON_NECKLACE, KORIM_MANE);
		
		addStartNpc(SIR_ERIC_RODEMAI);
		addTalkId(SIR_ERIC_RODEMAI);
		
		addKillId(FLAMESTONE_GIANT, PALIBATI_QUEEN_THEMIS, HEKATON_PRIME, GARGOYLE_LORD_TIPHON, LAST_LESSER_GIANT_GLAKI, RAHHA, DREAD_AVENGER_KRAVEN, SHACRAM, KATU_VAN_LEADER_ATUI, ORFENS_HANDMAIDEN, MIRROR_OF_OBLIVION, CARNAMAKOS, GHOST_OF_PEASANT_LEADER, ENCHANTED_FOREST_WATCHER_RUELL, BLACK_LILY, PAGAN_WATCHER_CERBERON, BLOODY_PRIEST_RUDELTO, LORD_ISHKA, KERNON, KORIM);
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
			htmltext = "30868-" + event + ".htm";
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
		else if (event.equalsIgnoreCase("30868-30.htm"))
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
					htmltext = "30868-0a.htm";
				else if (clan.getLevel() < 5)
					htmltext = "30868-0b.htm";
				else
					htmltext = "30868-0c.htm";
				break;
			
			case STATE_STARTED:
				final int raid = st.getInt("raid");
				final int item = reward_list[raid - 1][1];
				
				if (!st.hasQuestItems(item))
					htmltext = "30868-" + raid + "a.htm";
				else
				{
					final int reward = Rnd.get(reward_list[raid - 1][2], reward_list[raid - 1][3]);
					
					htmltext = "30868-" + raid + "b.htm";
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
		// Retrieve the qS of the clan leader.
		QuestState st = getClanLeaderQuestState(player, npc);
		if (st == null || !st.isStarted())
			return null;
		
		// Reward only if quest is setup on good index.
		final int raid = st.getInt("raid");
		if (reward_list[raid - 1][0] == npc.getNpcId())
			st.dropItemsAlways(reward_list[raid - 1][1], 1, 1);
		
		return null;
	}
}