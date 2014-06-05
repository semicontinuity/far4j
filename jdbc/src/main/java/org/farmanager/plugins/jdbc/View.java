package org.farmanager.plugins.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.farmanager.api.PanelMode;
import org.farmanager.api.PanelModeId;
import org.farmanager.api.jni.PanelColumnType;
import org.farmanager.api.panels.NamedColumnDescriptor;

public class View {
    private static final Logger LOGGER = Logger.getLogger(View.class);

    private Properties properties;
    private View parent;

    public View(final Properties properties, final View parent) {
        this.properties = new Properties();
        this.properties.putAll(properties);
        this.parent = parent;

        loadDriver();
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
}
