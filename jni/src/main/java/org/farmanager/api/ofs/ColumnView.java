package org.farmanager.api.ofs;

public @interface ColumnView
{
    String property();
    String title();
    int width() default 0;
}
