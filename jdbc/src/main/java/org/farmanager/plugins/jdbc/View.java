package org.farmanager.plugins.jdbc;

import java.util.Properties;

public class View {
    private Properties properties;
    private View parent;

    public View (final Properties properties, final View parent) {
        this.properties = new Properties();
        this.properties.putAll(properties);
        this.parent = parent;
    }

    public Properties getProperties () {
        return properties;
    }

    public View getParent () {
        return parent;
    }
}
