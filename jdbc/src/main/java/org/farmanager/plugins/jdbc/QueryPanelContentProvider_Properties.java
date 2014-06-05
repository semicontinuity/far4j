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
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.farmanager.api.AbstractPlugin;
import org.farmanager.api.PanelMode;
import org.farmanager.api.PluginPanelItem;
import org.farmanager.api.dialogs.YesNoDialog;
import org.farmanager.api.jni.FarInfoPanelLine;
import org.farmanager.api.jni.ProcessKeyFlags;
import org.farmanager.api.messages.Messages;
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
public class QueryPanelContentProvider_Properties extends QueryPanelContentProvider {
    private static final Logger LOGGER = Logger.getLogger(QueryPanelContentProvider_Properties.class);


    public QueryPanelContentProvider_Properties(final JDBCPlugin plugin, final JDBCPluginInstance instance,
            List<Query> queries) {
        super(instance, queries, plugin);
    }


    public void setView(final View view) {
        LOGGER.info("Setting view: " + view);
        this.currentView = view;
        this.properties = view.getProperties();

        query = view.getQuery();
        url = view.getUrl();
        columnCount = view.getColumnCount();
        defaults = view.initDefaults();
        navigatable = view.isNavigatable();
        childTemplate = view.getChildTemplate();
    }


    @Override
    public PanelMode[] getPanelModes() {
        return currentView.getPanelModes();
    }

    @Override
    public String getPanelTitle() {
        return currentView.getTitle();
    }

    @Override
    public PluginPanelItem[] getFindData(final int opMode) {
        LOGGER.info("getFindData " + opMode);
        // TODO: smarter: cache!
        fillInfoPanelLines();
        return pluginPanelItems = currentView.executeQuery(query, data, columnCount);
    }


    public List<IdValuePair> executeIdValueQuery(final String query) {
        try {
            Connection conn = DriverManager.getConnection(url);
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
        return new File(plugin.getHome(), currentView.getTitle() + ".txt");
    }

    private void printPadded(final int i, final String string, final FileWriter fileWriter) throws IOException {
        fileWriter.write(string);
        for (int c = 0; c < View.columnWidth(properties, i) - string.length(); c++) {
            fileWriter.write(' ');
        }
    }


    private int handleDelete() {
        if (properties.getProperty("delete.query") == null) {
            return 0;
        } else {
            if (new YesNoDialog("Delete", "Do you want to delete this record?", "OK", "Cancel").activate()) {
                final int currentItem = AbstractPlugin.getCurrentItem();
                int selectedItemId = Integer.valueOf(pluginPanelItems[currentItem - 1].cFileName);
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
            LOGGER.debug(strings[i]);
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
                    final String childKey = childKey();
                    if (childKey == null) {
                        LOGGER.info("No child key");
                        return;
                    }
                    try {
                        final Properties childProperties = new Properties();
                        for (Object key : properties.keySet()) {
                            final String propertyName = (String) key;
                            if (propertyName.startsWith("child."))
                                childProperties.put(propertyName.substring("child.".length()), properties.get(key));
                            LOGGER.debug(".");
                        }
                        childProperties.put("main-id", childKey);

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
        final int index = currentView.keyColumn();
        if (index == -1) {
            final int currentItem = AbstractPlugin.getCurrentItem();
            return pluginPanelItems[currentItem - 1].cFileName;
        }
        else {
            final int currentItem = AbstractPlugin.getCurrentItem();
            return pluginPanelItems[currentItem - 1].customColumns[index];
        }
    }


    public String getCurrentDirectory() {
        return currentView.getTitle();
    }


    protected static String zeropadded(final long id) {
        // We do formatting for correct sorting of entries
        // (if String.valueOf() is used, 1000 would be before 999)
        return format.format(id);
    }
}
