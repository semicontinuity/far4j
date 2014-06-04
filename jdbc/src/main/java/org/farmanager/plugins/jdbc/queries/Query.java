package org.farmanager.plugins.jdbc.queries;

import org.farmanager.plugins.jdbc.ParametersDialog;
import org.farmanager.plugins.jdbc.QueryPanelContentProvider;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.ArrayList;
import java.text.MessageFormat;

public class Query {

    private static final Logger LOGGER = Logger.getLogger (Query.class);

    private List<Parameter> parameters = new ArrayList<Parameter>();

    private String title;
    private String query;

    public void setTitle (final String title) {
        this.title = title;
    }

    public String getTitle () {
        return title;
    }

    public void setQuery (final String query) {
        this.query = query;
    }


    public void addParameter (final Parameter child) {
        parameters.add(child);
    }

    public List<Parameter> getParameters () {
        return parameters;
    }

    @Override public String toString () {
        return "Query["
            + "title=" + title + ","
            + "query=" + query + ","
            + "parameters=" + parameters + "]";
    }

    public int handleInsert (
        final QueryPanelContentProvider queryPanelContentProvider,
        final String[] defaults) {

        ParametersDialog dialog = new ParametersDialog (queryPanelContentProvider, this, defaults);
        if (!dialog.activate ())
        {
            return 1;
        }
        else
        {
/*
            String query = constructQuery (prefix + ".query", dialog.getParams (null));
            executeUpdate(query);
*/
            queryPanelContentProvider.executeUpdate(construct(dialog));
            return 1;
        }
    }

    private String construct (final ParametersDialog dialog) {
        return String.format(query, dialog.getParams(null));
    }
}
