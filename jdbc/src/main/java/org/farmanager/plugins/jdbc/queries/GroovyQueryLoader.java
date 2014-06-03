package org.farmanager.plugins.jdbc.queries;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.log4j.Logger;
import org.codehaus.groovy.control.CompilerConfiguration;

public class GroovyQueryLoader {

    private static final Logger LOGGER = Logger.getLogger(GroovyQueryLoader.class);


    public List<Query> apply(final File directory) {
        LOGGER.info("Loading queries from " + directory);
        final File[] files = directory.listFiles();
        if (files == null) throw new IllegalArgumentException();

        final List<Query> queries = new ArrayList<>();
        for (File file : files) {
            try {
                queries.add(loadQuery(file));
            }
            catch (Throwable e) {
                LOGGER.error(e, e);
            }
        }
        return queries;
    }

    private static Query loadQuery (final File file) throws IOException {
//        LOGGER.info("Processing groovy script " + file);

        final Binding binding = new Binding();
        final CompilerConfiguration conf = new CompilerConfiguration();
        conf.setScriptBaseClass("org.farmanager.plugins.jdbc.queries.builder.QueryScript");
        final GroovyShell groovyShell = new GroovyShell(binding, conf);
        final Script script = groovyShell.parse(file);
        final Object o = script.run();
        return (Query) o;
    }
}
