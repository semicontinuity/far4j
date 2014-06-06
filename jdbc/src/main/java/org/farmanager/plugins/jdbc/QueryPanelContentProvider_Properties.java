package org.farmanager.plugins.jdbc;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.farmanager.api.AbstractPlugin;
import org.farmanager.api.PanelMode;
import org.farmanager.api.PluginPanelItem;
import org.farmanager.api.jni.FarInfoPanelLine;
import org.farmanager.api.jni.ProcessKeyFlags;
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

        columnCount = view.getColumnCount();
        defaults = view.initDefaults();
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
        infoPanelLines = currentView.fillInfoPanelLines();
        return pluginPanelItems = currentView.executeQuery();
    }


    @Override
    public int getFile(final String destPath, final String fileName, final int move, final int opmode) {
        return currentView.getFile(destPath, fileName);
    }


    @Override
    public int putFile(String srcPath, final String fileName, final int move, final int opmode) {
        return currentView.putFile(srcPath, fileName);
    }


    public int processKey(final int key, final int controlState) {
        int realKey = ProcessKeyFlags.clearedPreprocess(key);
        if (alt(controlState) && shift (controlState) && realKey == VK_F4) {
            return currentView.handleFavorites(queries, this);
        }
        else if (shift(controlState) && realKey == VK_F4) {
            return currentView.handleInsert(this);
        }
        else if (noFlags(controlState) && realKey == VK_F4) {
            return currentView.handleUpdate(this);
        }
        else if (noFlags(controlState) && realKey == VK_F8) {
            return currentView.handleDelete();
        }
        else if (shift(controlState) && realKey == VK_F2) {
            return currentView.handleExport(outputFile());
        }
        else {
            return 0;
        }
    }


    @Override
    public FarInfoPanelLine[] getInfoPanelLines() {
        return currentView.infoPanelLines;
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

    private File outputFile() {
        return new File(plugin.getHome(), currentView.getTitle() + ".txt");
    }

}
