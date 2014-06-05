package org.farmanager.plugins.jdbc;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.farmanager.api.AbstractPlugin;
import org.farmanager.api.PanelMode;
import org.farmanager.api.PluginPanelItem;
import org.farmanager.api.jni.FarInfoPanelLine;
import org.farmanager.api.vfs.AbstractPanelContentProvider;
import org.farmanager.plugins.jdbc.queries.Query;

/**
 * @author igorkarpov
 */
public class QueryPanelContentProvider extends AbstractPanelContentProvider {
    protected static DecimalFormat format = new DecimalFormat("0000000000");
    protected final AbstractPlugin plugin;
    protected final JDBCPluginInstance instance;
    /**
     * In some reports, lines may have non-unique ids... take care of that
     */
    protected final Map<Integer, String[]> data;
    protected String query;
    protected String url;
    protected int columnCount;
    protected String panelTitle;
    protected Properties properties;
    protected FarInfoPanelLine[] infoPanelLines;
    protected String[] defaults;
    protected PluginPanelItem[] pluginPanelItems;
    /** If true, query results will be rendered as directories */
    protected boolean navigatable;
    protected String childTemplate;
    protected View currentView;
    protected List<Query> queries;

    public QueryPanelContentProvider(
            final JDBCPluginInstance instance, List<Query> queries, final JDBCPlugin plugin)
    {
        this.instance = instance;
        this.data = new HashMap<>();
        this.queries = queries;
        this.plugin = plugin;
    }
}
