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
import org.farmanager.api.messages.Messages;
import org.farmanager.api.panels.NamedColumnDescriptor;
import org.farmanager.plugins.jdbc.queries.Query;

public class View {
    private static final Logger LOGGER = Logger.getLogger(View.class);

    protected Properties properties;
    protected View parent;
    protected PanelMode[] panelModes;
    /**
     * In some reports, lines may have non-unique ids... take care of that
     */
    protected final Map<Integer, String[]> data;
    protected PluginPanelItem[] pluginPanelItems;
    protected String[] defaults;
    protected FarInfoPanelLine[] infoPanelLines;

    public View(final Properties properties, final View parent) {
        this.properties = new Properties();
        this.properties.putAll(properties);
        this.parent = parent;
        this.data = new HashMap<>();
        this.panelModes = panelModes();

        loadDriver();
    }


    String getQuery() {
        return properties.getProperty("query");
    }


    static String[] favoriteQueriesTitles(List<Query> queries1) {
/*
        final String count = properties.getProperty("favorite.query.count");
        final int length = Integer.parseInt(count);
        final String[] names = new String[length];
        for (int i = 0; i < names.length; i++) {
            names[i] = properties.getProperty("favorite.query." + i + ".query.title");

        }
        return names;
*/
        final List<Query> list = queries1;
        final String[] strings = new String[list.size()];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = list.get(i).getTitle();
            LOGGER.debug(strings[i]);
        }
        return strings;
    }

    public Map<Integer, String[]> getData() {
        return data;
    }

    private static String readStringFromFile (File file) throws IOException {
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

    private void printPadded(final int i, final String string, final FileWriter fileWriter) throws IOException {
        fileWriter.write(string);
        for (int c = 0; c < columnWidth(i) - string.length(); c++) {
            fileWriter.write(' ');
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
                pluginPanelItem[i] = presentation(i, value);
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


    String presentation(final int i, final String s) {
        if (columnHasPadding(i)) {
            return pad(s, columnWidth(i));
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

    private boolean columnHasPadding(final int i) {
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
        final NamedColumnDescriptor[] columns =
                new NamedColumnDescriptor[getColumnCount()];
        for (int i = 0; i < getColumnCount(); i++) {
            columns[i] = new NamedColumnDescriptor(
                    columnTitle(i),
                    columnType(i),
                    columnWidth(i));
        }
        final PanelMode[] panelModes = new PanelMode[10];
        for (int i = 0; i < panelModes.length; i++) {
            panelModes[i] = new PanelMode(columns, i == PanelModeId.WIDE, null);
        }
        return panelModes;
    }

    private PanelColumnType columnType(final int i) {
        return Enum.valueOf(PanelColumnType.class, properties.getProperty("column." + i + ".type", "CUSTOM" + i));
    }

    private Integer columnWidth(int i) {
        return Integer.valueOf(properties.getProperty("column." + i + ".width"));
    }

    private String columnTitle(int i) {
        return properties.getProperty("column." + i + ".title");
    }

    String[] initDefaults() {
        return defaults = defaults();
    }

    String[] defaults() {
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

    /**
     * Every info panel line contains result of certain query
     *  @param i index of info panel line
     *
     */
    FarInfoPanelLine fillInfoPanelLine(final int i) {
        try {
            Connection conn = DriverManager.getConnection(getUrl());
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(
                    getProperties().getProperty(
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
            return new FarInfoPanelLine(
                    getProperties().getProperty(String.format("info.line.%d.text", i)),
                    String.valueOf(object),
                    false
            );
        } catch (SQLException e) {
            Messages.longOperation(e.toString());
            LOGGER.error(e, e);
            return null;
        }
    }

    FarInfoPanelLine[] fillInfoPanelLines() {
        return infoPanelLines = infoPanelLines();
    }

    private FarInfoPanelLine[] infoPanelLines() {
        Properties properties = getProperties();
        String property;
        property = properties.getProperty("info.line.count");
        if (property == null) {
            return new FarInfoPanelLine[0];
        }
        try {
            int count = Integer.parseInt(property);
            if (count < 1) {
                throw new IllegalArgumentException("info.line.count should be > 0");
            }
            final FarInfoPanelLine[] infoPanelLines = new FarInfoPanelLine[count];
            for (int i = 0; i < count; i++) {
                infoPanelLines[i] = fillInfoPanelLine(i);
            }
            return infoPanelLines;
        } catch (Exception e) {
            LOGGER.warn(e.getMessage());
            return null;
        }
    }

    public void executeUpdate(final String query) {
        LOGGER.info("Executing query " + query);
        try {
            final Connection conn = DriverManager.getConnection(getUrl());
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

    PluginPanelItem[] executeQuery() {
        return pluginPanelItems = executeQuery(getQuery(), data, getColumnCount());
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
                    pluginPanelItem.customColumns[i] = presentation(i, value);
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



    public List<IdValuePair> executeIdValueQuery(final String query) {
        try {
            Connection conn = DriverManager.getConnection(getUrl());
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            final List<IdValuePair> items = new ArrayList<>();

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

    public Object executeScalarQuery(final String query) {
        try {
            Connection conn = DriverManager.getConnection(getUrl());
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery (query);

            Object value = null;

            while (rs.next ()) value = rs.getObject (1); // TODO: more checks

            rs.close();
            stmt.close();

            return value;
        }
        catch (Exception e) {
            LOGGER.error (e, e);
            return null;    // TODO
        }
    }

    int putFile(String srcPath, String fileName) {
        final File file = new File (srcPath, fileName);
        LOGGER.debug("Put file " + file);
        try {
            final String s = readStringFromFile(file);
            final String[] strings = s.split("\n");

            for (int i = 0; i < strings.length; i++) {
                strings[i] = strings[i].trim();
            }

            String query = insertQuery(strings);
            executeUpdate(query);

            return 1;
        }
        catch (IOException e) {
            LOGGER.error(e,e);
            return 0;
        }
    }

    int getFile(String destPath, String fileName) {
        LOGGER.debug("getFile " + destPath + ' ' + fileName);
        final int currentItem = AbstractPlugin.getCurrentItem();
        int selectedItemId = Integer.valueOf(pluginPanelItems[currentItem - 1].cFileName);
        LOGGER.debug("getFile: " + selectedItemId);
        final File file = new File(destPath, String.valueOf(selectedItemId));
        try {
            final FileWriter writer = new FileWriter(file);

            writer.write(String.valueOf(selectedItemId));
            writer.write('\n');
            final String[] strings = getData().get(selectedItemId);
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

        LOGGER.debug("getFile returns 0");
        return 0;
    }

    int handleFavorites(final List<Query> queries,
            QueryPanelContentProvider_Properties queryPanelContentProvider_properties) {
        final String[] titles = favoriteQueriesTitles(queries);
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
            return query.handleInsert(queryPanelContentProvider_properties, defaults, this);
        }
    }

    int handleInsert(QueryPanelContentProvider_Properties queryPanelContentProvider_properties) {
        if (properties.getProperty("insert.query") == null) {
            return 0;
        }
        final ParametersDialog dialog = new ParametersDialog(queryPanelContentProvider_properties, properties, "insert", defaults);
        if (!dialog.activate()) {
            return 1;
        } else {
            final String query = constructQuery("insert.query", dialog.getParams(null));
            executeUpdate(query);
            return 1;
        }
    }

    int handleUpdate(QueryPanelContentProvider_Properties queryPanelContentProvider_properties) {
        LOGGER.debug("Update!");
        if (updateQuery() == null) {
            return 0;
        }
        final int currentItem = AbstractPlugin.getCurrentItem();
        int selectedItemId = Integer.valueOf(pluginPanelItems[currentItem - 1].cFileName);

//        int selectedItemId = AbstractPlugin.getSelectedItemCrc32();
        String[] selectedLineValues = queryPanelContentProvider_properties.data.get(selectedItemId);
        ParametersDialog dialog = new ParametersDialog(queryPanelContentProvider_properties, getProperties(), "update", selectedLineValues);
        if (!dialog.activate()) {
            LOGGER.debug("Update dialog dismissed");
            return 1;
        } else {
            String query = constructQuery("update.query", dialog.getParams(selectedItemId));
            executeUpdate(query);
            return 1;
        }
    }

    int handleDelete() {
        if (getProperties().getProperty("delete.query") == null) {
            return 0;
        } else {
            if (new YesNoDialog("Delete", "Do you want to delete this record?", "OK", "Cancel").activate()) {
                final int currentItem = AbstractPlugin.getCurrentItem();
                int selectedItemId = Integer.valueOf(pluginPanelItems[currentItem - 1].cFileName);
                LOGGER.info("Going to delete column with id " + selectedItemId);
                String query = deleteQuery(selectedItemId);
                executeUpdate(query);
            }
            return 1;
        }
    }


    int handleExport(final File outputFile) {
        try {
            FileWriter fileWriter = new FileWriter(outputFile);
            exportData(fileWriter);
            exportInfo(fileWriter);
            fileWriter.close();
            return 0;

        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
    }

    void exportData(final FileWriter fileWriter) throws IOException {
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

    void exportInfo(final FileWriter fileWriter) throws IOException {
        if (infoPanelLines == null) return;
        fileWriter.write('\n');
        for (FarInfoPanelLine infoPanelLine : infoPanelLines) {
            fileWriter.write(infoPanelLine.getText());
            fileWriter.write(' ');
            fileWriter.write(infoPanelLine.getData());
            fileWriter.write('\n');
        }
    }
}
