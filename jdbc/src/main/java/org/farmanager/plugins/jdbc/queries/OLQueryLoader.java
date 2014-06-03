package org.farmanager.plugins.jdbc.queries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import neutrino.script.JavaBindings;
import org.apache.log4j.Logger;

public class OLQueryLoader {

    private static final Logger LOGGER = Logger.getLogger(OLQueryLoader.class);


    public List<Query> apply(final File directory) throws ScriptException {
        final ScriptEngine engine = new ScriptEngineManager().getEngineByName("OL");
        if (engine == null) throw new RuntimeException("Engine not found");

        final JavaBindings javaBindings = new JavaBindings();

        LOGGER.info("Loading queries from " + directory);
        final File[] files = directory.listFiles();
        if (files == null) throw new IllegalArgumentException();

        final List<Query> queries = new ArrayList<>();
        for (File file : files) {
            LOGGER.info("Loading query from " + file);
            try {
                final Query query = (Query) engine.eval(new BufferedReader(new FileReader(file)), javaBindings);
                queries.add(query);
            }
            catch (Throwable e) {
                LOGGER.error(e, e);
            }
        }
        LOGGER.debug("Loaded " + queries.size());
        return queries;
    }
}
