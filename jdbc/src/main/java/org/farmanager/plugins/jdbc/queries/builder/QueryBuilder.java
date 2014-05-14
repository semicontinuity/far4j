package org.farmanager.plugins.jdbc.queries.builder;

import groovy.util.BuilderSupport;
import org.farmanager.plugins.jdbc.queries.Parameter;
import org.farmanager.plugins.jdbc.queries.Query;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A helper class for creating nested trees of Node objects for
 * handling arbitrary data
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision: 38 $
 */
public class QueryBuilder extends BuilderSupport {

    public static QueryBuilder newInstance () {
        return new QueryBuilder();
    }

    protected void setParent (Object parent, Object child) {
        if (parent instanceof Query && child instanceof Parameter) {
            ((Query) parent).addParameter((Parameter) child);
        }
    }

    protected Object createNode (Object name) {
        return createNode(name, null, null);
    }


    protected Object createNode (Object name, Object value) {
        return createNode(name, null, value);
    }

    protected Object createNode (Object name, Map attributes) {
        return createNode(name, attributes, null);
    }

    protected Object createNode (Object name, Map attributes, Object value) {
        if ("persistentQuery".equals(name)) {
            final Query persistentQuery = new Query();
            if (attributes != null) {
                final Set set = attributes.keySet();
                for (Iterator iterator = set.iterator(); iterator.hasNext();) {
                    String key = (String) iterator.next();
                    if (key.equals("title")) {
                        persistentQuery.setTitle((String) attributes.get(key));
                    }
                    else if (key.equals("query")) {
                        persistentQuery.setQuery((String) attributes.get(key));
                    }
                    else throw new IllegalArgumentException();
                }
            }
            return persistentQuery;
        }
        else if ("parameter".equals(name)) {
            final Parameter parameter = new Parameter();
            if (attributes != null) {
                final Set set = attributes.keySet();
                for (Iterator iterator = set.iterator(); iterator.hasNext();) {
                    String key = (String) iterator.next();
                    if (key.equals("title")) {
                        parameter.setTitle((String) attributes.get(key));
                    }
                    else if (key.equals("query")) {
                        parameter.setQuery((String) attributes.get(key));
                    }
                    else if (key.equals("type")) {
                        parameter.setType((String) attributes.get(key));
                    }
                    else throw new IllegalArgumentException();
                }
            }
            return parameter;
        }
        else throw new IllegalArgumentException();
    }
}