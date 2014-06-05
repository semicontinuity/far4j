package org.farmanager.plugins.jdbc;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import org.farmanager.api.jni.PanelColumnType;
import org.farmanager.api.panels.NamedColumnDescriptor;

public class View {
    private static final Logger LOGGER = Logger.getLogger(View.class);

    private Properties properties;
    private View parent;
    protected PanelMode[] panelModes;

    public View(final Properties properties, final View parent) {
        this.properties = new Properties();
        this.properties.putAll(properties);
        this.parent = parent;

        this.panelModes = panelModes();

        loadDriver();
    }

    private static void printPadded(final int i, final String string, final FileWriter fileWriter,
            Properties properties1) throws IOException {
        fileWriter.write(string);
        for (int c = 0; c < columnWidth(properties1, i) - string.length(); c++) {
            fileWriter.write(' ');
        }
    }


    PluginPanelItem[] executeQuery(final String query, final Map<Integer, String[]> result,
            final int columnCount)
    {
        result.clear();
        final int hScreen = AbstractPlugin.saveScreen();
//        AbstractPlugin.message (
//                0, null, new String[] {"Please wait","Executing query", query}, 0);
        try {
            Connection conn = DriverManager.getConnection(getUrl());
            LOGGER.debug("Connection to " + getUrl() + " established");
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
                pluginPanelItem.dwFileAttributes = isNavigatable()
                        ? PluginPanelItem.FILE_ATTRIBUTE_DIRECTORY
                        : PluginPanelItem.FILE_ATTRIBUTE_NORMAL;
                pluginPanelItem.customColumns = new String[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    String value = String.valueOf(rs.getObject(i + 2));
                    pluginPanelItem.customColumns[i] = presentation(i, value,
                            getProperties());
                }

                final int namecolumn = nameColumn(getProperties());
                if (namecolumn == -1) {
                    pluginPanelItem.cFileName = QueryPanelContentProvider_Properties.zeropadded(id); // TODO
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
    private List<String[]> executeQuery(final String query, final int columnCount,
            QueryPanelContentProvider_Properties queryPanelContentProvider_properties) throws SQLException
    {
        final Connection conn = DriverManager.getConnection(queryPanelContentProvider_properties.url);
        LOGGER.debug("Connection to " + queryPanelContentProvider_properties.url + " established");
        final Statement stmt = conn.createStatement();
        LOGGER.debug("Executing query " + '"' + query + '"');
        LOGGER.debug("Column count: " + columnCount);
        final ResultSet rs = stmt.executeQuery(query);

        final ArrayList<String[]> items = new ArrayList<>();
        while (rs.next()) {
            final String[] pluginPanelItem = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                final String value = String.valueOf(rs.getObject(i + 1));
                pluginPanelItem[i] = presentation(i, value, getProperties());
            }
            items.add(pluginPanelItem);
        }
        return items;
    }


    int keyColumn() {
        return intPropertySafe(getProperties(), "key-column");
    }

    public PanelMode[] getPanelModes() {
        return panelModes;
    }

    private static PanelColumnType columnType(final Properties properties, final int i) {
        return Enum.valueOf(PanelColumnType.class, properties.getProperty("column." + i + ".type", "CUSTOM" + i));
    }

    static Integer columnWidth(Properties properties, int i) {
        return Integer.valueOf(properties.getProperty("column." + i + ".width"));
    }

    private static String columnTitle(Properties properties, int i) {
        return properties.getProperty("column." + i + ".title");
    }

    static String presentation(final int i,
            final String s, Properties properties1) {
        if (columnHasPadding(properties1, i)) {
            return pad(s, columnWidth(properties1, i));
        } else {
            return s;
        }
    }

    private static String pad(final String s, final int width) {
//        LOGGER.info("Padding string: [" + s + "]");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < width - s.length(); i++) builder.append(' ');
        builder.append(s);
//        LOGGER.info("Padded string: [" + builder.toString() + "]");
        return builder.toString();
    }

    private static boolean columnHasPadding(final Properties properties, final int i) {
        return properties.getProperty("column." + i + ".padding") != null;
    }

    private static int nameColumn (Properties properties) {
        return intPropertySafe(properties, "namecolumn");
    }

    static int intPropertySafe(final Properties properties, final String key) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }



    public Properties getProperties() {
        return properties;
    }

    public View getParent() {
        return parent;
    }

    void loadDriver() {
        final String driverClass = properties.getProperty("driver");
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load JDBC driver class " + driverClass);
        }
    }

    String getQuery() {
        return properties.getProperty("query");
    }

    String getUrl() {
        return properties.getProperty("url");
    }

    String getTitle() {
        return properties.getProperty("title");
    }

    int getColumnCount() {
        return Integer.parseInt(properties.getProperty("column.count"));
    }

    boolean isNavigatable() {
        return properties.getProperty("navigatable") != null;
    }

    String getChildTemplate() {
        return properties.getProperty("child-template");
    }

    PanelMode[] panelModes() {

        final Properties properties1 = getProperties();
        final NamedColumnDescriptor[] columns =
                new NamedColumnDescriptor[getColumnCount()];
        for (int i = 0; i < getColumnCount(); i++) {
            columns[i] = new NamedColumnDescriptor(
                    columnTitle(properties1, i),
                    columnType(properties1, i),
                    columnWidth(properties1, i));
        }
        final PanelMode[] panelModes = new PanelMode[10];
        for (int i = 0; i < panelModes.length; i++) {
            panelModes[i] = new PanelMode(columns, i == PanelModeId.WIDE, null);
        }
        return panelModes;
    }

    String[] initDefaults() {
        final String query = properties.getProperty("defaults.query");
        LOGGER.info("Defaults query=" + query);
        if (query == null) return null;

        int columnCount = Integer.parseInt(getProperties().getProperty("insert.query.param.count", "0"));
        // TODO: dummy parameter
        final PluginPanelItem[] panelItems = executeQuery(query, new HashMap<Integer, String[]>(),
                columnCount);
        LOGGER.info("Defaults query executed");
        return panelItems[0].customColumns;
    }

    void exportData(final FileWriter fileWriter,
            QueryPanelContentProvider_Properties queryPanelContentProvider_properties)
            throws IOException
    {
        for (PluginPanelItem pluginPanelItem : queryPanelContentProvider_properties.pluginPanelItems) {
            final String[] strings = pluginPanelItem.customColumns;
            for (int i = 0; i < strings.length; i++) {
                String string = strings[i];
                printPadded(i, string, fileWriter, getProperties());
                fileWriter.write(' ');
            }
            fileWriter.write('\n');
        }
    }


    String deleteQuery(int selectedItemId) {
        return constructQuery("delete.query", new Object[]{selectedItemId});
    }

    String updateQuery() {
        return properties.getProperty("update.query");
    }


    String insertQuery(String[] strings) {
        return constructQuery("insert.query", strings);
    }

    String constructQuery(final String templateName, final Object[] params) {
        return (new MessageFormat(properties.getProperty(templateName))).format(params);
    }

}
