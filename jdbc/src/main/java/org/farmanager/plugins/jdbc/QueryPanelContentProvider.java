package org.farmanager.plugins.jdbc;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.farmanager.api.AbstractPlugin;
import org.farmanager.api.PanelMode;
import org.farmanager.api.PanelModeId;
import org.farmanager.api.PluginPanelItem;
import org.farmanager.api.dialogs.YesNoDialog;
import org.farmanager.api.jni.FarInfoPanelLine;
import org.farmanager.api.jni.PanelColumnType;
import org.farmanager.api.jni.ProcessKeyFlags;
import org.farmanager.api.messages.Messages;
import org.farmanager.api.panels.NamedColumnDescriptor;
import org.farmanager.api.vfs.AbstractPanelContentProvider;
import org.farmanager.plugins.jdbc.queries.GroovyQueryLoader;
import org.farmanager.plugins.jdbc.queries.Query;

import static org.farmanager.api.jni.KeyCodes.VK_F2;
import static org.farmanager.api.jni.KeyCodes.VK_F4;
import static org.farmanager.api.jni.KeyCodes.VK_F8;
import static org.farmanager.api.jni.ProcessKeyFlags.alt;
import static org.farmanager.api.jni.ProcessKeyFlags.noFlags;
import static org.farmanager.api.jni.ProcessKeyFlags.shift;

/**
 * Provides a content of a panel that is a result of given SQL query
 */
public class QueryPanelContentProvider extends AbstractPanelContentProvider {
    private static final Logger LOGGER = Logger.getLogger(QueryPanelContentProvider.class);

    private String query;
    private String url;
    private int columnCount;
    private PanelMode[] panelModes;
    private String panelTitle;
    private Properties properties;
    private FarInfoPanelLine[] infoPanelLines;
    private String[] defaults;

    private PluginPanelItem[] pluginPanelItems;


    private static DecimalFormat format = new DecimalFormat("0000000000");


    /** If true, query results will be rendered as directories */
    private boolean navigatable;
    private String childTemplate;
    private View currentView;


    private final AbstractPlugin plugin;
    private final JDBCPluginInstance instance;
    /**
     * In some reports, lines may have non-unique ids... take care of that
     */
    private final Map<Integer, String[]> data;
    private List<Query> queries;


    public QueryPanelContentProvider(final JDBCPlugin plugin, final JDBCPluginInstance instance, List<Query> queries) {
        this.plugin = plugin;
        this.instance = instance;
        this.data = new HashMap<>();
        this.queries = queries;
    }


    public void setView(final View view) {
        LOGGER.info("Setting view: " + view);
        this.currentView = view;
        init(view.getProperties());
    }

    public void init(final Properties properties) {
        LOGGER.info("Initializing from properties");
        this.properties = properties;

        loadDriver(properties.getProperty("driver"));

        query = properties.getProperty("query");
        url = properties.getProperty("url");
        panelTitle = properties.getProperty("title");
        columnCount = Integer.parseInt(properties.getProperty("column.count"));
        initPanelModes (properties);
        initDefaults (properties);
        navigatable = properties.getProperty("navigatable") != null;
        childTemplate = properties.getProperty("child-template");
    }

    private static void loadDriver(final String driverClass) {
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load JDBC driver class " + driverClass);
        }
    }

    private void initDefaults(final Properties properties) {
        final String query = properties.getProperty("defaults.query");
        LOGGER.info("Defaults query=" + query);
        if (query == null) return;

        int columnCount = Integer.parseInt(properties.getProperty("insert.query.param.count", "0"));
        // TODO: dummy parameter
        final PluginPanelItem[] panelItems = executeQuery(query, new HashMap<Integer, String[]>(), columnCount);
        defaults = panelItems[0].customColumns;
        LOGGER.info("Defaults query executed");
    }


    private void initPanelModes(final Properties properties) {
        final NamedColumnDescriptor[] columns = new NamedColumnDescriptor[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columns[i] = new NamedColumnDescriptor(
                    columnTitle(properties, i),
                    columnType(properties, i),
                    columnWidth(properties, i));
        }
        panelModes = new PanelMode[10];
        for (int i = 0; i < panelModes.length; i++) {
            panelModes[i] = new PanelMode(columns, i == PanelModeId.WIDE, null);
        }
    }


    @Override
    public PanelMode[] getPanelModes() {
        return panelModes;
    }

    @Override
    public String getPanelTitle() {
        return panelTitle;
    }

    @Override
    public PluginPanelItem[] getFindData(final int opMode) {
        LOGGER.info("getFindData " + opMode);
        // TODO: smarter: cache!
        fillInfoPanelLines();
        return pluginPanelItems = executeQuery(query, data, columnCount);
    }


    private PluginPanelItem[] executeQuery(
            final String query, final Map<Integer, String[]> result, final int columnCount)
    {
        result.clear();
        final int hScreen = AbstractPlugin.saveScreen();
//        AbstractPlugin.message (
//                0, null, new String[] {"Please wait","Executing query", query}, 0);
        try {
            Connection conn = DriverManager.getConnection(url);
            LOGGER.debug("Connection to " + url + " established");
            Statement stmt = conn.createStatement();
            LOGGER.debug("Executing query " + query);
            LOGGER.debug("Column count: " + columnCount);
            ResultSet rs = stmt.executeQuery(query);

            final ArrayList<PluginPanelItem> items = new ArrayList<>();
            while (rs.next()) {
                final PluginPanelItem pluginPanelItem = new PluginPanelItem();
                // First column is ID
                final int id = rs.getInt(1);
                // keep id in crc32.
                // we use Far presentation model to data model, which is ugly
                pluginPanelItem.crc32 = id;
                pluginPanelItem.dwFileAttributes = navigatable
                        ? PluginPanelItem.FILE_ATTRIBUTE_DIRECTORY
                        : PluginPanelItem.FILE_ATTRIBUTE_NORMAL;
                pluginPanelItem.customColumns = new String[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    String value = String.valueOf(rs.getObject(i + 2));
                    pluginPanelItem.customColumns[i] = presentation(i, value);
                }

                final int namecolumn = nameColumn(properties);
                if (namecolumn == -1) {
                    pluginPanelItem.cFileName = zeropadded(id); // TODO
                } else {
                    pluginPanelItem.cFileName = pluginPanelItem.customColumns[namecolumn];
                }
                //LOGGER.info("Set name: " + pluginPanelItem.cFileName);

                items.add(pluginPanelItem);
                result.put(id, pluginPanelItem.customColumns);
            }

            final PluginPanelItem[] pluginPanelItems = new PluginPanelItem[items.size()];
            items.toArray(pluginPanelItems);
            return pluginPanelItems;
        } catch (Exception e) {
            LOGGER.error(e, e);
            return null;    // TODO
        } finally {
            AbstractPlugin.restoreScreen(hScreen);
        }
    }


    // new?
    private List<String[]> executeQuery(final String query, final int columnCount) throws SQLException {
        final Connection conn = DriverManager.getConnection(url);
        LOGGER.debug("Connection to " + url + " established");
        final Statement stmt = conn.createStatement();
        LOGGER.debug("Executing query " + '"' + query + '"');
        LOGGER.debug("Column count: " + columnCount);
        final ResultSet rs = stmt.executeQuery(query);

        final ArrayList<String[]> items = new ArrayList<String[]>();
        while (rs.next()) {
            final String[] pluginPanelItem = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                final String value = String.valueOf(rs.getObject(i + 1));
                pluginPanelItem[i] = presentation(i, value);
            }
            items.add(pluginPanelItem);
        }
        return items;
    }


    private String presentation(final int i, final String s) {
        if (columnHasPadding(properties, i)) {
            return pad(s, columnWidth(properties, i));
        } else {
            return s;
        }
    }

    private static boolean columnHasPadding(final Properties properties, final int i) {
        return properties.getProperty("column." + i + ".padding") != null;
    }

    private static PanelColumnType columnType(final Properties properties, final int i) {
        return Enum.valueOf(PanelColumnType.class, properties.getProperty("column." + i + ".type", "CUSTOM" + i));
    }

    private static Integer columnWidth (Properties properties, int i)
    {
        return Integer.valueOf (properties.getProperty ("column." + i + ".width"));
    }

    private static String columnTitle(Properties properties, int i) {
        return properties.getProperty("column." + i + ".title");
    }

    private static int nameColumn (Properties properties)
    {
        return intPropertySafe(properties, "namecolumn");
    }

    private static int keyColumn(final Properties properties) {
        return intPropertySafe(properties, "key-column");
    }

    private static int intPropertySafe (final Properties properties, final String key) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }


    private String pad(final String s, final int width) {
//        LOGGER.info("Padding string: [" + s + "]");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < width - s.length(); i++) builder.append(' ');
        builder.append(s);
//        LOGGER.info("Padded string: [" + builder.toString() + "]");
        return builder.toString();
    }


    public List<IdValuePair> executeIdValueQuery(final String query) {
        try {
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            final List<IdValuePair> items = new ArrayList<IdValuePair>();

            while (rs.next()) {
                final int id = rs.getInt(1); // TODO: more checks
                final String value = rs.getString(2);  // TODO: more checks
                items.add(new IdValuePair(id, value));
            }
            return items;
        } catch (Exception e) {
            LOGGER.error(e, e);
            return null;    // TODO
        }
    }


    public Object executeScalarQuery (final String query)
    {
        try
        {
            Connection conn = DriverManager.getConnection (url);
            Statement stmt = conn.createStatement ();
            ResultSet rs = stmt.executeQuery (query);

            Object value = null;

            while (rs.next ())
            {
                value = rs.getObject (1); // TODO: more checks

            }

            rs.close();
            stmt.close();

            return value;

        }
        catch (Exception e)
        {
            LOGGER.error (e, e);
            return null;    // TODO
        }
    }


    @Override
    public int getFile (final String fileName, final String destPath, final int move, final int opmode) {
        int selectedItemId = AbstractPlugin.getSelectedItemCrc32 ();
        final File file = new File (destPath, String.valueOf(selectedItemId));
        try {
            final FileWriter writer = new FileWriter(file);

            writer.write(String.valueOf(selectedItemId));
            writer.write('\n');
            final String[] strings = data.get(selectedItemId);
            for (String string : strings) {
                writer.write(string);
                writer.write('\n');
            }

            writer.close();
        }
        catch (IOException e) {
            LOGGER.error(e,e);
            return 1;
        }

        return 0;
    }


    @Override
    public int putFile (final String fileName, final int move, final int opmode) {
        final File file = new File (AbstractPlugin.getAnotherPanelDirectory (), fileName);
        try {
            final String s = readStringFromFile(file);
            final String[] strings = s.split("\n");

            for (int i = 0; i < strings.length; i++) {
                strings[i] = strings[i].trim();
            }

            String query = constructQuery ("insert.query", strings);
            executeUpdate(query);

            return 1;
        }
        catch (IOException e) {
            LOGGER.error(e,e);
            return 0;
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
        fileReader.close();
        return stringBuilder.toString ();
    }


    public int processKey(final int key, final int controlState) {
        int realKey = ProcessKeyFlags.clearedPreprocess(key);
        if (alt(controlState) && shift (controlState) && realKey == VK_F4) {
            return handleFavorites();
        }
        else if (shift(controlState) && realKey == VK_F4) {
            return handleInsert();
        }
        else if (noFlags(controlState) && realKey == VK_F4) {
            return handleUpdate();
        }
        else if (noFlags(controlState) && realKey == VK_F8) {
            return handleDelete();
        }
        else if (shift(controlState) && realKey == VK_F2) {
            return handleExport();
        }
        else {
            return 0;
        }
    }

    private int handleExport() {
        try {
            FileWriter fileWriter = new FileWriter(outputFile());
            exportData(fileWriter);
            exportInfo(fileWriter);
            fileWriter.close();
            return 0;

        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
    }

    private void exportInfo(final FileWriter fileWriter)
            throws IOException
    {
        if (infoPanelLines == null) return;
        fileWriter.write('\n');
        for (FarInfoPanelLine infoPanelLine : infoPanelLines) {
            fileWriter.write(infoPanelLine.getText());
            fileWriter.write(' ');
            fileWriter.write(infoPanelLine.getData());
            fileWriter.write('\n');
        }
    }

    private void exportData(final FileWriter fileWriter)
            throws IOException
    {
        for (PluginPanelItem pluginPanelItem : pluginPanelItems) {
            final String[] strings = pluginPanelItem.customColumns;
            for (int i = 0; i < strings.length; i++) {
                String string = strings[i];
                printPadded(i, string, fileWriter);
                fileWriter.write(' ');
            }
            fileWriter.write('\n');
        }
    }

    private File outputFile() {
        return new File(plugin.getHome(), panelTitle + ".txt");
    }

    private void printPadded(final int i, final String string, final FileWriter fileWriter) throws IOException {
        fileWriter.write(string);
        for (int c = 0; c < columnWidth(properties, i) - string.length(); c++) {
            fileWriter.write(' ');
        }
    }


    private int handleDelete() {
        if (properties.getProperty("delete.query") == null) {
            return 0;
        } else {
            if (new YesNoDialog("Delete", "Do you want to delete this record?", "OK", "Cancel").activate()) {
                int selectedItemId = AbstractPlugin.getSelectedItemCrc32();
                LOGGER.info("Going to delete column with id " + selectedItemId);
                String query = constructQuery("delete.query", new Object[]{selectedItemId});
                executeUpdate(query);
            }
            return 1;
        }
    }

    private int handleUpdate() {
        LOGGER.debug("Update!");
        if (properties.getProperty("update.query") == null) {
            return 0;
        }
        final int currentItem = AbstractPlugin.getCurrentItem();
        int selectedItemId = Integer.valueOf(pluginPanelItems[currentItem - 1].cFileName);

//        int selectedItemId = AbstractPlugin.getSelectedItemCrc32();
        String[] selectedLineValues = data.get(selectedItemId);
        ParametersDialog dialog = new ParametersDialog(this, properties, "update", selectedLineValues);
        if (!dialog.activate()) {
            LOGGER.debug("Update dialog dismissed");
            return 1;
        } else {
            String query = constructQuery("update.query", dialog.getParams(selectedItemId));
            executeUpdate(query);
            return 1;
        }
    }


    private int handleFavorites() {
        final String[] titles = favoriteQueriesTitles();
        if (titles.length == 0) return 0;

        ItemSelectionDialog dialog = new ItemSelectionDialog ("Favorite queries", titles);
        final int i = dialog.show();
        if (i == -1)
        {
            return 1;
        }
        else
        {
//            return handleInsert(
//                "favorite.query." + dialog.selectedItem());
            final Query query = queries.get(dialog.selectedItem());
            return query.handleInsert(this, defaults);
        }
    }


    private int handleInsert() {
        if (properties.getProperty("insert.query") == null) {
            return 0;
        }
        final ParametersDialog dialog = new ParametersDialog(this, properties, "insert", defaults);
        if (!dialog.activate()) {
            return 1;
        } else {
            final String query = constructQuery("insert.query", dialog.getParams(null));
            executeUpdate(query);
            return 1;
        }
    }

    private String[] favoriteQueriesTitles () {
/*
        final String count = properties.getProperty("favorite.query.count");
        final int length = Integer.parseInt(count);
        final String[] names = new String[length];
        for (int i = 0; i < names.length; i++) {
            names[i] = properties.getProperty("favorite.query." + i + ".query.title");

        }
        return names;
*/
        final List<Query> list = queries;
        final String[] strings = new String[list.size()];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = list.get(i).getTitle();
        }
        return strings;
    }



    private String constructQuery(
            final String templateName, final Object[] params)
    {
        return (new MessageFormat(properties.getProperty(templateName))).format(
                params);
    }


    public void executeUpdate(final String query) {
        LOGGER.info("Executing query " + query);
        try {
            final Connection conn = DriverManager.getConnection(url);
            final Statement stmt = conn.createStatement();
            stmt.executeUpdate(query);
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            Messages.longOperation(e.toString());
            LOGGER.error(e, e);
        }
        AbstractPlugin.updatePanel();
        AbstractPlugin.redrawPanel();
    }


    private void fillInfoPanelLines() {
        infoPanelLines = null;
        String property;
        property = properties.getProperty("info.line.count");
        if (property == null) {
            return;
        }
        try {
            int count = Integer.parseInt(property);
            if (count < 1) {
                throw new IllegalArgumentException("info.line.count should be > 0");
            }
            infoPanelLines = new FarInfoPanelLine[count];
            for (int i = 0; i < count; i++) fillInfoPanelLine(i);

        } catch (Exception e) {
            LOGGER.warn(e.getMessage());
        }
    }

    /**
     * Every info panel line contains result of certain query
     *
     * @param i index of info panel line
     */
    private void fillInfoPanelLine(final int i) {
        try {
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(
                    properties.getProperty(
                            (new StringBuilder()).append("info.line.")
                                    .append(i)
                                    .append(".query").toString()
                    )
            );
            resultSet.next();
            Object object = resultSet.getObject(1);
            LOGGER.info("Info line: " + object);
            stmt.close();
            conn.close();
            infoPanelLines[i] = new FarInfoPanelLine(
                    properties.getProperty(String.format("info.line.%d.text", i)),
                    String.valueOf(object),
                    false
            );
        } catch (SQLException e) {
            Messages.longOperation(e.toString());
            LOGGER.error(e, e);
        }
    }

    @Override
    public FarInfoPanelLine[] getInfoPanelLines() {
        LOGGER.info("Requested info panel lines: " + (infoPanelLines != null ? infoPanelLines.length : "<null>"));
        return infoPanelLines;
    }

    @Override
    public void setDirectory(String directory) {
        LOGGER.info("Navigating to view " + directory);
        try {
            if ("..".equals(directory)) {
                final View parent = currentView.getParent();
                if (parent == null)
                    instance.navigateToSessionList();
                else
                    setView(parent);
            } else {
                if (childTemplate != null) {
                    LOGGER.info("Has child template:: " + childTemplate);
                    try {
                        final Properties childProperties = new Properties();
                        for (Object key : properties.keySet()) {
                            final String propertyName = (String) key;
                            if (propertyName.startsWith("child."))
                                childProperties.put(propertyName.substring("child.".length()), properties.get(key));
                            LOGGER.debug(".");
                        }
                        LOGGER.debug("#");

                        childProperties.put("main-id", childKey());

                        final View childView = new View(
                                instance.loadTemplate(childTemplate, childProperties),
                                currentView);

                        setView(childView);
                    } catch (IOException e) {
                        LOGGER.error(e, e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e, e);  // TODO
        }
    }

    private String childKey() {
        final int index = keyColumn(properties);
        if (index == -1) {
            return String.valueOf(AbstractPlugin.getSelectedItemCrc32());
        }
        else {
            final int currentItem = AbstractPlugin.getCurrentItem();
            return pluginPanelItems[currentItem - 1].cFileName;
        }
    }


    public String getCurrentDirectory() {
        return panelTitle;
    }


    protected static String zeropadded(final long id) {
        // We do formatting for correct sorting of entries
        // (if String.valueOf() is used, 1000 would be before 999)
        return format.format(id);
    }
}
