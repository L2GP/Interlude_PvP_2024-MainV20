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
public class SchemeBuffData3
{
	private static final List<BuffHolder> _generalBuffs3 = new ArrayList<>();
	
	private static final List<BuffHolder> _warriorBuffs3 = new ArrayList<>();
	private static final List<BuffHolder> _mageBuffs3 = new ArrayList<>();
	
	public static void load()
	{
		// prevenimos datos duplicados en cado de recargar este metodo
		_generalBuffs3.clear();
		_warriorBuffs3.clear();
		_mageBuffs3.clear();
		
		loadBuffs3();
		loadMageBuffs3();
		loadWarriorBuffs3();
	}
	
	private static void loadBuffs3()
	{
		try
		{
			File f = new File("./data/xml/engine/scheme_buffer3/generalBuffs3.xml");
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
					
					_generalBuffs3.add(new BuffHolder(type, id, lvl));
				}
			}
			
			System.out.println("SchemeBuffData3: Load " + _generalBuffs3.size() + " buffs 3.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void loadMageBuffs3()
	{
		try
		{
			File f = new File("./data/xml/engine/scheme_buffer3/setMageBuffs3.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("buff"))
				{
					NamedNodeMap attrs = d.getAttributes();
					
					int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
					int lvl = Integer.parseInt(attrs.getNamedItem("lvl").getNodeValue());
					
					_mageBuffs3.add(new BuffHolder(id, lvl));
				}
			}
			
			System.out.println("SchemeBuffData3: Load " + _mageBuffs3.size() + " set mage buffs 3.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void loadWarriorBuffs3()
	{
		try
		{
			File f = new File("./data/xml/engine/scheme_buffer3/setWarriorBuffs3.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("buff"))
				{
					NamedNodeMap attrs = d.getAttributes();
					
					int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
					int lvl = Integer.parseInt(attrs.getNamedItem("lvl").getNodeValue());
					
					_warriorBuffs3.add(new BuffHolder(id, lvl));
				}
			}
			
			System.out.println("SchemeBuffData3: Load " + _warriorBuffs3.size() + " set warrior buffs 3.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static List<BuffHolder> getAllMageBuffs3()
	{
		return _mageBuffs3;
	}
	
	public static List<BuffHolder> getAllWarriorBuffs3()
	{
		return _warriorBuffs3;
	}
	
	public static List<BuffHolder> getAllGeneralBuffs3()
	{
		return _generalBuffs3;
	}
}
