package org.farmanager.plugins.jdbc.queries;

public class Parameter {
    private String title;
    private String query;
    private String type;

    public String getTitle () {
        return title;
    }

    public void setTitle (final String title) {
        this.title = title;
    }

    public String getQuery () {
        return query;
    }

    public void setQuery (final String query) {
        this.query = query;
    }

    public String getType () {
        return type;
    }

    public void setType (final String type) {
        this.type = type;
    }

    @Override public String toString () {
        return "Parameter["
            + "title=" + title + ","
            + "query=" + query + ","
            + "type=" + type + "]";
    }
    
}
