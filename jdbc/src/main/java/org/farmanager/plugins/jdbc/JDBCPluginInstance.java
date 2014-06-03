package org.farmanager.plugins.jdbc;

import org.apache.log4j.Logger;
import org.farmanager.api.vfs.GenericSessionListPanelContentProvider;
import org.farmanager.api.vfs.MultiVirtualFSPluginInstance;
import org.farmanager.plugins.jdbc.queries.GroovyQueryLoader;
import org.farmanager.plugins.jdbc.queries.OLQueryLoader;
import org.farmanager.plugins.jdbc.queries.Query;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
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


    public JDBCPluginInstance(final JDBCPlugin plugin) throws Exception {
        LOGGER.debug("> <init>");
        try {
            this.plugin = plugin;
            this.sessionListPanelContentProvider = new GenericSessionListPanelContentProvider(
                    this.plugin,
                    this,
                    " JDBC: Stored sessions ")
            {
                protected Properties loadSession(final File target) throws IOException {
                    LOGGER.debug("@ loadSession");
                    final Properties properties = super.loadSession(target);
                    final String template = properties.getProperty("use-template");
                    return (template != null)
                            ? loadTemplate(template, properties)
                            : properties;
                }
            };
            final List<Query> queries1 = new OLQueryLoader().apply(new File(plugin.pluginSettingsFolder(), "queries1"));
            final List<Query> queries =
                    new GroovyQueryLoader().apply(new File(plugin.pluginSettingsFolder(), "queries"));

            queries.addAll(queries1);
            this.queryPanelContentProvider = new QueryPanelContentProvider(plugin, this, queries);

        }
        catch (Exception e) {
            LOGGER.error(e, e);
            throw e;
        }
        finally {
            LOGGER.debug("< <init>");
        }
    }

//    @Override
//    public String getHostFile() {
//        return plugin.pluginSettingsFolder().toString();
//    }

    @Override
    public void navigateToSessionList() {
        LOGGER.debug("navigateToSessionList");
        panelContentProvider = sessionListPanelContentProvider;
    }

    @Override
    public void openSession(final Properties properties) throws IOException {
//        queryPanelContentProvider.init(properties);
//        navigateToQueryResult();
        final String template = properties.getProperty("use-template");
        final Properties realProperties = (template != null)
                ? loadTemplate(template, properties)
                : properties;

        queryPanelContentProvider.setView(new View(realProperties,null));
        panelContentProvider = queryPanelContentProvider;
    }

    public void navigateToQueryResult() {
        LOGGER.debug("navigateToQueryResult");
        panelContentProvider = queryPanelContentProvider;
    }

    public Properties loadTemplate(final String name, final Properties parameters) throws IOException {
        LOGGER.info("Loading template " + name + " with properties " + parameters);
        // quick and dirty
        final File templatesFolder = plugin.templatesFolder();
        LOGGER.info("templatesFolder " + templatesFolder);
        final File file = new File(templatesFolder, name);
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
        LOGGER.info("Loaded template " + name);
        return properties;
    }
}
