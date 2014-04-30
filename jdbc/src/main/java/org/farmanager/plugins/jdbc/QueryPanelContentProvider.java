package org.farmanager.plugins.jdbc;

import org.apache.log4j.Logger;
import org.farmanager.api.AbstractPlugin;
import org.farmanager.api.PanelMode;
import org.farmanager.api.PanelModeId;
import org.farmanager.api.PluginPanelItem;
import org.farmanager.api.dialogs.YesNoDialog;
import org.farmanager.api.jni.FarInfoPanelLine;
import static org.farmanager.api.jni.KeyCodes.*;
import org.farmanager.api.jni.PanelColumnType;
import org.farmanager.api.jni.ProcessKeyFlags;
import static org.farmanager.api.jni.ProcessKeyFlags.noFlags;
import static org.farmanager.api.jni.ProcessKeyFlags.shift;
import org.farmanager.api.messages.Messages;
import org.farmanager.api.panels.NamedColumnDescriptor;
import org.farmanager.api.vfs.AbstractPanelContentProvider;
import org.farmanager.api.vfs.MultiVirtualFSPluginInstance;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.*;

/** Provides a content of a panel that is a result of given SQL query */
public class QueryPanelContentProvider extends AbstractPanelContentProvider
{
    private static final Logger LOGGER = Logger.getLogger (QueryPanelContentProvider.class);

    /**
     * In some reports, lines may have non-unique ids... take care of that
     */
    private Map<Integer, String[]> data;


    private String query;
    private String url;
    private int columnCount;
    private PanelMode[] panelModes;
    private String panelTitle;
    private Properties properties;
    private FarInfoPanelLine[] infoPanelLines;
    private String[] defaults;
    private AbstractPlugin plugin;
    private PluginPanelItem[] pluginPanelItems;
    private MultiVirtualFSPluginInstance instance;

    private static DecimalFormat format = new DecimalFormat("0000000000");



    public QueryPanelContentProvider(JDBCPlugin plugin, JDBCPluginInstance instance)
    {
        this.instance = instance;
        this.plugin = plugin;
        this.data = new HashMap<Integer, String[]> ();
    }

    public void init (Properties properties)
    {
        this.properties = properties;

        try
        {
            Class.forName (properties.getProperty ("driver"));
        }
        catch (ClassNotFoundException e)
        {
            // TODO
            Messages.showException (e);
        }

        query = properties.getProperty ("query");
        url = properties.getProperty ("url");
        panelTitle = properties.getProperty ("title");
        columnCount = Integer.parseInt (properties.getProperty ("column.count"));
        initPanelModes (properties);
        initDefaults (properties);
    }

    private void initDefaults(Properties properties)
    {
        final String query = properties.getProperty("defaults.query");
        LOGGER.info("Defaults query=" + query);
        if (query == null) return;

        int columnCount = Integer.parseInt (properties.getProperty ("insert.query.param.count"));
        // TODO: dummy parameter
        final PluginPanelItem[] panelItems = executeQuery(query, new HashMap<Integer, String[]>(), columnCount);
        defaults = panelItems[0].customColumns;
    }

    private void initPanelModes (Properties properties)
    {
        NamedColumnDescriptor[] columns = new NamedColumnDescriptor[columnCount];
        for (int i = 0; i < columnCount; i++)
        {
            columns[i] = new NamedColumnDescriptor (
                    columnTitle (properties, i),
                    columnType (i),
                    columnWidth (properties, i));
        }
        panelModes = new PanelMode[10];
        for (int i = 0; i < panelModes.length; i++)
        {
            panelModes[i] = new PanelMode (columns, i == PanelModeId.WIDE, null);
        }
    }


    @Override
    public PanelMode[] getPanelModes ()
    {
        return panelModes;
    }

    @Override
    public String getPanelTitle ()
    {
        return panelTitle;
    }

    @Override
    public PluginPanelItem[] getFindData (final int opMode) {
        LOGGER.info("getFindData " + opMode);
        // TODO: smarter: cache!
        fillInfoPanelLines ();
        pluginPanelItems = executeQuery(query, data, columnCount);
        return pluginPanelItems;
    }

    private PluginPanelItem[] executeQuery(
            final String query, final Map<Integer, String[]> result, final int columnCount)
    {
        result.clear ();
        final int hScreen = AbstractPlugin.saveScreen ();
        AbstractPlugin.message (
                0, null, "Please wait\nExecuting query", 0);


        try {
            Connection conn = DriverManager.getConnection (url);
            LOGGER.debug("Connection to " + url + " established");
            Statement stmt = conn.createStatement ();
            LOGGER.debug("Executing query " + query);
            LOGGER.debug("Column count: " + columnCount);
            ResultSet rs = stmt.executeQuery (query);

            final ArrayList<PluginPanelItem> items = new ArrayList<PluginPanelItem> ();
            while (rs.next ()) {
                final PluginPanelItem pluginPanelItem = new PluginPanelItem ();
                // First column is ID
                final int id = rs.getInt(1);
                // keep id in crc32.
                // we use Far presentation model to data model, which is ugly
                pluginPanelItem.crc32 = id;
                pluginPanelItem.dwFileAttributes = PluginPanelItem.FILE_ATTRIBUTE_NORMAL;
                pluginPanelItem.customColumns = new String[columnCount];
                for (int i = 0; i < columnCount; i++)
                {
                    String value = String.valueOf(rs.getObject(i + 2));
                    pluginPanelItem.customColumns[i] = presentation(i, value);
                }

                final int namecolumn = nameColumn(properties);
                if (namecolumn == -1)
                {
                    pluginPanelItem.cFileName = zeropadded(id); // TODO
                }
                else
                {
                    pluginPanelItem.cFileName = pluginPanelItem.customColumns[namecolumn];
                }
                //LOGGER.info("Set name: " + pluginPanelItem.cFileName);

                items.add (pluginPanelItem);
                result.put (new Integer(id), pluginPanelItem.customColumns);
            }

            final PluginPanelItem[] pluginPanelItems = new PluginPanelItem[items.size ()];
            items.toArray (pluginPanelItems);
            return pluginPanelItems;
        }
        catch (Exception e) {
            LOGGER.error (e, e);
            return null;    // TODO
        }
        finally {
            AbstractPlugin.restoreScreen (hScreen);
        }
    }


    private String presentation(int i, String s)
    {
        if (columnPadding(properties, i))
            return pad (s, columnWidth(properties, i));
        else
            return s;
    }

    private static boolean columnPadding (Properties properties, int i)
    {
//        LOGGER.info("Checking for padding " + i);
        boolean b = properties.getProperty("column." + i + ".padding") != null;
//        LOGGER.info("padding " + b);
        return b;
    }

    private static PanelColumnType columnType (int i)
    {
        return Enum.valueOf (PanelColumnType.class, "CUSTOM" + i);
    }

    private static Integer columnWidth (Properties properties, int i)
    {
        return Integer.valueOf (properties.getProperty ("column." + i + ".width"));
    }

    private static String columnTitle (Properties properties, int i)
    {
        return properties.getProperty ("column." + i + ".title");
    }

    private static int nameColumn (Properties properties) {
        try
        {
            return Integer.parseInt(properties.getProperty("namecolumn"));
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }

    private String pad(String s, int width)
    {
//        LOGGER.info("Padding string: [" + s + "]");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < width - s.length(); i++) builder.append(' ');
        builder.append(s);
//        LOGGER.info("Padded string: [" + builder.toString() + "]");
        return builder.toString();
    }


    public List<IdValuePair> executeIdValueQuery (final String query)
    {
        try
        {
            Connection conn = DriverManager.getConnection (url);
            Statement stmt = conn.createStatement ();
            ResultSet rs = stmt.executeQuery (query);

            final List<IdValuePair> items = new ArrayList<IdValuePair> () ;

            while (rs.next ())
            {
                final int id = rs.getInt (1); // TODO: more checks
                final String value = rs.getString (2);  // TODO: more checks
                items.add(new IdValuePair(id, value));
            }
            return items;
        }
        catch (Exception e)
        {
            LOGGER.error (e, e);
            return null;    // TODO
        }
    }

    public int processKey (int key, int controlState)
    {
        int realKey = ProcessKeyFlags.clearedPreprocess (key);
        if (shift (controlState) && realKey == VK_F4)
        {
            return handleInsert ();
        }
        if (noFlags (controlState) && realKey == VK_F4)
        {
            return handleUpdate ();
        }
        if (noFlags (controlState) && realKey == VK_F8)
        {
            return handleDelete ();
        }
        if (shift (controlState) && realKey == VK_F2)
        {
            return handleExport ();
        }
        else
        {
            return 0;
        }
    }

    private int handleExport()
    {
        try
        {
            FileWriter fileWriter = new FileWriter(outputFile());
            exportData(fileWriter);
            exportInfo(fileWriter);
            fileWriter.close();
            return 0;

        }
        catch (IOException e)
        {
            e.printStackTrace();
            return 1;
        }
    }

    private void exportInfo(FileWriter fileWriter)
            throws IOException
    {
        if (infoPanelLines == null) return;
        fileWriter.write('\n');
        for (FarInfoPanelLine infoPanelLine : infoPanelLines)
        {
            fileWriter.write(infoPanelLine.getText());
            fileWriter.write(' ');
            fileWriter.write(infoPanelLine.getData());
            fileWriter.write('\n');
        }
    }

    private void exportData(FileWriter fileWriter)
            throws IOException
    {
        for (PluginPanelItem pluginPanelItem : pluginPanelItems)
        {
            final String[] strings = pluginPanelItem.customColumns;
            for (int i = 0; i < strings.length; i++)
            {
                String string = strings[i];
                printPadded(i, string, fileWriter);
                fileWriter.write(' ');
            }
            fileWriter.write('\n');
        }
    }

    private File outputFile()
    {
        return new File(plugin.getHome(), panelTitle + ".txt");
    }

    private void printPadded(int i, String string, FileWriter fileWriter) throws IOException
    {
        fileWriter.write(string);
        for (int c=0; c < columnWidth(properties, i) - string.length(); c++)
            fileWriter.write(' ');
    }


    private int handleDelete ()
    {
        if (properties.getProperty ("delete.query") == null)
        {
            return 0;
        }
        else
        {
            if (new YesNoDialog("Delete", "Do you want to delete this record?", "OK", "Cancel").activate())
            {
                int selectedItemId = AbstractPlugin.getSelectedItemCrc32 ();
                LOGGER.info("Going to delete column with id " + selectedItemId);
                String query = constructQuery ("delete.query", new Object[]{selectedItemId});
                executeUpdate(query);
            }
            return 1;
        }
    }

    private int handleUpdate ()
    {
        if (properties.getProperty ("update.query") == null)
            return 0;
        int selectedItemId = AbstractPlugin.getSelectedItemCrc32 ();
        String[] selectedLineValues = data.get (selectedItemId);
        ParametersDialog dialog = new ParametersDialog (this, properties, "update", selectedLineValues);
        if (!dialog.activate ())
        {
            return 1;
        }
        else
        {
            String query = constructQuery ("update.query", dialog.getParams (selectedItemId));
            executeUpdate(query);
            return 1;
        }
    }

    private int handleInsert ()
    {
        if (properties.getProperty ("insert.query") == null)
            return 0;
        ParametersDialog dialog = new ParametersDialog (this, properties, "insert", defaults);
        if (!dialog.activate ())
        {
            return 1;
        }
        else
        {
            String query = constructQuery ("insert.query", dialog.getParams (null));
            executeUpdate(query);
            return 1;
        }
    }

    private String constructQuery (
            final String templateName, final Object[] params)
    {
        return (new MessageFormat (properties.getProperty (templateName))).format (
                params);
    }


    private void executeUpdate(String query)
    {
        LOGGER.info("Executing query " + query);
        try
        {
            Connection conn = DriverManager.getConnection (url);
            Statement stmt = conn.createStatement ();
            stmt.executeUpdate (query);
            stmt.close ();
            conn.close ();
        }
        catch (SQLException e)
        {
            Messages.shortMessage (e.toString ());
            LOGGER.error (e, e);
        }
        AbstractPlugin.updatePanel ();
        AbstractPlugin.redrawPanel ();
    }


    private void fillInfoPanelLines ()
    {
        infoPanelLines = null;
        String property;
        property = properties.getProperty ("info.line.count");
        if (property == null)
            return;
        try
        {
            int count = Integer.parseInt (property);
            if (count < 1)
                throw new IllegalArgumentException ("info.line.count should be > 0");
            infoPanelLines = new FarInfoPanelLine[count];
            for (int i = 0; i < count; i++) fillInfoPanelLine(i);

        }
        catch (Exception e)
        {
            LOGGER.warn (e.getMessage ());
        }
    }

    /**
     * Every info panel line contains result of certain query
     * @param i index of info panel line
     */
    private void fillInfoPanelLine(int i)
    {
        try
        {
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(
                    properties.getProperty(
                            (new StringBuilder()).append("info.line.")
                                    .append(i)
                                    .append(".query").toString()));
            resultSet.next();
            Object object = resultSet.getObject(1);
            LOGGER.info("Info line: " + object);
            stmt.close();
            conn.close();
            infoPanelLines[i] = new FarInfoPanelLine(
                    properties.getProperty(
                            new StringBuilder()
                                    .append("info.line.")
                                    .append(i)
                                    .append(".text").toString()),
                    String.valueOf(object),
                    false);
        }
        catch (SQLException e)
        {
            Messages.shortMessage(e.toString());
            LOGGER.error(e, e);
        }
    }

    @Override
    public FarInfoPanelLine[] getInfoPanelLines ()
    {
        LOGGER.info("Requested info panel lines: " + (infoPanelLines != null ? infoPanelLines.length : "<null>"));
        return infoPanelLines;
    }

    @Override
    public void setDirectory(String directory) {
        LOGGER.info("setDirectory " + directory);
        try {
            if ("..".equals(directory)) {
                instance.navigateToSessionList();
            } else {
                LOGGER.warn("There should be no directories here!");
            }
        } catch (Exception e) {
            LOGGER.error(e, e);  // TODO
        }
    }


    public String getCurrentDirectory()
    {
        return panelTitle;
    }


    protected static String zeropadded(long id) {
        // We do formatting for correct sorting of entries
        // (if String.valueOf() is used, 1000 would be before 999)
        return format.format(id);
    }
}
