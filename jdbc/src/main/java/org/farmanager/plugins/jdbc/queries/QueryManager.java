package org.farmanager.plugins.jdbc.queries;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.log4j.Logger;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class QueryManager {

    private static final Logger LOGGER = Logger.getLogger(QueryManager.class);
    private File directory;

    private transient List<Query> queries = new ArrayList<Query>();

    public void setDirectory (final File directory) {
        this.directory = directory;
    }

    public void create () {
        for (File file : directory.listFiles()) {
            try {
                queries.add(loadQuery(file));
            }
            catch (Throwable e) {
                LOGGER.error(e, e);
            }
        }

        LOGGER.info(queries);
    }

    public List<Query> getQueries () {
        return queries;
    }

    private static Query loadQuery (final File file) throws IOException {
        LOGGER.info("Processing groovy script " + file);

        final Binding binding = new Binding();
        final CompilerConfiguration conf = new CompilerConfiguration();
        conf.setScriptBaseClass("org.farmanager.plugins.jdbc.queries.builder.QueryScript");
        final GroovyShell groovyShell = new GroovyShell(binding, conf);
        final Script script = groovyShell.parse(file);
        final Object o = script.run();
        return (Query) o;
    }
}
