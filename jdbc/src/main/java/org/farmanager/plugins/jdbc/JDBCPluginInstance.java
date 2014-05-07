package org.farmanager.plugins.jdbc;

import org.apache.log4j.Logger;
import org.farmanager.api.vfs.GenericSessionListPanelContentProvider;
import org.farmanager.api.vfs.MultiVirtualFSPluginInstance;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class JDBCPluginInstance extends MultiVirtualFSPluginInstance
        implements GenericSessionListPanelContentProvider.Listener
{
    static Logger LOGGER = Logger.getLogger(JDBCPluginInstance.class);

    protected final JDBCPlugin plugin;

    private final GenericSessionListPanelContentProvider sessionListPanelContentProvider;
    private final QueryPanelContentProvider queryPanelContentProvider;


    public JDBCPluginInstance(JDBCPlugin plugin) {
        this.plugin = plugin;
        this.sessionListPanelContentProvider = new GenericSessionListPanelContentProvider(
                this.plugin,
                this,
                " JDBC: Stored sessions ")
        {
            protected Properties loadSession(final File target) throws IOException {
                final Properties properties = super.loadSession(target);
                final String template = properties.getProperty("use-template");
                return (template != null)
                        ? loadTemplate(template, properties)
                        : properties;
            }
        };
        this.queryPanelContentProvider = new QueryPanelContentProvider(plugin, this);
    }

    public void navigateToSessionList() {
        LOGGER.debug("navigateToSessionList");
        panelContentProvider = sessionListPanelContentProvider;
    }

    public void openSession(final Properties properties) {
        queryPanelContentProvider.init(properties);
        navigateToQueryResult();
    }

    public void navigateToQueryResult() {
        LOGGER.debug("navigateToQueryResult");
        panelContentProvider = queryPanelContentProvider;
    }

    public Properties loadTemplate(final String name, final Properties parameters) throws IOException {
        // quick and dirty
        final File file = new File(plugin.templatesFolder(), name);
        final FileInputStream inputStream = new FileInputStream(file);
        final byte[] bytes = new byte[(int) file.length()];
        inputStream.read(bytes);
        inputStream.close();

        final String contents = new String(bytes);
        String result = contents;
        final Set<Map.Entry<Object, Object>> entries = parameters.entrySet();

        for (Map.Entry<Object, Object> entry : entries) {
            result = result.replace("${" + entry.getKey() + "}", (CharSequence) entry.getValue());
        }

        final Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(result.getBytes()));
        return properties;
    }
}
