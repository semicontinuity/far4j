package org.farmanager.api.ofs;

import org.apache.log4j.Logger;
import org.farmanager.api.AbstractPlugin;
import org.farmanager.api.PanelMode;
import org.farmanager.api.PluginPanelItem;
import org.farmanager.api.jni.PanelColumnType;
import org.farmanager.api.panels.NamedColumnDescriptor;
import org.farmanager.api.vfs.AbstractPanelContentProvider;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A prototype of a higher-level API for a virtual file system.
 * Every node of a filesystem tree is an Object.
 * <p/>
 * A node's presentation is discovered in the following order:
 * <ul>
 * <li>
 * A node can explicitly provide information about its presentation by implementing specific interfaces.
 * This approach is recommended for nodes that have "dynamic" presentation.
 * </li>
 * <li>
 * A node class may have associated presentation information. (TODO: implement)
 * </li>
 * <li>
 * A node class can have special annotations that provide hints about its presentation. (TODO: implement)
 * </li>
 * <li>
 * If node does not have any presentation hints,
 * a reasonable "default" representation is created. (TODO: implement)
 * </li>
 * </ul>
 */
public class NodeContentProvider extends AbstractPanelContentProvider
{
    public static final Logger LOGGER = Logger.getLogger (NodeContentProvider.class);

    private Object node;


    public NodeContentProvider (final Object node)
    {
        if (node == null) throw new IllegalArgumentException ();
        this.node = node;
    }

    @Override
    public PanelMode[] getPanelModes ()
    {
        if (node instanceof FarPresentationAttributes)
            return ((FarPresentationAttributes) node).getFarPresentationPanelModes ();
        else
            throw new UnsupportedOperationException ("not supported yet");
    }

    @Override
    public String getPanelTitle ()
    {
        if (node instanceof FarPresentationAttributes)
            return " " + ((FarPresentationAttributes) node).getFarPanelTitle () + " ";
        else
            return " " + node.toString () + " ";
    }

    public String getCurrentDirectory ()
    {
        if (node instanceof FarPresentationAttributes)
        {
            String farDirectory = ((FarPresentationAttributes) node).getFarDirectory ();
            LOGGER.debug ("=" + farDirectory);
            return farDirectory;
        }
        else
            return node.toString ();
    }


    /**
     * @param opMode a combination of OPM_* flags
     * @return A list of panel items to be shown. All columns are returned regardless of the mode.
     */
    @Override
    public PluginPanelItem[] getFindData (final int opMode)
    {
        final int hScreen = AbstractPlugin.saveScreen ();
        AbstractPlugin.message (0, null, "Please wait\nReading node content", 0);

        FarPresentationAttributes presentation = (FarPresentationAttributes) node;
        if (presentation == null) return null;

        final String[] properties = presentation.getFarPresentationChildrenProperties ();
        final PanelMode mode = getPanelModes ()[0];// TODO temp!

        try
        {
            final Object[] children = getChildren ();
            if (children == null) return new PluginPanelItem[0];

            final PluginPanelItem[] pluginPanelItems = new PluginPanelItem[children.length];
            for (int j = 0; j < children.length; j++)
            {
                final Object child = children[j];
                final PluginPanelItem pluginPanelItem = new PluginPanelItem ();
                pluginPanelItems[j] = pluginPanelItem;

                for (int i = 0; i < properties.length; i++)
                {
                    final NamedColumnDescriptor descriptor = mode.getColumnDescriptor (i);
                    final String propertyName = properties[i];
                    final Object value = IntrospectionHelper.getProperty (child, propertyName);
                    final String string = String.valueOf (value);

                    pluginPanelItem.dwFileAttributes = (child instanceof ChildrenAttribute)
                        ? PluginPanelItem.FILE_ATTRIBUTE_DIRECTORY
                        : PluginPanelItem.FILE_ATTRIBUTE_NORMAL;

                    if (descriptor.getType () == PanelColumnType.ID)
                        pluginPanelItem.cFileName = string;
                    else if (descriptor.getType () == PanelColumnType.DESCRIPTION)
                        pluginPanelItem.description = string;
                }
            }

            return pluginPanelItems;
        }
        catch (Exception e)
        {
            LOGGER.error (e, e);    // TODO
            return null;
        }
        finally
        {
            AbstractPlugin.restoreScreen (hScreen);
        }
    }

    private Object[] getChildren () throws Exception
    {
        Class<? extends Object> clazz = node.getClass ();
        final Method method = IntrospectionHelper.findChildrenGetterMethod (clazz);
        LOGGER.debug ("Found method " + method);
        if (method != null)
        {
            final Class returnType = method.getReturnType ();
            if (!returnType.isArray () || returnType.getComponentType ().isPrimitive ())
                return null;
            return (Object[]) method.invoke (node);
        }
        else if (node instanceof ChildrenAttribute)
        {
            return ((ChildrenAttribute) node).getChildren ();
        }
        return null;
    }

    PanelMode[] detectFarPresentationPanelModes () throws Exception
    {
        Class clazz = node.getClass ();
        return findColumns (clazz);
    }

    protected static PanelMode[] findColumns (Class clazz)
    {
        final Map<Integer, List<MethodMappedColumnDescriptor>> map
            = new HashMap<Integer, List<MethodMappedColumnDescriptor>> ();
        for (int i = 0; i < 10; i++) map.put (i, new ArrayList<MethodMappedColumnDescriptor> ());


        final Method[] methods = clazz.getMethods ();
        for (Method method : methods)
        {
            final Column column = method.getAnnotation (Column.class);
            if (column == null) continue;

            for (Mode mode : column.modes ())
            {
                MethodMappedColumnDescriptor methodMappedColumnDescriptor = new MethodMappedColumnDescriptor (
                    method, column.title (), column.type (), mode.width ());

                List<MethodMappedColumnDescriptor> methodMappedColumnDescriptors = map.get (mode.name ());
                methodMappedColumnDescriptors.add (methodMappedColumnDescriptor);
            }
        }

        PanelMode[] panelModes = new PanelMode[10];
        for (int i = 0; i < 10; i++)
        {
            List<MethodMappedColumnDescriptor> list = map.get (i);
            MethodMappedColumnDescriptor[] descriptors = new MethodMappedColumnDescriptor[list.size ()];
            list.toArray (descriptors);
            panelModes[i] = new PanelMode (descriptors, false, null);
        }

        return panelModes;
    }


    public void putFile (String fileName, int move, int opmode) throws IOException
    {
        if (node instanceof PutContentOperation)
        {
            final File file = new File (AbstractPlugin.getAnotherPanelDirectory (), fileName);
            final String contents = readStringFromFile (file);   // TODO: what about non-standard panels?
            ((PutContentOperation) node).putContent (fileName, contents);
        }
    }

    private static String readStringFromFile (File file) throws IOException
    {
        final FileReader fileReader = new FileReader (file);
        final StringBuilder stringBuilder = new StringBuilder ();
        char[] buffer = new char[2048];
        while (true)
        {
            final int read = fileReader.read (buffer);
            if (read == -1) break;
            stringBuilder.append (buffer, 0, read);
        }
        return stringBuilder.toString ();
    }
}
