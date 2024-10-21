/*
 * L2J_EngineMods
 * Engine developed by Fissban.
 *
 * This software is not free and you do not have permission
 * to distribute without the permission of its owner.
 *
 * This software is distributed only under the rule
 * of www.devsadmins.com.
 * 
 * Contact us with any questions by the media
 * provided by our web or email marco.faccio@gmail.com
 */
package enginemods.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import enginemods.enums.BuffType;
import enginemods.holders.BuffHolder;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

/**
 * @author fissban
 */
public class SchemeBuffData2
{
	private static final List<BuffHolder> _generalBuffs2 = new ArrayList<>();
	
	private static final List<BuffHolder> _warriorBuffs2 = new ArrayList<>();
	private static final List<BuffHolder> _mageBuffs2 = new ArrayList<>();
	
	public static void load()
	{
		// prevenimos datos duplicados en cado de recargar este metodo
		_generalBuffs2.clear();
		_warriorBuffs2.clear();
		_mageBuffs2.clear();
		
		loadBuffs2();
		loadMageBuffs2();
		loadWarriorBuffs2();
	}
	
	private static void loadBuffs2()
	{
		try
		{
			File f = new File("./data/xml/engine/scheme_buffer2/generalBuffs2.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("buff"))
				{
					NamedNodeMap attrs = d.getAttributes();
					
					BuffType type = BuffType.valueOf(attrs.getNamedItem("type").getNodeValue());
					int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
					int lvl = Integer.parseInt(attrs.getNamedItem("lvl").getNodeValue());
					
					_generalBuffs2.add(new BuffHolder(type, id, lvl));
				}
			}
			
			System.out.println("SchemeBuffData2: Load " + _generalBuffs2.size() + " buffs2.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void loadMageBuffs2()
	{
		try
		{
			File f = new File("./data/xml/engine/scheme_buffer2/setMageBuffs2.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("buff"))
				{
					NamedNodeMap attrs = d.getAttributes();
					
					int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
					int lvl = Integer.parseInt(attrs.getNamedItem("lvl").getNodeValue());
					
					_mageBuffs2.add(new BuffHolder(id, lvl));
				}
			}
			
			System.out.println("SchemeBuffData2: Load " + _mageBuffs2.size() + " set mage buffs2.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void loadWarriorBuffs2()
	{
		try
		{
			File f = new File("./data/xml/engine/scheme_buffer2/setWarriorBuffs2.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("buff"))
				{
					NamedNodeMap attrs = d.getAttributes();
					
					int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
					int lvl = Integer.parseInt(attrs.getNamedItem("lvl").getNodeValue());
					
					_warriorBuffs2.add(new BuffHolder(id, lvl));
				}
			}
			
			System.out.println("SchemeBuffData2: Load " + _warriorBuffs2.size() + " set warrior2 buffs.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static List<BuffHolder> getAllMageBuffs2()
	{
		return _mageBuffs2;
	}
	
	public static List<BuffHolder> getAllWarriorBuffs2()
	{
		return _warriorBuffs2;
	}
	
	public static List<BuffHolder> getAllGeneralBuffs2()
	{
		return _generalBuffs2;
	}
}
