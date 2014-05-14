package org.farmanager.plugins.jdbc.queries.builder;

import groovy.lang.Script;

public abstract class QueryScript extends Script {

    protected QueryBuilder builder = new QueryBuilder();

/*
    public Object invokeMethod(String methodName, Object args) {
        if ("query".equals(methodName))
            return builder.invokeMethod(methodName, args);
        else
            return super.invokeMethod(methodName, args);
    }
*/
}
