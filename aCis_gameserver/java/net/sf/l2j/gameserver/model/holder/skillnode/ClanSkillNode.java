package net.sf.l2j.gameserver.model.holder.skillnode;

import net.sf.l2j.gameserver.templates.StatsSet;

/**
 * A datatype used by clan skill types. It extends {@link GeneralSkillNode}.
 */
public final class ClanSkillNode extends GeneralSkillNode
{
	private final int _itemId;
	private final int _itemCount;
	
	
	public ClanSkillNode(StatsSet set)
	{
		super(set);
		
		_itemId = set.getInteger("itemId");
		_itemCount = set.getInteger("itemCount");
		
	}
	
	public int getItemId()
	{
		return _itemId;
	}
	
	public int getItemCount()
	{
		return _itemCount;
	}
}