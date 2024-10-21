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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

/**
 * @author fissban
 */
public class SkillData
{
    private static final Logger LOG = Logger.getLogger(SkillData.class.getName());

    private static final Map<String, String> SKILLS = new HashMap<>();
    public static final Map<Integer, String> CUSTOM_ICONS = new HashMap<>();

    /**
     * Carga tanto las habilidades estándar como los iconos personalizados.
     */
    public static void load()
    {
        // Prevenimos datos duplicados en caso de recargar este método
        SKILLS.clear();
        CUSTOM_ICONS.clear();

        try
        {
            // Cargar habilidades estándar
            File skillsFile = new File("./data/xml/modsSkill.xml");
            Document skillsDoc = XMLDocumentFactory.getInstance().loadDocument(skillsFile);

            Node n = skillsDoc.getFirstChild();
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
            {
                if (d.getNodeName().equalsIgnoreCase("skill"))
                {
                    NamedNodeMap attrs = d.getAttributes();

                    String id = attrs.getNamedItem("id").getNodeValue();
                    String level = attrs.getNamedItem("level").getNodeValue();
                    String description = attrs.getNamedItem("description").getNodeValue();
                    SKILLS.put(id + " " + level, description);
                }
            }

            LOG.info(SkillData.class.getSimpleName() + " cargó " + SKILLS.size() + " datos de habilidades.");

            // Cargar iconos personalizados
            loadCustomIcons();

            LOG.info(SkillData.class.getSimpleName() + " cargó " + CUSTOM_ICONS.size() + " iconos personalizados.");
        }
        catch (Exception e)
        {
            LOG.severe("Error al cargar SkillData: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Carga los iconos personalizados desde el archivo modsCustomIcons.xml
     */
    private static void loadCustomIcons()
    {
        try
        {
            File customIconsFile = new File("./data/xml/modsCustomIcons.xml");
            if (!customIconsFile.exists())
            {
                LOG.warning("Archivo de iconos personalizados no encontrado: " + customIconsFile.getPath());
                return;
            }

            Document customDoc = XMLDocumentFactory.getInstance().loadDocument(customIconsFile);
            Node n = customDoc.getFirstChild();
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
            {
                if (d.getNodeName().equalsIgnoreCase("icon"))
                {
                    NamedNodeMap attrs = d.getAttributes();

                    int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                    String file = attrs.getNamedItem("file").getNodeValue();
                    CUSTOM_ICONS.put(id, file);
                }
            }
        }
        catch (Exception e)
        {
            LOG.severe("Error al cargar iconos personalizados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtiene la descripción de una habilidad específica.
     *
     * @param id  ID de la habilidad
     * @param lvl Nivel de la habilidad
     * @return Descripción de la habilidad
     */
    public static String getDescription(int id, int lvl)
    {
        return SKILLS.get(id + " " + lvl);
    }

    /**
     * Obtiene el icono correspondiente a una habilidad.
     * Primero verifica si existe un icono personalizado; si no, utiliza la lógica predeterminada.
     *
     * @param id ID de la habilidad
     * @return Nombre del archivo de icono
     */
    public static String getSkillIcon(int id)
    {
        // Verificar si hay un icono personalizado
        if (CUSTOM_ICONS.containsKey(id))
        {
            return CUSTOM_ICONS.get(id); // Retorna algo como "myiconos.skill2000"
        }

        // Lógica de iconos predeterminados
        String formato;
        if (id == 4)
        {
            formato = "0004";
        }
        else if (id > 9 && id < 100)
        {
            formato = "00" + id;
        }
        else if (id > 99 && id < 1000)
        {
            formato = "0" + id;
        }
        else if (id == 1517)
        {
            formato = "1536";
        }
        else if (id == 1518)
        {
            formato = "1537";
        }
        else if (id == 1547)
        {
            formato = "0065";
        }
        else if (id == 2076)
        {
            formato = "0195";
        }
        else if (id > 4550 && id < 4555)
        {
            formato = "5739";
        }
        else if (id > 4698 && id < 4701)
        {
            formato = "1331";
        }
        else if (id > 4701 && id < 4704)
        {
            formato = "1332";
        }
        else if (id == 6049)
        {
            formato = "0094";
        }
        else
        {
            formato = String.valueOf(id);
        }
        return "Icon.skill" + formato;
    }
}

